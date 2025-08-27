package com.example.chatapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class DebugModeController {

    @GetMapping("/login/ondbg")
    public String enableDebugMode(HttpSession session) {
        session.setAttribute("debugMode", true);
        session.setAttribute("debugModeAccess", true);
        return "redirect:/login/dbgmode";
    }
    
    @GetMapping("/login/offdbg")
    public String disableDebugMode(HttpSession session) {
        session.setAttribute("debugMode", false);
        return "redirect:/login";
    }
    
    @GetMapping("/login/dbgmode")
    public String debugModePage(HttpSession session, Model model) {
        // 直接アクセスを防ぐため、デバッグフラグをチェック
        Boolean debugFlag = (Boolean) session.getAttribute("debugModeAccess");
        if (debugFlag == null || !debugFlag) {
            // デバッグモードアクセス権限がない場合は通常ログインページへリダイレクト
            return "redirect:/login";
        }
        
        // アクセス権限フラグをクリア（一回限りのアクセス）
        session.removeAttribute("debugModeAccess");
        
        // デバッグモードを有効化
        session.setAttribute("debugMode", true);
        model.addAttribute("debugMode", true);
        
        // デバッグモード専用ページを表示
        return "debug-login";
    }
}
