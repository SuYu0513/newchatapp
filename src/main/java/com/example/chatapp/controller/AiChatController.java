package com.example.chatapp.controller;

import com.example.chatapp.entity.AiPersona;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.AiPersonaRepository;
import com.example.chatapp.service.GeminiService;
import com.example.chatapp.service.LeonardoService;
import com.example.chatapp.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final GeminiService geminiService;
    private final LeonardoService leonardoService;
    private final AiPersonaRepository aiPersonaRepository;
    private final UserService userService;

    public AiChatController(GeminiService geminiService, LeonardoService leonardoService, AiPersonaRepository aiPersonaRepository, UserService userService) {
        this.geminiService = geminiService;
        this.leonardoService = leonardoService;
        this.aiPersonaRepository = aiPersonaRepository;
        this.userService = userService;
    }

    private User getUserFromPrincipal(Principal principal) {
        if (principal == null) return null;
        return userService.findByUsername(principal.getName()).orElse(null);
    }

    /**
     * AIにメッセージを送信して返答を取得
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request, Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "メッセージを入力してください"
            ));
        }

        String reply = geminiService.chat(user, message.trim());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "reply", reply
        ));
    }

    /**
     * 会話履歴を取得
     */
    @GetMapping("/chat/history")
    public ResponseEntity<?> getHistory(Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        List<Map<String, String>> history = geminiService.getHistory(user.getId());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "history", history
        ));
    }

    /**
     * 会話履歴をクリア
     */
    @DeleteMapping("/chat/history")
    public ResponseEntity<?> clearHistory(Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        geminiService.clearHistory(user.getId());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "会話履歴をクリアしました"
        ));
    }

    /**
     * 初回挨拶メッセージを取得（履歴に保存しない）
     */
    @PostMapping("/chat/greeting")
    public ResponseEntity<?> getGreeting(Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        // 履歴をクリアして新しい会話を開始
        geminiService.clearHistory(user.getId());
        
        // 挨拶メッセージを生成（履歴には保存されない）
        String greeting = geminiService.generateGreeting(user);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "reply", greeting
        ));
    }

    /**
     * APIが設定されているかチェック
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "configured", geminiService.isConfigured()
        ));
    }

    /**
     * AIペルソナ設定を取得
     */
    @GetMapping("/persona")
    public ResponseEntity<?> getPersona(Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        AiPersona persona = aiPersonaRepository.findByUser(user).orElse(null);

        if (persona == null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "setupCompleted", false
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("setupCompleted", persona.isSetupCompleted());
        response.put("aiName", persona.getAiName());
        response.put("relationship", persona.getRelationship());
        response.put("gender", persona.getGender());
        response.put("ageGroup", persona.getAgeGroup());
        response.put("personality", persona.getPersonality());
        response.put("speakingStyle", persona.getSpeakingStyle());
        response.put("avatarUrl", persona.getAvatarUrl());

        return ResponseEntity.ok(response);
    }

    /**
     * AIペルソナ設定を保存
     */
    @PostMapping("/persona")
    public ResponseEntity<?> savePersona(@RequestBody Map<String, String> request, Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        AiPersona persona = aiPersonaRepository.findByUser(user).orElse(new AiPersona());
        persona.setUser(user);
        persona.setAiName(request.get("aiName"));
        persona.setRelationship(request.get("relationship"));
        persona.setGender(request.get("gender"));
        persona.setAgeGroup(request.get("ageGroup"));
        persona.setPersonality(request.get("personality"));
        persona.setSpeakingStyle(request.get("speakingStyle"));
        persona.setAvatarUrl(request.get("avatarUrl"));
        persona.setSetupCompleted(true);

        aiPersonaRepository.save(persona);

        // 会話履歴をクリアして新しいペルソナで開始
        geminiService.clearHistory(user.getId());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "AIの設定を保存しました"
        ));
    }

    /**
     * AIペルソナ設定をリセット
     */
    @DeleteMapping("/persona")
    public ResponseEntity<?> resetPersona(Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        aiPersonaRepository.findByUser(user).ifPresent(aiPersonaRepository::delete);
        geminiService.clearHistory(user.getId());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "AI設定をリセットしました"
        ));
    }

    /**
     * Leonardo AIでアバター画像を生成
     */
    @PostMapping("/avatar/generate")
    public ResponseEntity<?> generateAvatar(@RequestBody Map<String, String> request, Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        if (!leonardoService.isConfigured()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "Leonardo APIが設定されていません"
            ));
        }

        String gender = request.get("gender");
        String ageGroup = request.get("ageGroup");
        String personality = request.get("personality");

        // 複数のアバターを生成
        List<String> avatarUrls = leonardoService.generateAvatars(gender, ageGroup, personality);

        if (avatarUrls.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "アバターの生成に失敗しました"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "avatars", avatarUrls
        ));
    }

    /**
     * Leonardo APIの設定状態を確認
     */
    @GetMapping("/avatar/status")
    public ResponseEntity<?> getAvatarStatus(Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "ログインが必要です"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "configured", leonardoService.isConfigured()
        ));
    }
}
