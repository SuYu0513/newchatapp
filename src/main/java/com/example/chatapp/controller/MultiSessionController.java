package com.example.chatapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;

@Controller
@RequestMapping("/multi-session")
public class MultiSessionController {

    @GetMapping("/login")
    public String multiSessionLogin(
            @RequestParam(value = "tab", required = false) String tabId,
            HttpServletRequest request, 
            Model model) {
        
        // タブIDが指定されていない場合は新しいIDを生成
        if (tabId == null || tabId.isEmpty()) {
            tabId = "tab-" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        // 現在のセッション情報を取得
        HttpSession session = request.getSession(true);
        String sessionId = session.getId();
        
        model.addAttribute("tabId", tabId);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("loginUrl", "/multi-session/login?tab=" + tabId);
        
        return "multi-session/login";
    }

    @GetMapping("/switch-user")
    public String switchUser(
            @RequestParam(value = "tab", required = false) String tabId,
            HttpServletRequest request,
            Model model) {
        
        if (tabId == null || tabId.isEmpty()) {
            tabId = "tab-" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        // 現在のセッションを無効化して新しいセッションを作成
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        
        HttpSession newSession = request.getSession(true);
        String newSessionId = newSession.getId();
        
        model.addAttribute("tabId", tabId);
        model.addAttribute("newSessionId", newSessionId);
        model.addAttribute("loginUrl", "/login?tab=" + tabId);
        
        return "multi-session/switch-user";
    }

    @GetMapping("/info")
    public String multiSessionInfo(
            @RequestParam(value = "tab", required = false) String tabId,
            HttpServletRequest request,
            Model model) {
        
        HttpSession session = request.getSession(false);
        
        model.addAttribute("tabId", tabId);
        model.addAttribute("sessionExists", session != null);
        model.addAttribute("sessionId", session != null ? session.getId() : "なし");
        
        return "multi-session/info";
    }
}
