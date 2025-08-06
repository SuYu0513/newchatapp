package com.example.chatapp.controller;

import com.example.chatapp.dto.UserRegistrationDto;
import com.example.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
                             Model model) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(
                userDto.getUsername(),
                userDto.getPassword(),
                userDto.getEmail()
            );
            return "redirect:/login?success=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
