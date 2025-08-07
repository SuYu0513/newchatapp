package com.example.chatapp.service;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * オンラインユーザーの状態管理サービス
 */
@Service
public class OnlineUserService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private FriendshipService friendshipService;

    // オンラインユーザーの状態を管理するMap
    // Key: username, Value: OnlineUserInfo
    private final Map<String, OnlineUserInfo> onlineUsers = new ConcurrentHashMap<>();

    /**
     * オンラインユーザー情報を格納するクラス
     */
    public static class OnlineUserInfo {
        private String username;
        private String displayName;
        private Long userId;
        private String status; // online, away, busy
        private LocalDateTime lastActiveTime;
        private String sessionId;

        public OnlineUserInfo(String username, String displayName, Long userId, String sessionId) {
            this.username = username;
            this.displayName = displayName;
            this.userId = userId;
            this.sessionId = sessionId;
            this.status = "online";
            this.lastActiveTime = LocalDateTime.now();
        }

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDateTime getLastActiveTime() { return lastActiveTime; }
        public void setLastActiveTime(LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public void updateActivity() {
            this.lastActiveTime = LocalDateTime.now();
        }
    }

    /**
     * ユーザーをオンラインに設定
     */
    public void setUserOnline(String username, String sessionId) {
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserProfile profile = userProfileService.getOrCreateProfile(user);
                String displayName = profile.getDisplayNameOrUsername();

                OnlineUserInfo userInfo = new OnlineUserInfo(username, displayName, user.getId(), sessionId);
                onlineUsers.put(username, userInfo);

                System.out.println("ユーザーがオンラインになりました: " + username);

                // フレンドにオンライン状態を通知
                notifyFriendsAboutStatusChange(user, true);
                
                // 全体のオンラインユーザー数を更新
                broadcastOnlineUserCount();
            }
        } catch (Exception e) {
            System.err.println("ユーザーをオンラインに設定中にエラー: " + e.getMessage());
        }
    }

    /**
     * ユーザーをオフラインに設定
     */
    public void setUserOffline(String username) {
        try {
            OnlineUserInfo userInfo = onlineUsers.remove(username);
            if (userInfo != null) {
                System.out.println("ユーザーがオフラインになりました: " + username);

                Optional<User> userOpt = userService.findByUsername(username);
                if (userOpt.isPresent()) {
                    // フレンドにオフライン状態を通知
                    notifyFriendsAboutStatusChange(userOpt.get(), false);
                }
                
                // 全体のオンラインユーザー数を更新
                broadcastOnlineUserCount();
            }
        } catch (Exception e) {
            System.err.println("ユーザーをオフラインに設定中にエラー: " + e.getMessage());
        }
    }

    /**
     * ユーザーの状態を更新（online, away, busy）
     */
    public void updateUserStatus(String username, String status) {
        OnlineUserInfo userInfo = onlineUsers.get(username);
        if (userInfo != null) {
            userInfo.setStatus(status);
            userInfo.updateActivity();

            try {
                Optional<User> userOpt = userService.findByUsername(username);
                if (userOpt.isPresent()) {
                    // フレンドに状態変更を通知
                    notifyFriendsAboutStatusChange(userOpt.get(), true);
                }
            } catch (Exception e) {
                System.err.println("ユーザー状態更新中にエラー: " + e.getMessage());
            }
        }
    }

    /**
     * ユーザーの活動時間を更新
     */
    public void updateUserActivity(String username) {
        OnlineUserInfo userInfo = onlineUsers.get(username);
        if (userInfo != null) {
            userInfo.updateActivity();
        }
    }

    /**
     * 特定ユーザーのオンライン状態を取得
     */
    public OnlineUserInfo getOnlineUserInfo(String username) {
        return onlineUsers.get(username);
    }

    /**
     * ユーザーがオンラインかどうかチェック
     */
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    /**
     * ユーザーIDからオンライン状態を取得
     */
    public String getUserStatusById(Long userId) {
        try {
            Optional<User> userOpt = userService.findById(userId);
            if (!userOpt.isPresent()) {
                return "offline";
            }
            
            String username = userOpt.get().getUsername();
            OnlineUserInfo userInfo = onlineUsers.get(username);
            
            if (userInfo == null) {
                return "offline";
            }
            
            return userInfo.getStatus();
        } catch (Exception e) {
            return "offline";
        }
    }

    /**
     * すべてのオンラインユーザーを取得
     */
    public List<OnlineUserInfo> getAllOnlineUsers() {
        return new ArrayList<>(onlineUsers.values());
    }

    /**
     * 特定ユーザーのオンラインのフレンドを取得
     */
    public List<OnlineUserInfo> getOnlineFriends(String username) {
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            if (!userOpt.isPresent()) {
                return new ArrayList<>();
            }

            User user = userOpt.get();
            List<User> friends = friendshipService.getFriends(user);
            
            return friends.stream()
                    .filter(friend -> isUserOnline(friend.getUsername()))
                    .map(friend -> getOnlineUserInfo(friend.getUsername()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("オンラインフレンド取得中にエラー: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * オンラインユーザー数を取得
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    /**
     * 状態別のオンラインユーザー数を取得
     */
    public Map<String, Integer> getOnlineUserCountByStatus() {
        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("online", 0);
        statusCounts.put("away", 0);
        statusCounts.put("busy", 0);

        for (OnlineUserInfo userInfo : onlineUsers.values()) {
            String status = userInfo.getStatus();
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }

        return statusCounts;
    }

    /**
     * フレンドにオンライン状態の変更を通知
     */
    private void notifyFriendsAboutStatusChange(User user, boolean isOnline) {
        try {
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("type", "friend_status_change");
            statusUpdate.put("userId", user.getId());
            statusUpdate.put("username", user.getUsername());
            statusUpdate.put("displayName", userProfileService.getOrCreateProfile(user).getDisplayNameOrUsername());
            statusUpdate.put("isOnline", isOnline);
            
            if (isOnline) {
                OnlineUserInfo userInfo = getOnlineUserInfo(user.getUsername());
                statusUpdate.put("status", userInfo != null ? userInfo.getStatus() : "online");
            } else {
                statusUpdate.put("status", "offline");
            }

            // 全体のトピックに送信して、フレンドリストページで受信できるようにする
            messagingTemplate.convertAndSend("/topic/friend-status", statusUpdate);
            
        } catch (Exception e) {
            System.err.println("フレンド状態変更通知エラー: " + e.getMessage());
        }
    }

    /**
     * 全体のオンラインユーザー数をブロードキャスト
     */
    private void broadcastOnlineUserCount() {
        try {
            Map<String, Object> countUpdate = new HashMap<>();
            countUpdate.put("type", "online_count_update");
            countUpdate.put("totalCount", getOnlineUserCount());
            countUpdate.put("statusCounts", getOnlineUserCountByStatus());

            messagingTemplate.convertAndSend("/topic/online-count", countUpdate);
        } catch (Exception e) {
            System.err.println("オンラインユーザー数ブロードキャスト中にエラー: " + e.getMessage());
        }
    }

    /**
     * セッションIDに基づいてユーザーをオフラインに設定
     */
    public void setUserOfflineBySessionId(String sessionId) {
        try {
            String username = onlineUsers.entrySet().stream()
                    .filter(entry -> sessionId.equals(entry.getValue().getSessionId()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (username != null) {
                setUserOffline(username);
            }
        } catch (Exception e) {
            System.err.println("セッションIDによるオフライン設定中にエラー: " + e.getMessage());
        }
    }

    /**
     * 非アクティブユーザーのクリーンアップ（定期実行用）
     */
    public void cleanupInactiveUsers() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
        
        List<String> inactiveUsers = onlineUsers.entrySet().stream()
                .filter(entry -> entry.getValue().getLastActiveTime().isBefore(cutoffTime))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String username : inactiveUsers) {
            System.out.println("非アクティブユーザーをオフラインに設定: " + username);
            setUserOffline(username);
        }
    }
}
