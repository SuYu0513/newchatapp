package com.example.chatapp.controller;

import com.example.chatapp.dto.MessageDto;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class ChatController {

    @GetMapping("/chat")
    public String chatPage(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
        } else {
            model.addAttribute("username", "ゲスト");
        }
        return "chat";
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/chatroom/1")
    public MessageDto sendMessage(MessageDto message, Authentication authentication) {
        // 送信者の情報を設定
        if (authentication != null) {
            message.setSenderUsername(authentication.getName());
        } else {
            message.setSenderUsername("匿名ユーザー");
        }
        message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return message;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/chatroom/1")
    public MessageDto addUser(MessageDto message, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "匿名ユーザー";
        message.setSenderUsername(username);
        message.setContent(username + "がチャットに参加しました");
        message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return message;
    }
}
