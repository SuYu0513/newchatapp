package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.service.UserProfileService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.FriendshipService;
import com.example.chatapp.service.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * ユーザープロフィール管理コントローラー
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserService userService;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private OnlineUserService onlineUserService;

    /**
     * プロフィール表示
     */
    @GetMapping
    public String showProfile(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        UserProfile profile = userProfileService.getOrCreateProfile(user);
        model.addAttribute("profile", profile);
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("isOwnProfile", true);
        
        return "profile/view";
    }

    /**
     * 他のユーザーのプロフィール表示
     */
    @GetMapping("/{userId}")
    public String showUserProfile(@PathVariable Long userId, Authentication auth, Model model) {
        User currentUser = userService.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // 対象ユーザーを取得
        Optional<User> targetUserOpt = userService.findById(userId);
        if (targetUserOpt.isEmpty()) {
            model.addAttribute("error", "ユーザーが見つかりません");
            return "error/404";
        }
        
        User targetUser = targetUserOpt.get();
        
        // 自分のプロフィールの場合はリダイレクト
        if (targetUser.equals(currentUser)) {
            return "redirect:/profile";
        }
        
        // プロフィールを取得または作成
        UserProfile profile = userProfileService.getOrCreateProfile(targetUser);
        
        // プロフィール表示権限チェック
        if (!userProfileService.canViewProfile(profile, currentUser)) {
            model.addAttribute("error", "このプロフィールを表示する権限がありません");
            return "error/403";
        }
        
        // フレンド関係の状態を取得
        String friendshipStatus = getFriendshipStatus(currentUser, targetUser);
        
        // デバッグログ
        System.out.println("=== プロフィール表示デバッグ ===");
        System.out.println("現在のユーザー: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");
        System.out.println("対象ユーザー: " + targetUser.getUsername() + " (ID: " + targetUser.getId() + ")");
        System.out.println("フレンド関係ステータス: " + friendshipStatus);
        System.out.println("===============================");
        
        model.addAttribute("profile", profile);
        model.addAttribute("user", targetUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwnProfile", false);
        model.addAttribute("friendshipStatus", friendshipStatus);
        
        return "profile/view";
    }
    
    /**
     * フレンド関係の状態を取得
     */
    private String getFriendshipStatus(User currentUser, User targetUser) {
        String status = friendshipService.getFriendshipStatus(currentUser, targetUser);
        
        // FriendshipServiceの戻り値をプロフィール画面用の値に変換
        switch (status) {
            case "friends":
                return "FRIEND";
            case "sent_request":
                return "REQUEST_SENT";
            case "received_request":
                return "REQUEST_RECEIVED";
            case "blocked_by_you":
            case "blocked_by_them":
                return "BLOCKED";
            case "none":
            case "declined":
            default:
                return "NONE";
        }
    }

    /**
     * プロフィール編集画面
     */
    @GetMapping("/edit")
    public String showEditProfile(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        UserProfile profile = userProfileService.getOrCreateProfile(user);
        model.addAttribute("profile", profile);
        model.addAttribute("onlineStatuses", UserProfile.OnlineStatus.values());
        model.addAttribute("privacyLevels", UserProfile.PrivacyLevel.values());
        
        return "profile/edit";
    }

    /**
     * プロフィール更新
     */
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute UserProfile profileData,
                              @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                              Authentication auth, 
                              RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== プロフィール更新開始 ===");
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            System.out.println("ユーザー: " + user.getUsername());
            System.out.println("アバターファイル: " + (avatarFile != null ? avatarFile.getOriginalFilename() : "null"));
            System.out.println("ファイルが空か: " + (avatarFile != null ? avatarFile.isEmpty() : "null"));
            
            // プロフィール情報を更新
            userProfileService.updateProfile(user, profileData);
            System.out.println("プロフィール基本情報更新完了");
            
            // アバター画像がアップロードされている場合は処理
            if (avatarFile != null && !avatarFile.isEmpty()) {
                System.out.println("アバター画像をアップロード中...");
                try {
                    userProfileService.uploadAvatar(user, avatarFile);
                    redirectAttributes.addFlashAttribute("successMessage", "プロフィールとアバター画像を更新しました");
                    System.out.println("アバター画像アップロード成功");
                } catch (IOException e) {
                    System.err.println("アバター画像アップロードエラー（IO）: " + e.getMessage());
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("successMessage", "プロフィールを更新しました（画像のアップロードに失敗: " + e.getMessage() + "）");
                } catch (IllegalArgumentException e) {
                    System.err.println("アバター画像アップロードエラー（引数）: " + e.getMessage());
                    redirectAttributes.addFlashAttribute("successMessage", "プロフィールを更新しました（画像エラー: " + e.getMessage() + "）");
                }
            } else {
                System.out.println("アバター画像なし");
                redirectAttributes.addFlashAttribute("successMessage", "プロフィールを更新しました");
            }
            
            System.out.println("=== プロフィール更新終了 ===");
            
        } catch (Exception e) {
            System.err.println("プロフィール更新エラー: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "プロフィールの更新に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/profile";
    }

    /**
     * アバター画像アップロード
     */
    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            userProfileService.uploadAvatar(user, file);
            redirectAttributes.addFlashAttribute("successMessage", "アバター画像を更新しました");
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "予期しないエラーが発生しました");
        }
        
        return "redirect:/profile/edit";
    }

    /**
     * プロフィール検索
     */
    @GetMapping("/search")
    public String searchProfiles(@RequestParam(required = false) String keyword, Model model) {
        List<UserProfile> searchResults = List.of();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchResults = userProfileService.searchProfiles(keyword);
        }
        
        model.addAttribute("keyword", keyword);
        model.addAttribute("searchResults", searchResults);
        
        return "profile/search";
    }

    /**
     * オンラインユーザー一覧
     */
    @GetMapping("/online")
    public String showOnlineUsers(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String username = auth.getName();
        User currentUser = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // オンラインフレンドを取得
        List<OnlineUserService.OnlineUserInfo> onlineFriends = onlineUserService.getOnlineFriends(username);
        
        // すべてのオンラインユーザーも取得（必要に応じて）
        List<OnlineUserService.OnlineUserInfo> allOnlineUsers = onlineUserService.getAllOnlineUsers();
        long onlineCount = onlineUserService.getOnlineUserCount();
        
        model.addAttribute("onlineFriends", onlineFriends);
        model.addAttribute("allOnlineUsers", allOnlineUsers);
        model.addAttribute("onlineCount", onlineCount);
        model.addAttribute("currentUser", currentUser);
        
        return "profile/online";
    }

    /**
     * オンラインステータス更新 (AJAX)
     */
    @PostMapping("/status")
    @ResponseBody
    public ResponseEntity<?> updateOnlineStatus(@RequestParam String status, Authentication auth) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            UserProfile.OnlineStatus onlineStatus = UserProfile.OnlineStatus.valueOf(status.toUpperCase());
            userProfileService.updateOnlineStatus(user, onlineStatus);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("ステータスの更新に失敗しました");
        }
    }

    /**
     * 最終アクセス時間更新 (AJAX)
     */
    @PostMapping("/heartbeat")
    @ResponseBody
    public ResponseEntity<?> updateHeartbeat(Authentication auth) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            userProfileService.updateLastSeen(user);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("ハートビートの更新に失敗しました");
        }
    }
}
