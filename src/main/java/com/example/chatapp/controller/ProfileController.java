package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.entity.Friendship;
import com.example.chatapp.service.UserProfileService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.FriendshipService;
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
        
        Optional<UserProfile> profileOpt = userProfileService.getProfileByUserId(userId);
        if (profileOpt.isEmpty()) {
            return "redirect:/profile";
        }
        
        UserProfile profile = profileOpt.get();
        User targetUser = profile.getUser();
        
        // 自分のプロフィールの場合はリダイレクト
        if (targetUser.equals(currentUser)) {
            return "redirect:/profile";
        }
        
        // プロフィール表示権限チェック
        if (!userProfileService.canViewProfile(profile, currentUser)) {
            model.addAttribute("error", "このプロフィールを表示する権限がありません");
            return "error/403";
        }
        
        // フレンド関係の状態を取得
        String friendshipStatus = getFriendshipStatus(currentUser, targetUser);
        
        model.addAttribute("profile", profile);
        model.addAttribute("user", targetUser);
        model.addAttribute("isOwnProfile", false);
        model.addAttribute("friendshipStatus", friendshipStatus);
        
        return "profile/view";
    }
    
    /**
     * フレンド関係の状態を取得
     */
    private String getFriendshipStatus(User currentUser, User targetUser) {
        // 既にフレンドかチェック
        if (friendshipService.areFriends(currentUser, targetUser)) {
            return "FRIEND";
        }
        
        // 自分が送信済みの申請があるかチェック
        Optional<Friendship> sentRequest = friendshipService.findFriendshipBetweenUsers(currentUser, targetUser);
        if (sentRequest.isPresent()) {
            Friendship friendship = sentRequest.get();
            if (friendship.getRequester().equals(currentUser) && 
                friendship.getStatus() == Friendship.FriendshipStatus.PENDING) {
                return "REQUEST_SENT";
            }
            // 相手から申請が来ている場合
            if (friendship.getAddressee().equals(currentUser) && 
                friendship.getStatus() == Friendship.FriendshipStatus.PENDING) {
                return "REQUEST_RECEIVED";
            }
            // ブロックされている場合
            if (friendship.getStatus() == Friendship.FriendshipStatus.BLOCKED) {
                return "BLOCKED";
            }
        }
        
        // 何も関係がない場合
        return "NONE";
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
                              Authentication auth, 
                              RedirectAttributes redirectAttributes) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            userProfileService.updateProfile(user, profileData);
            redirectAttributes.addFlashAttribute("successMessage", "プロフィールを更新しました");
            
        } catch (Exception e) {
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
    public String showOnlineUsers(Model model) {
        List<UserProfile> onlineUsers = userProfileService.getOnlineUsers();
        long onlineCount = userProfileService.getOnlineUserCount();
        
        model.addAttribute("onlineUsers", onlineUsers);
        model.addAttribute("onlineCount", onlineCount);
        
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
