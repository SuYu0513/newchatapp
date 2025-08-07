package com.example.chatapp.config;

import com.example.chatapp.service.OnlineUserService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    @Lazy
    private OnlineUserService onlineUserService;
    
    @Autowired
    @Lazy
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                      Authentication authentication) throws IOException, ServletException {
        
        String username = authentication.getName();
        System.out.println("=== ログイン成功ハンドラー実行 ===");
        System.out.println("ユーザー名: " + username);
        
        try {
            // ユーザーをオンライン状態にする
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                String sessionId = request.getSession().getId();
                onlineUserService.setUserOnline(username, sessionId);
                System.out.println("ユーザーをオンライン状態に設定: " + username + " (セッション: " + sessionId + ")");
            } else {
                System.err.println("ユーザーが見つかりません: " + username);
            }
        } catch (Exception e) {
            System.err.println("オンライン状態設定エラー: " + e.getMessage());
            e.printStackTrace();
        }
        
        // デフォルトのリダイレクト先にリダイレクト
        response.sendRedirect("/chat");
    }
}
