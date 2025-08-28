package com.example.chatapp.controller;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RandomMatch;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.service.RandomMatchingService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ランダムマッチング機能のコントローラー
 */
@Controller
@RequestMapping("/random-matching")
public class RandomMatchingController {

    @Autowired
    private RandomMatchingService randomMatchingService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserProfileService userProfileService;

    /**
     * ランダムマッチングページの表示
     */
    @GetMapping
    public String showRandomMatching(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        UserProfile profile = userProfileService.getOrCreateProfile(user);
        
        // ランダムマッチングが許可されているかチェック
        if (!profile.getAllowRandomMatching()) {
            model.addAttribute("error", "ランダムマッチングを使用するには、プロフィール設定で許可してください");
            return "random-matching/not-allowed";
        }

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);

        return "random-matching/index";
    }

    /**
     * マッチング相手の検索とランダムマッチ開始
     */
    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startMatching(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            UserProfile profile = userProfileService.getOrCreateProfile(user);
            
            // ランダムマッチングが許可されているかチェック
            if (!profile.getAllowRandomMatching()) {
                response.put("success", false);
                response.put("message", "ランダムマッチングが許可されていません");
                return ResponseEntity.ok(response);
            }

            // 相性の良いマッチング相手を探す
            User matchedUser = randomMatchingService.findBestMatch(user);
            
            if (matchedUser == null) {
                response.put("success", false);
                response.put("message", "現在マッチング可能なユーザーが見つかりません");
                return ResponseEntity.ok(response);
            }

            // ランダムマッチを作成（ルームは作成しない）
            RandomMatch match = randomMatchingService.createMatch(user, matchedUser);

            response.put("success", true);
            response.put("matchedUser", Map.of(
                "id", matchedUser.getId(),
                "username", matchedUser.getUsername(),
                "displayName", userProfileService.getOrCreateProfile(matchedUser).getDisplayNameOrUsername()
            ));
            response.put("matchId", match.getId());
            // roomIdは返さない（チャット開始時に作成）

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "マッチングに失敗しました: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * チャット開始（ルーム作成または既存ルーム使用）
     */
    @PostMapping("/start-chat/{matchId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startChat(@PathVariable Long matchId, 
                                                        Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            // マッチ情報を取得
            RandomMatch match = randomMatchingService.getMatch(matchId)
                .orElseThrow(() -> new RuntimeException("マッチが見つかりません"));

            // ユーザーがこのマッチに参加しているかチェック
            if (!match.involvesUser(user)) {
                response.put("success", false);
                response.put("message", "このマッチにアクセスする権限がありません");
                return ResponseEntity.ok(response);
            }

            // ルームを作成または既存ルームを取得
            ChatRoom chatRoom = randomMatchingService.getOrCreateChatRoom(match);

            response.put("success", true);
            response.put("roomId", chatRoom.getId());
            response.put("roomName", chatRoom.getName());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "チャット開始に失敗しました: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * マッチング履歴の表示
     */
    @GetMapping("/history")
    public String showHistory(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        List<RandomMatchingService.GroupedMatchHistory> groupedHistory = 
            randomMatchingService.getGroupedMatchHistory(user);
        model.addAttribute("groupedHistory", groupedHistory);

        return "random-matching/history";
    }

    /**
     * マッチング統計情報の取得
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMatchingStats(Authentication auth) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // グローバル統計データを取得
            long totalMatches = randomMatchingService.getTotalMatches();
            long activeMatches = randomMatchingService.getActiveMatches();
            long totalUsers = userService.getTotalUserCount();
            
            stats.put("totalMatches", totalMatches);
            stats.put("activeMatches", activeMatches);
            stats.put("totalUsers", totalUsers);
            stats.put("success", true);

        } catch (Exception e) {
            stats.put("error", "統計情報の取得に失敗しました");
            stats.put("success", false);
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * マッチング設定の表示
     */
    @GetMapping("/settings")
    public String showSettings(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        UserProfile profile = userProfileService.getOrCreateProfile(user);
        model.addAttribute("profile", profile);

        return "random-matching/settings";
    }

    /**
     * マッチング設定の保存
     */
    @PostMapping("/settings")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveSettings(
            @RequestParam(value = "allowRandomMatching", defaultValue = "false") boolean allowRandomMatching,
            @RequestParam(value = "autoMatching", defaultValue = "false") boolean autoMatching,
            @RequestParam(value = "minAge", defaultValue = "18") int minAge,
            @RequestParam(value = "maxAge", defaultValue = "50") int maxAge,
            @RequestParam(value = "profileVisibilityLevel", defaultValue = "detailed") String profileVisibilityLevel,
            @RequestParam(value = "avoidBlockedUsers", defaultValue = "false") boolean avoidBlockedUsers,
            @RequestParam(value = "matchNotification", defaultValue = "false") boolean matchNotification,
            @RequestParam(value = "messageNotification", defaultValue = "false") boolean messageNotification,
            Authentication auth) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            UserProfile profile = userProfileService.getOrCreateProfile(user);
            
            // 設定を更新
            profile.setAllowRandomMatching(allowRandomMatching);
            
            // プロフィールを保存
            userProfileService.updateProfile(user, profile);

            response.put("success", true);
            response.put("message", "設定を保存しました");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "設定の保存に失敗しました: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * マッチングの終了
     */
    @PostMapping("/end/{matchId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> endMatch(@PathVariable Long matchId, 
                                                       Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            randomMatchingService.endMatch(matchId, user);

            response.put("success", true);
            response.put("message", "マッチングを終了しました");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "マッチング終了に失敗しました: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }


}
