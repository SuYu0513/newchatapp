package com.example.chatapp.controller;

import com.example.chatapp.entity.FavoriteTag;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.repository.FavoriteTagRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.repository.UserProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profile")
public class ProfileCreationController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private FavoriteTagRepository favoriteTagRepository;
    
    @GetMapping("/create")
    public String showProfileCreationPage(HttpSession session, Model model) {
        System.out.println("=== プロフィール作成画面 GET ===");
        System.out.println("セッションID: " + session.getId());
        
        Long userId = (Long) session.getAttribute("newUserId");
        System.out.println("セッションから取得したnewUserId: " + userId);
        
        if (userId == null) {
            System.out.println("newUserIdがnullのためログイン画面にリダイレクト");
            System.out.println("================================");
            return "redirect:/login";
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            System.out.println("ユーザーが見つからないためログイン画面にリダイレクト");
            System.out.println("================================");
            return "redirect:/login";
        }
        
        System.out.println("ユーザー発見: " + user.getUsername());
        System.out.println("================================");
        
        // 人気の「好きなもの」タグトップ10を取得
        List<FavoriteTag> topTags = favoriteTagRepository.findTop10ByOrderByUsageCountDesc(PageRequest.of(0, 10));
        
        model.addAttribute("user", user);
        model.addAttribute("topTags", topTags);
        return "create-profile";
    }
    
    @PostMapping("/create")
    public String createProfile(@RequestParam("displayName") String displayName,
                               @RequestParam(value = "bio", required = false) String bio,
                               @RequestParam(value = "favoriteThings", required = false) String favoriteThings,
                               HttpSession session,
                               HttpServletRequest request) {
        Long userId = (Long) session.getAttribute("newUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        // UserProfileを取得または作成
        UserProfile userProfile = userProfileRepository.findByUser(user)
                .orElse(new UserProfile(user));

        // プロフィール情報を更新
        userProfile.setDisplayName(displayName);
        userProfile.setBio(bio != null ? bio : "");

        // 好きなものを保存（カンマ区切り）
        if (favoriteThings != null && !favoriteThings.trim().isEmpty()) {
            userProfile.setFavoriteThings(favoriteThings);

            // 各タグの使用回数を更新
            List<String> tags = Arrays.stream(favoriteThings.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            for (String tagName : tags) {
                FavoriteTag tag = favoriteTagRepository.findByTagName(tagName)
                        .orElse(new FavoriteTag(tagName));
                tag.setUsageCount(tag.getUsageCount() + 1);
                favoriteTagRepository.save(tag);
            }
        } else {
            userProfile.setFavoriteThings("");
        }

        userProfileRepository.save(userProfile);

        // 自動ログイン処理
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // セッションに認証情報を保存
        request.getSession().setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        );

        // セッションから一時データをクリア
        session.removeAttribute("newUserId");

        // ログイン後と同じホーム画面にリダイレクト
        return "redirect:/home";
    }
}
