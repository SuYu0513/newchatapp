package com.example.chatapp.config;

import com.example.chatapp.service.OnlineUserService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.UserProfileService;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Autowired
    @Lazy
    private OnlineUserService onlineUserService;
    
    @Autowired
    @Lazy
    private UserService userService;
    
    @Autowired
    @Lazy
    private UserProfileService userProfileService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, 
                               Authentication authentication) throws IOException, ServletException {
        
        if (authentication != null) {
            String username = authentication.getName();
            System.out.println("=== ログアウト成功ハンドラー実行 ===");
            System.out.println("ユーザー名: " + username);
            
            try {
                // ユーザーをオフライン状態にする
                onlineUserService.setUserOffline(username);
                System.out.println("ユーザーをオフライン状態に設定: " + username);
                
                // UserProfileのオンラインステータスもOFFLINEに更新
                Optional<User> userOpt = userService.findByUsername(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    userProfileService.updateOnlineStatus(user, UserProfile.OnlineStatus.OFFLINE);
                    System.out.println("ユーザープロフィールのステータスをOFFLINEに更新: " + username);
                }
                
            } catch (Exception e) {
                System.err.println("オフライン状態設定エラー: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // ログアウト後のリダイレクト先
        response.sendRedirect("/login?logout=true");
    }
}
