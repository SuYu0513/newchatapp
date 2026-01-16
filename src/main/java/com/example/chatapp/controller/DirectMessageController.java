package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.service.DirectMessageService;
import com.example.chatapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DM機能のREST APIコントローラー
 */
@RestController
@RequestMapping("/api/dm")
public class DirectMessageController {

    private static final Logger logger = LoggerFactory.getLogger(DirectMessageController.class);

    @Autowired
    private DirectMessageService dmService;
    
    @Autowired
    private UserService userService;

    /**
     * 現在のユーザーのDM会話一覧を取得
     * GET /api/dm/conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(createErrorResponse("認証が必要です"));
            }

            // プリンシパル名からユーザーIDを取得（実装に応じて調整）
            Long userId = getUserIdFromPrincipal(principal);
            List<Map<String, Object>> conversations = dmService.getUserConversations(userId);
            long totalUnread = dmService.getTotalUnreadCount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversations", conversations);
            response.put("totalUnread", totalUnread);

            logger.info("DM会話一覧取得: UserId={}, Count={}", userId, conversations.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("DM会話一覧取得エラー", e);
            return ResponseEntity.status(500).body(createErrorResponse("会話一覧の取得に失敗しました"));
        }
    }

    /**
     * 友達（相互フォロー）のDM会話一覧を取得
     * GET /api/dm/conversations/friends
     */
    @GetMapping("/conversations/friends")
    public ResponseEntity<Map<String, Object>> getFriendsConversations(Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(createErrorResponse("認証が必要です"));
            }

            Long userId = getUserIdFromPrincipal(principal);
            List<Map<String, Object>> conversations = dmService.getFriendsConversations(userId);
            long totalUnread = dmService.getTotalUnreadCount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversations", conversations);
            response.put("totalUnread", totalUnread);

            logger.info("友達DM会話一覧取得: UserId={}, Count={}", userId, conversations.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("友達DM会話一覧取得エラー", e);
            return ResponseEntity.status(500).body(createErrorResponse("友達会話一覧の取得に失敗しました"));
        }
    }

    /**
     * 特定ユーザーとのDM会話を開始または取得
     * POST /api/dm/conversations/start
     */
    @PostMapping("/conversations/start")
    public ResponseEntity<Map<String, Object>> startConversation(
            @RequestBody Map<String, Long> request,
            Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(createErrorResponse("認証が必要です"));
            }

            Long userId = getUserIdFromPrincipal(principal);
            Long targetUserId = request.get("targetUserId");

            if (targetUserId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("対象ユーザーIDが必要です"));
            }

            if (userId.equals(targetUserId)) {
                return ResponseEntity.badRequest().body(createErrorResponse("自分自身とのDMは開始できません"));
            }

            var conversation = dmService.getOrCreateConversation(userId, targetUserId);
            var conversationInfo = dmService.getConversationInfo(conversation.getId(), userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversation", conversationInfo);

            logger.info("DM会話開始: ConversationId={}, UserId={}, TargetUserId={}", 
                       conversation.getId(), userId, targetUserId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("DM会話開始エラー", e);
            return ResponseEntity.status(500).body(createErrorResponse("会話の開始に失敗しました"));
        }
    }

    /**
     * 特定の会話のメッセージ一覧を取得
     * GET /api/dm/conversations/{conversationId}/messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable Long conversationId,
            Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(createErrorResponse("認証が必要です"));
            }

            Long userId = getUserIdFromPrincipal(principal);
            List<Map<String, Object>> messages = dmService.getConversationMessages(conversationId, userId);
            var conversationInfo = dmService.getConversationInfo(conversationId, userId);

            // メッセージを既読にする
            dmService.markConversationAsRead(conversationId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            response.put("conversation", conversationInfo);

            logger.info("DMメッセージ取得: ConversationId={}, UserId={}, MessageCount={}", 
                       conversationId, userId, messages.size());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("DMメッセージ取得エラー: " + e.getMessage());
            return ResponseEntity.status(403).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("DMメッセージ取得エラー", e);
            return ResponseEntity.status(500).body(createErrorResponse("メッセージの取得に失敗しました"));
        }
    }

    /**
     * 会話を既読にする
     * POST /api/dm/conversations/{conversationId}/read
     */
    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long conversationId,
            Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(createErrorResponse("認証が必要です"));
            }

            Long userId = getUserIdFromPrincipal(principal);
            dmService.markConversationAsRead(conversationId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("既読エラー: " + e.getMessage());
            return ResponseEntity.status(403).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("既読エラー", e);
            return ResponseEntity.status(500).body(createErrorResponse("既読処理に失敗しました"));
        }
    }

    /**
     * 未読メッセージ数を取得
     * GET /api/dm/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(createErrorResponse("認証が必要です"));
            }

            Long userId = getUserIdFromPrincipal(principal);
            long unreadCount = dmService.getTotalUnreadCount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("unreadCount", unreadCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("未読数取得エラー", e);
            return ResponseEntity.status(500).body(createErrorResponse("未読数の取得に失敗しました"));
        }
    }

    // ヘルパーメソッド
    private Long getUserIdFromPrincipal(Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username).orElseThrow(
            () -> new RuntimeException("ユーザーが見つかりません: " + username)
        );
        return user.getId();
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}
