package com.example.chatapp.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        // 認証されているかチェック
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");
        
        // 未認証の場合は必ずloginにリダイレクト
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        // 認証済みの場合、エラーの種類に応じて処理
        Object status = request.getAttribute("javax.servlet.error.status_code");
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            switch (statusCode) {
                case 404:
                    // 認証済みでも存在しないページは最初からやり直し
                    return "redirect:/chat";
                case 403:
                    // アクセス拒否もloginに戻す
                    return "redirect:/login";
                default:
                    // その他のエラーもloginに戻す
                    return "redirect:/login";
            }
        }
        
        // デフォルトはloginにリダイレクト
        return "redirect:/login";
    }
}
