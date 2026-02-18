package com.example.chatapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRedirectInterceptor implements HandlerInterceptor {

    @Value("${app.debug.enabled:false}")
    private boolean debugEnabled;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 静的リソースや許可されたパスはスキップ
        if (isAllowedPath(requestURI)) {
            return true;
        }

        // 認証状態をチェック
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");

        // 未認証でlogin/register以外にアクセスしようとした場合は強制的にloginにリダイレクト
        if (!isAuthenticated && !requestURI.equals("/login") && !requestURI.equals("/register")) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }

    private boolean isAllowedPath(String requestURI) {
        // 常に許可するパス
        boolean allowed = requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/") ||
               requestURI.equals("/manifest.json") ||
               requestURI.equals("/sw.js") ||
               requestURI.startsWith("/icon-") ||
               requestURI.equals("/apple-touch-icon.png") ||
               requestURI.equals("/login") ||
               requestURI.equals("/register") ||
               requestURI.equals("/profile/create") ||
               requestURI.equals("/error") ||
               requestURI.equals("/passreset") ||
               requestURI.startsWith("/api/auth/");

        // デバッグモード時のみ許可するパス
        if (debugEnabled) {
            allowed = allowed ||
                requestURI.startsWith("/login/") ||
                requestURI.equals("/icon-generator.html");
        }

        // H2コンソール有効時のみ許可するパス
        if (h2ConsoleEnabled) {
            allowed = allowed || requestURI.startsWith("/h2-console/");
        }

        return allowed;
    }
}
