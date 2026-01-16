package com.example.chatapp.controller;

import com.example.chatapp.entity.DirectMessage;
import com.example.chatapp.entity.DirectMessageConversation;
import com.example.chatapp.entity.User;
import com.example.chatapp.service.DirectMessageService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * DM機能のWebSocketコントローラー
 */
@Controller
public class DirectMessageWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(DirectMessageWebSocketController.class);

    @Autowired
    private DirectMessageService dmService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * DMメッセージを送信
     * クライアントから /app/dm.send にメッセージが送られる
     */
    @MessageMapping("/dm.send")
    public void sendDirectMessage(@Payload Map<String, Object> payload, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;

        if (username == null) {
            logger.error("認証されていないユーザーからのDM送信試行");
            return;
        }

        try {
            // ペイロードからデータを取得
            Long conversationId = getLongValue(payload.get("conversationId"));
            String content = (String) payload.get("content");

            if (conversationId == null || content == null || content.trim().isEmpty()) {
                logger.error("無効なDMメッセージ: conversationId={}, content={}", conversationId, content);
                return;
            }

            logger.info("📨 DM送信開始: Username={}, ConversationId={}", username, conversationId);

            // 送信者のユーザー情報を取得
            User sender = userService.findByUsername(username).orElse(null);
            if (sender == null) {
                logger.error("ユーザーが見つかりません: {}", username);
                return;
            }

            // メッセージを保存
            DirectMessage message = dmService.sendMessage(conversationId, sender.getId(), content);

            // 会話情報を取得
            DirectMessageConversation conversation = message.getConversation();
            User receiver = conversation.getOtherUser(sender);

            // WebSocket用のメッセージデータを作成
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("messageId", message.getId());
            messageData.put("conversationId", conversationId);
            messageData.put("senderId", sender.getId());
            messageData.put("senderUsername", sender.getUsername());
            messageData.put("senderDisplayName", userProfileService.getDisplayName(sender));
            messageData.put("senderAvatarUrl", userProfileService.getAvatarUrl(sender));
            messageData.put("receiverId", receiver.getId());
            messageData.put("content", message.getContent());
            messageData.put("sentAt", message.getSentAt().toString());
            messageData.put("isRead", false);

            // 送信者にメッセージを返信（自分の画面に即座に表示）
            messagingTemplate.convertAndSendToUser(
                    sender.getUsername(),
                    "/queue/dm",
                    messageData
            );

            // 受信者にメッセージを送信
            messagingTemplate.convertAndSendToUser(
                    receiver.getUsername(),
                    "/queue/dm",
                    messageData
            );

            logger.info("✅ DM送信完了: MessageId={}, Sender={}, Receiver={}", 
                       message.getId(), sender.getUsername(), receiver.getUsername());

        } catch (Exception e) {
            logger.error("❌ DM送信エラー", e);
        }
    }

    /**
     * DM既読通知
     * クライアントから /app/dm.read にメッセージが送られる
     */
    @MessageMapping("/dm.read")
    public void markAsRead(@Payload Map<String, Object> payload, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;

        if (username == null) {
            logger.error("認証されていないユーザーからの既読通知");
            return;
        }

        try {
            Long conversationId = getLongValue(payload.get("conversationId"));

            if (conversationId == null) {
                logger.error("無効な既読通知: conversationId={}", conversationId);
                return;
            }

            User user = userService.findByUsername(username).orElse(null);
            if (user == null) {
                logger.error("ユーザーが見つかりません: {}", username);
                return;
            }

            // 既読処理
            dmService.markConversationAsRead(conversationId, user.getId());

            // 会話情報を取得して相手に既読通知を送信
            var conversationInfo = dmService.getConversationInfo(conversationId, user.getId());
            Long otherUserId = (Long) conversationInfo.get("userId");

            User otherUser = userService.findById(otherUserId).orElse(null);
            if (otherUser != null) {
                Map<String, Object> readNotification = new HashMap<>();
                readNotification.put("conversationId", conversationId);
                readNotification.put("readBy", user.getUsername());

                messagingTemplate.convertAndSendToUser(
                        otherUser.getUsername(),
                        "/queue/dm-read",
                        readNotification
                );

                logger.info("✅ DM既読通知送信: ConversationId={}, ReadBy={}, NotifyTo={}", 
                           conversationId, user.getUsername(), otherUser.getUsername());
            }

        } catch (Exception e) {
            logger.error("❌ DM既読処理エラー", e);
        }
    }

    // ヘルパーメソッド
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
