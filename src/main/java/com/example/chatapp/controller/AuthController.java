package com.example.chatapp.controller;

import com.example.chatapp.dto.UserRegistrationDto;
import com.example.chatapp.entity.User;
import com.example.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           @RequestParam(value = "success", required = false) String success,
                           HttpSession session,
                           Model model) {
        if (error != null) {
            model.addAttribute("error", "ユーザー名またはパスワードが正しくありません");
        }
        if (logout != null) {
            model.addAttribute("message", "ログアウトしました");
        }
        if (success != null) {
            model.addAttribute("success", "登録が完了しました。ログインしてください。");
        }
        
        // デバッグモード情報を取得してテンプレートに渡す
        Boolean debugMode = (Boolean) session.getAttribute("debugMode");
        boolean isDebugMode = debugMode != null && debugMode;
        model.addAttribute("debugMode", isDebugMode);
        
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                             BindingResult result,
                             Model model,
                             HttpSession session) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            User user = userService.registerUser(
                userDto.getUsername(),
                userDto.getPassword(),
                userDto.getEmail()
            );
            
            System.out.println("=== 新規登録完了 ===");
            System.out.println("ユーザーID: " + user.getId());
            System.out.println("ユーザー名: " + user.getUsername());
            
            // セッションに新規ユーザーIDを保存してプロフィール作成画面へ
            session.setAttribute("newUserId", user.getId());
            
            System.out.println("セッションID: " + session.getId());
            System.out.println("セッションに保存したnewUserId: " + session.getAttribute("newUserId"));
            System.out.println("=====================");
            
            return "redirect:/profile/create";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
