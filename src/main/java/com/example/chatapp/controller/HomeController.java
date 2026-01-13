package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.repository.UserProfileRepository;
import com.example.chatapp.service.PostService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
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

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        System.out.println("=== HomeController /home にアクセス ===");
        if (authentication != null) {
            String username = authentication.getName();
            System.out.println("認証済みユーザー: " + username);
            model.addAttribute("username", username);
            model.addAttribute("posts", postService.getAllPosts());
            
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
                                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                                String filename = UUID.randomUUID().toString() + extension;
                                Path filePath = uploadPath.resolve(filename);
                                Files.copy(file.getInputStream(), filePath);
                                uploadedFilePaths.add("/uploads/posts/" + filename);
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
                postService.createPost(userOpt.get(), content.trim(), mediaPathsStr);
                redirectAttributes.addFlashAttribute("success", "投稿しました！");
            }
        }
        return "redirect:/home";
    }
}
