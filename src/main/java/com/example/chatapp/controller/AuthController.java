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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home(java.security.Principal principal) {
        // 既にログイン済みの場合はホームにリダイレクト
        if (principal != null) {
            return "redirect:/home";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           @RequestParam(value = "success", required = false) String success,
                           HttpSession session,
                           Model model,
                           java.security.Principal principal) {
        
        // 既にログイン済みの場合はホームにリダイレクト
        if (principal != null) {
            return "redirect:/home";
        }
        
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
    
    @GetMapping("/password-reset")
    public String passwordResetPage() {
        return "password-reset";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                             BindingResult result,
                             Model model,
                             HttpSession session) {
        if (result.hasErrors()) {
            return "register";
        }
        
        // パスワード一致チェック
        if (!userDto.isPasswordMatching()) {
            model.addAttribute("error", "パスワードが一致しません");
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
    
    // パスワードリセット用エンドポイント
    @PostMapping("/api/auth/verify-user")
    @ResponseBody
    public Map<String, Object> verifyUser(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = request.get("username");
            String email = request.get("email");

            if (username == null || username.trim().isEmpty() || email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "ユーザー名とメールアドレスを入力してください");
                return response;
            }

            Optional<User> userOpt = userService.findByUsername(username.trim());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String userEmail = user.getEmail();
                if (userEmail != null && userEmail.equals(email.trim())) {
                    response.put("success", true);
                    response.put("userId", user.getId());
                    return response;
                }
            }

            response.put("success", false);
            response.put("message", "ユーザー名またはメールアドレスが一致しません");
        } catch (Exception e) {
            System.err.println("ユーザー確認エラー: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "エラーが発生しました。もう一度お試しください。");
        }
        return response;
    }
    
    @PostMapping("/api/auth/reset-password")
    @ResponseBody
    public Map<String, Object> resetPassword(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Object userIdObj = request.get("userId");
            Object newPasswordObj = request.get("newPassword");

            if (userIdObj == null || newPasswordObj == null) {
                response.put("success", false);
                response.put("message", "必要な情報が不足しています");
                return response;
            }

            Long userId = Long.valueOf(userIdObj.toString());
            String newPassword = newPasswordObj.toString();

            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userService.updatePassword(user, newPassword);
                response.put("success", true);
                response.put("message", "パスワードが正常に更新されました");
            } else {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
            }
        } catch (Exception e) {
            System.err.println("パスワードリセットエラー: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "パスワードの更新中にエラーが発生しました");
        }

        return response;
    }
}
