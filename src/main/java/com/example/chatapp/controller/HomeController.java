package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.entity.Post;
import com.example.chatapp.repository.UserProfileRepository;
import com.example.chatapp.service.PostService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class HomeController {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ChatRoomService chatRoomService;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private FriendshipService friendshipService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        System.out.println("=== HomeController /home にアクセス ===");
        if (authentication != null) {
            String username = authentication.getName();
            System.out.println("認証済みユーザー: " + username);
            model.addAttribute("username", username);
            model.addAttribute("posts", postService.getAllPosts());

            // ユーザーをメインルームに自動参加させる
            try {
                chatRoomService.ensureUserInMainRoom(username);
            } catch (Exception e) {
                System.err.println("メインルーム自動参加エラー: " + e.getMessage());
            }

            // ユーザー情報を追加（マイページ用）
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);

                // プロフィール情報を追加
                Optional<UserProfile> profileOpt = userProfileRepository.findByUser(user);
                if (profileOpt.isPresent()) {
                    model.addAttribute("profile", profileOpt.get());
                }

                // フォロー/フォロワー/フレンド数を追加
                model.addAttribute("followingCount", friendshipService.getFollowingCount(user));
                model.addAttribute("followerCount", friendshipService.getFollowersCount(user));
                model.addAttribute("friendCount", friendshipService.getFriends(user).size());
            }

            // ルーム情報も追加
            model.addAttribute("userRooms", chatRoomService.getUserChatRooms(username));
            model.addAttribute("availableRooms", chatRoomService.getAvailablePublicRooms(username));
        }
        System.out.println("=== main-app テンプレートを返します ===");
        return "main-app"; // 統合画面を表示
    }
    
    @PostMapping("/home/post")
    public String createPost(@RequestParam("content") String content,
                           @RequestParam(value = "media", required = false) MultipartFile[] mediaFiles,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        if (authentication != null && content != null && !content.trim().isEmpty()) {
            Optional<User> userOpt = userService.findByUsername(authentication.getName());
            if (userOpt.isPresent()) {
                List<String> uploadedFilePaths = new ArrayList<>();
                
                // ファイルアップロード処理
                if (mediaFiles != null && mediaFiles.length > 0) {
                    String uploadDir = "src/main/resources/static/uploads/posts/";
                    Path uploadPath = Paths.get(uploadDir);
                    
                    try {
                        if (!Files.exists(uploadPath)) {
                            Files.createDirectories(uploadPath);
                        }
                        
                        for (MultipartFile file : mediaFiles) {
                            if (!file.isEmpty()) {
                                String originalFilename = file.getOriginalFilename();
                                if (originalFilename != null) {
                                    String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                                    String filename = UUID.randomUUID().toString() + extension;
                                    Path filePath = uploadPath.resolve(filename);
                                    Files.copy(file.getInputStream(), filePath);
                                    uploadedFilePaths.add("/uploads/posts/" + filename);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        redirectAttributes.addFlashAttribute("error", "ファイルのアップロードに失敗しました。");
                        return "redirect:/home";
                    }
                }
                
                // 投稿作成
                String mediaPathsStr = uploadedFilePaths.isEmpty() ? null : String.join(",", uploadedFilePaths);
                Post newPost = postService.createPost(userOpt.get(), content.trim(), mediaPathsStr);
                
                // WebSocketで新しい投稿を全ユーザーに通知
                Map<String, Object> postData = new HashMap<>();
                postData.put("id", newPost.getId());
                postData.put("content", newPost.getContent());
                postData.put("mediaPath", newPost.getMediaPath());
                postData.put("createdAt", newPost.getCreatedAt().toString());
                
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", newPost.getUser().getId());
                userData.put("username", newPost.getUser().getUsername());
                
                UserProfile profile = newPost.getUser().getProfile();
                if (profile != null) {
                    userData.put("displayName", profile.getDisplayName());
                    userData.put("avatarUrl", profile.getAvatarUrl());
                }
                
                postData.put("user", userData);
                
                messagingTemplate.convertAndSend("/topic/posts", postData);
                
                redirectAttributes.addFlashAttribute("success", "投稿しました！");
            }
        }
        return "redirect:/home";
    }
}
