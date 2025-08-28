package com.example.chatapp.config;

import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404エラー - 存在しないページへのアクセス
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public void handleNotFound(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 静的リソースかどうかをチェック
        String requestURI = request.getRequestURI();
        if (isStaticResource(requestURI)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // 通常のページアクセスはloginにリダイレクト
        response.sendRedirect("/login");
    }

    /**
     * 認証エラー
     */
    @ExceptionHandler(AuthenticationException.class)
    public void handleAuthenticationException(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("/login");
    }

    /**
     * その他の例外
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(HttpServletRequest request, Exception ex) {
        String requestURI = request.getRequestURI();
        
        // 既にloginページにいる場合はエラーページを表示
        if (requestURI.equals("/login") || requestURI.equals("/register")) {
            return "error/500"; // エラーページがある場合
        }
        
        // その他の場合はloginにリダイレクト
        return "redirect:/login";
    }

    /**
     * 静的リソースかどうかを判定
     */
    private boolean isStaticResource(String requestURI) {
        return requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/") ||
               requestURI.endsWith(".css") ||
               requestURI.endsWith(".js") ||
               requestURI.endsWith(".png") ||
               requestURI.endsWith(".jpg") ||
               requestURI.endsWith(".jpeg") ||
               requestURI.endsWith(".gif") ||
               requestURI.endsWith(".ico") ||
               requestURI.equals("/manifest.json") ||
               requestURI.equals("/sw.js");
    }
}
