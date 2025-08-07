package com.example.chatapp.controller;

import com.example.chatapp.service.OnlineUserService;
import com.example.chatapp.service.OnlineUserService.OnlineUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * オンラインユーザー状態管理のAPIコントローラー
 */
@RestController
@RequestMapping("/api/online")
public class OnlineStatusController {

    @Autowired
    private OnlineUserService onlineUserService;

    /**
     * 現在のオンラインユーザー一覧を取得
     */
    @GetMapping("/users")
    public ResponseEntity<List<OnlineUserInfo>> getOnlineUsers() {
        List<OnlineUserInfo> onlineUsers = onlineUserService.getAllOnlineUsers();
        return ResponseEntity.ok(onlineUsers);
    }

    /**
     * 現在ログインしているユーザーのオンラインフレンド一覧を取得
     */
    @GetMapping("/friends")
    public ResponseEntity<List<OnlineUserInfo>> getOnlineFriends(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();
        List<OnlineUserInfo> onlineFriends = onlineUserService.getOnlineFriends(username);
        return ResponseEntity.ok(onlineFriends);
    }

    /**
     * オンラインユーザー数を取得
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getOnlineUserCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalCount", onlineUserService.getOnlineUserCount());
        response.put("statusCounts", onlineUserService.getOnlineUserCountByStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * 特定ユーザーのオンライン状態を取得
     */
    @GetMapping("/status/{username}")
    public ResponseEntity<Map<String, Object>> getUserOnlineStatus(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        
        OnlineUserInfo userInfo = onlineUserService.getOnlineUserInfo(username);
        if (userInfo != null) {
            response.put("isOnline", true);
            response.put("status", userInfo.getStatus());
            response.put("lastActiveTime", userInfo.getLastActiveTime());
        } else {
            response.put("isOnline", false);
            response.put("status", "offline");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 現在のユーザーの状態を更新
     */
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (auth == null || !auth.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "認証が必要です");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();
        String status = request.get("status");

        if (status == null || (!status.equals("online") && !status.equals("away") && !status.equals("busy"))) {
            response.put("success", false);
            response.put("message", "無効な状態です");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            onlineUserService.updateUserStatus(username, status);
            response.put("success", true);
            response.put("message", "状態を更新しました");
            response.put("status", status);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "状態の更新に失敗しました: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * アクティビティを更新（ハートビート用）
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        if (auth == null || !auth.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "認証が必要です");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();
        
        try {
            onlineUserService.updateUserActivity(username);
            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "ハートビートの更新に失敗しました");
        }

        return ResponseEntity.ok(response);
    }
}
