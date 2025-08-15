package com.example.chatapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class DebugModeController {

    @GetMapping("/login/ondbg")
    public String enableDebugMode(HttpSession session) {
        session.setAttribute("debugMode", true);
        return "redirect:/login";
    }
    
    @GetMapping("/login/offdbg")
    public String disableDebugMode(HttpSession session) {
        session.setAttribute("debugMode", false);
        return "redirect:/login";
    }
}
