package com.example.chatapp.service;

import com.example.chatapp.entity.DirectMessage;
import com.example.chatapp.entity.DirectMessageConversation;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.DirectMessageConversationRepository;
import com.example.chatapp.repository.DirectMessageRepository;
import com.example.chatapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DirectMessageService {

    private static final Logger logger = LoggerFactory.getLogger(DirectMessageService.class);

    @Autowired
    private DirectMessageConversationRepository conversationRepository;

    @Autowired
    private DirectMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private FriendshipService friendshipService;

    /**
     * DM会話を取得または作成
     */
    public DirectMessageConversation getOrCreateConversation(Long user1Id, Long user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + user1Id));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + user2Id));

        // 既存の会話を検索
        Optional<DirectMessageConversation> existing = conversationRepository.findByUsers(user1, user2);
        if (existing.isPresent()) {
            logger.info("既存のDM会話を使用: ConversationId={}", existing.get().getId());
            return existing.get();
        }

        // 新規会話を作成（user1とuser2の順序を統一：IDが小さい方をuser1に）
        User smaller = user1.getId() < user2.getId() ? user1 : user2;
        User larger = user1.getId() < user2.getId() ? user2 : user1;

        DirectMessageConversation conversation = new DirectMessageConversation(smaller, larger);
        conversation = conversationRepository.save(conversation);
        logger.info("新規DM会話を作成: ConversationId={}, User1={}, User2={}", 
                    conversation.getId(), smaller.getUsername(), larger.getUsername());
        return conversation;
    }

    /**
     * DMメッセージを送信
     */
    public DirectMessage sendMessage(Long conversationId, Long senderId, String content) {
        DirectMessageConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会話が見つかりません: " + conversationId));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("送信者が見つかりません: " + senderId));

        // 受信者を特定
        User receiver = conversation.getOtherUser(sender);
        if (receiver == null) {
            throw new RuntimeException("この会話に送信者が含まれていません");
        }

        // メッセージを作成
        DirectMessage message = new DirectMessage(conversation, sender, receiver, content);
        message = messageRepository.save(message);

        // 会話の最終メッセージ情報を更新
        conversation.setLastMessageAt(message.getSentAt());
        conversation.setLastMessageContent(content);
        conversation.setLastMessageSender(sender);
        conversationRepository.save(conversation);

        logger.info("DMメッセージ送信: MessageId={}, Sender={}, Receiver={}", 
                    message.getId(), sender.getUsername(), receiver.getUsername());
        return message;
    }

    /**
     * 指定ユーザーのDM会話一覧を取得（最新順）
     */
    public List<Map<String, Object>> getUserConversations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + userId));

        List<DirectMessageConversation> conversations = conversationRepository.findByUserOrderByLastMessageAtDesc(user);

        return conversations.stream().map(conv -> {
            User otherUser = conv.getOtherUser(user);
            long unreadCount = messageRepository.countUnreadMessages(conv, user);

            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conv.getId());
            data.put("userId", otherUser.getId());
            data.put("username", otherUser.getUsername());
            data.put("displayName", userProfileService.getDisplayName(otherUser));
            data.put("avatarUrl", userProfileService.getAvatarUrl(otherUser));
            data.put("lastMessage", conv.getLastMessageContent());
            data.put("lastMessageAt", conv.getLastMessageAt());
            data.put("lastMessageSender", conv.getLastMessageSender() != null ? 
                     conv.getLastMessageSender().getUsername() : null);
            data.put("unreadCount", unreadCount);
            return data;
        }).collect(Collectors.toList());
    }

    /**
     * 友達（相互フォロー）のDM会話一覧を取得
     */
    public List<Map<String, Object>> getFriendsConversations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + userId));

        // 友達一覧を取得
        List<User> friends = friendshipService.getFriends(user);
        Set<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toSet());

        // 全会話を取得
        List<DirectMessageConversation> allConversations = conversationRepository.findByUserOrderByLastMessageAtDesc(user);

        // 友達とのDM会話のみをフィルタリング
        return allConversations.stream()
                .filter(conv -> {
                    User otherUser = conv.getOtherUser(user);
                    return friendIds.contains(otherUser.getId());
                })
                .map(conv -> {
                    User otherUser = conv.getOtherUser(user);
                    long unreadCount = messageRepository.countUnreadMessages(conv, user);

                    Map<String, Object> data = new HashMap<>();
                    data.put("conversationId", conv.getId());
                    data.put("userId", otherUser.getId());
                    data.put("username", otherUser.getUsername());
                    data.put("displayName", userProfileService.getDisplayName(otherUser));
                    data.put("avatarUrl", userProfileService.getAvatarUrl(otherUser));
                    data.put("lastMessage", conv.getLastMessageContent());
                    data.put("lastMessageAt", conv.getLastMessageAt());
                    data.put("lastMessageSender", conv.getLastMessageSender() != null ? 
                             conv.getLastMessageSender().getUsername() : null);
                    data.put("unreadCount", unreadCount);
                    return data;
                })
                .collect(Collectors.toList());
    }

    /**
     * 会話のメッセージ一覧を取得
     */
    public List<Map<String, Object>> getConversationMessages(Long conversationId, Long userId) {
        DirectMessageConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会話が見つかりません: " + conversationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + userId));

        // ユーザーがこの会話に参加しているか確認
        if (!conversation.containsUser(user)) {
            throw new RuntimeException("この会話にアクセスする権限がありません");
        }

        List<DirectMessage> messages = messageRepository.findByConversationOrderBySentAtAsc(conversation);

        return messages.stream().map(msg -> {
            Map<String, Object> data = new HashMap<>();
            data.put("messageId", msg.getId());
            data.put("senderId", msg.getSender().getId());
            data.put("senderUsername", msg.getSender().getUsername());
            data.put("senderDisplayName", userProfileService.getDisplayName(msg.getSender()));
            data.put("senderAvatarUrl", userProfileService.getAvatarUrl(msg.getSender()));
            data.put("content", msg.getContent());
            data.put("sentAt", msg.getSentAt());
            data.put("isRead", msg.isRead());
            data.put("isOwn", msg.getSender().getId().equals(userId));
            return data;
        }).collect(Collectors.toList());
    }

    /**
     * 会話を既読にする
     */    @Transactional    public void markConversationAsRead(Long conversationId, Long userId) {
        DirectMessageConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会話が見つかりません: " + conversationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + userId));

        if (!conversation.containsUser(user)) {
            throw new RuntimeException("この会話にアクセスする権限がありません");
        }

        messageRepository.markConversationAsRead(conversation, user);
        logger.info("会話を既読にしました: ConversationId={}, UserId={}", conversationId, userId);
    }

    /**
     * ユーザーの全未読メッセージ数を取得
     */
    public long getTotalUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + userId));

        return messageRepository.countAllUnreadMessages(user);
    }

    /**
     * 会話情報を取得
     */
    public Map<String, Object> getConversationInfo(Long conversationId, Long userId) {
        DirectMessageConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会話が見つかりません: " + conversationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + userId));

        if (!conversation.containsUser(user)) {
            throw new RuntimeException("この会話にアクセスする権限がありません");
        }

        User otherUser = conversation.getOtherUser(user);

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversation.getId());
        data.put("userId", otherUser.getId());
        data.put("username", otherUser.getUsername());
        data.put("displayName", userProfileService.getDisplayName(otherUser));
        data.put("avatarUrl", userProfileService.getAvatarUrl(otherUser));
        return data;
    }
}
