package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    @Autowired
    private UserService userService;

    // ユーザー検索エンドポイント
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam("q") String query,
            Principal principal) {
        
        if (principal == null || query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String currentUsername = principal.getName();
            
            // ユーザー名またはIDで検索
            List<User> users = userService.searchUsers(query.trim());
            
            // 現在のユーザーを除外
            List<Map<String, Object>> result = users.stream()
                .filter(user -> !user.getUsername().equals(currentUsername))
                .limit(10) // 結果を10件に制限
                .map(user -> {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("id", user.getId());
                    userData.put("username", user.getUsername());
                    userData.put("friendCode", user.getFriendCode());
                    return userData;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
