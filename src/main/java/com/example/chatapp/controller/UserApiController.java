package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.UserProfileService;
import com.example.chatapp.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserProfileService userProfileService;
    
    @Autowired
    private FriendshipService friendshipService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ユーザー検索エンドポイント
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam("q") String query,
            Principal principal) {
        
        if (principal == null || query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String currentUsername = principal.getName();
            
            // ユーザー名またはIDで検索
            List<User> users = userService.searchUsers(query.trim());
            
            // 現在のユーザーを除外
            List<Map<String, Object>> result = users.stream()
                .filter(user -> !user.getUsername().equals(currentUsername))
                .limit(10) // 結果を10件に制限
                .map(user -> {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("id", user.getId());
                    userData.put("username", user.getUsername());
                    userData.put("friendCode", user.getFriendCode());
                    return userData;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // ユーザープロフィール取得エンドポイント
    @GetMapping("/{userId}/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            UserProfile profile = userProfileService.getOrCreateProfile(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            // ユーザー情報
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("friendCode", user.getFriendCode());
            response.put("user", userData);
            
            // プロフィール情報
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("displayName", profile.getDisplayName());
            profileData.put("bio", profile.getBio());
            profileData.put("avatarUrl", profile.getAvatarUrl());
            profileData.put("favoriteThings", profile.getFavoriteThings());
            response.put("profile", profileData);
            
            // フォロー/フォロワー/友達数
            long followingCount = friendshipService.getFollowingCount(user);
            long followerCount = friendshipService.getFollowersCount(user);
            long friendCount = friendshipService.getFriendsCount(user);
            
            System.out.println("=== Profile Debug for User: " + user.getUsername() + " (ID: " + userId + ") ===");
            System.out.println("Following Count: " + followingCount);
            System.out.println("Follower Count: " + followerCount);
            System.out.println("Friend Count: " + friendCount);
            
            response.put("followingCount", followingCount);
            response.put("followerCount", followerCount);
            response.put("friendCount", friendCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // ユーザーをフォロー
    @PostMapping("/{userId}/follow")
    public ResponseEntity<Map<String, Object>> followUser(@PathVariable Long userId, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String currentUsername = principal.getName();
            User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            User targetUser = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("対象のユーザーが見つかりません"));
            
            // フォローする
            friendshipService.follow(currentUser, targetUser);
            
            // 最新のフォロー状態を取得
            String relationship = friendshipService.getRelationship(currentUser, targetUser);
            
            response.put("success", true);
            response.put("message", "フォローしました");
            response.put("relationship", relationship);
            response.put("followingCount", friendshipService.getFollowingCount(currentUser));
            response.put("followerCount", friendshipService.getFollowersCount(currentUser));
            response.put("friendCount", friendshipService.getFriendsCount(currentUser));
            response.put("targetFollowerCount", friendshipService.getFollowersCount(targetUser));
            
            // WebSocketでフォロー数変更を全ユーザーに通知
            Map<String, Object> followUpdate = new HashMap<>();
            followUpdate.put("action", "follow");
            followUpdate.put("followerId", currentUser.getId());
            followUpdate.put("followedId", targetUser.getId());
            // フォローされた人の情報
            followUpdate.put("followedUserData", Map.of(
                "userId", targetUser.getId(),
                "followerCount", friendshipService.getFollowersCount(targetUser),
                "followingCount", friendshipService.getFollowingCount(targetUser),
                "friendCount", friendshipService.getFriendsCount(targetUser)
            ));
            // フォローした人の情報
            followUpdate.put("followerUserData", Map.of(
                "userId", currentUser.getId(),
                "followerCount", friendshipService.getFollowersCount(currentUser),
                "followingCount", friendshipService.getFollowingCount(currentUser),
                "friendCount", friendshipService.getFriendsCount(currentUser)
            ));
            messagingTemplate.convertAndSend("/topic/follow-updates", followUpdate);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "フォローに失敗しました: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // フォローを解除
    @PostMapping("/{userId}/unfollow")
    public ResponseEntity<Map<String, Object>> unfollowUser(@PathVariable Long userId, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String currentUsername = principal.getName();
            User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            User targetUser = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("対象のユーザーが見つかりません"));
            
            // フォローを解除
            friendshipService.unfollow(currentUser, targetUser);
            
            // 最新のフォロー状態を取得
            String relationship = friendshipService.getRelationship(currentUser, targetUser);
            
            response.put("success", true);
            response.put("message", "フォローを解除しました");
            response.put("relationship", relationship);
            response.put("followingCount", friendshipService.getFollowingCount(currentUser));
            response.put("followerCount", friendshipService.getFollowersCount(currentUser));
            response.put("friendCount", friendshipService.getFriendsCount(currentUser));
            response.put("targetFollowerCount", friendshipService.getFollowersCount(targetUser));
            
            // WebSocketでフォロー数変更を全ユーザーに通知
            Map<String, Object> followUpdate = new HashMap<>();
            followUpdate.put("action", "unfollow");
            followUpdate.put("followerId", currentUser.getId());
            followUpdate.put("followedId", targetUser.getId());
            // フォロー解除された人の情報
            followUpdate.put("followedUserData", Map.of(
                "userId", targetUser.getId(),
                "followerCount", friendshipService.getFollowersCount(targetUser),
                "followingCount", friendshipService.getFollowingCount(targetUser),
                "friendCount", friendshipService.getFriendsCount(targetUser)
            ));
            // フォロー解除した人の情報
            followUpdate.put("followerUserData", Map.of(
                "userId", currentUser.getId(),
                "followerCount", friendshipService.getFollowersCount(currentUser),
                "followingCount", friendshipService.getFollowingCount(currentUser),
                "friendCount", friendshipService.getFriendsCount(currentUser)
            ));
            messagingTemplate.convertAndSend("/topic/follow-updates", followUpdate);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "フォロー解除に失敗しました: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // ユーザーとの関係を取得
    @GetMapping("/{userId}/relationship")
    public ResponseEntity<Map<String, Object>> getRelationship(@PathVariable Long userId, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String currentUsername = principal.getName();
            User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            User targetUser = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("対象のユーザーが見つかりません"));
            
            String relationship = friendshipService.getRelationship(currentUser, targetUser);
            
            response.put("success", true);
            response.put("relationship", relationship);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
