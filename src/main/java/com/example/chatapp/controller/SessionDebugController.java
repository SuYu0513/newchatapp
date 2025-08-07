package com.example.chatapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/debug")
public class SessionDebugController {

    @GetMapping("/session")
    public String showSessionInfo(HttpServletRequest request, Model model, Principal principal) {
        HttpSession session = request.getSession(false);
        
        Map<String, Object> sessionInfo = new HashMap<>();
        
        if (session != null) {
            sessionInfo.put("sessionId", session.getId());
            sessionInfo.put("creationTime", new java.util.Date(session.getCreationTime()));
            sessionInfo.put("lastAccessedTime", new java.util.Date(session.getLastAccessedTime()));
            sessionInfo.put("maxInactiveInterval", session.getMaxInactiveInterval());
            sessionInfo.put("isNew", session.isNew());
            
            // セッション属性を取得
            Map<String, Object> attributes = new HashMap<>();
            Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                Object value = session.getAttribute(name);
                attributes.put(name, value.toString());
            }
            sessionInfo.put("attributes", attributes);
        }
        
        model.addAttribute("sessionInfo", sessionInfo);
        model.addAttribute("principal", principal != null ? principal.getName() : "未ログイン");
        model.addAttribute("remoteAddr", request.getRemoteAddr());
        model.addAttribute("userAgent", request.getHeader("User-Agent"));
        
        return "debug/session";
    }
}
