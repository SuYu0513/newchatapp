package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.service.UserProfileService;
import com.example.chatapp.service.UserService;
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
        
        // プロフィール表示権限チェック
        if (!userProfileService.canViewProfile(profile, currentUser)) {
            model.addAttribute("error", "このプロフィールを表示する権限がありません");
            return "error/403";
        }
        
        model.addAttribute("profile", profile);
        model.addAttribute("user", profile.getUser());
        model.addAttribute("isOwnProfile", profile.getUser().equals(currentUser));
        
        return "profile/view";
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
