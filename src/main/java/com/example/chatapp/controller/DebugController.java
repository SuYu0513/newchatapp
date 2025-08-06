package com.example.chatapp.controller;

import com.example.chatapp.entity.Message;
import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DebugController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @GetMapping("/debug/data")
    public Map<String, Object> debugData() {
        Map<String, Object> debug = new HashMap<>();
        
        // データベースの内容を確認
        List<User> users = userRepository.findAll();
        List<ChatRoom> chatRooms = chatRoomRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        
        debug.put("ユーザー数", users.size());
        debug.put("チャットルーム数", chatRooms.size());
        debug.put("メッセージ数", messages.size());
        
        debug.put("ユーザー一覧", users.stream().map(u -> u.getUsername()).toList());
        debug.put("チャットルーム一覧", chatRooms.stream().map(r -> r.getName() + " (ID:" + r.getId() + ")").toList());
        debug.put("メッセージ一覧", messages.stream().map(m -> 
            m.getUser().getUsername() + ": " + m.getContent() + " (ルーム:" + m.getChatRoom().getId() + ")"
        ).toList());
        
        return debug;
    }

    @GetMapping("/debug/history")
    public Map<String, Object> debugHistory() {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            // MessageServiceを通して履歴を取得
            List<Message> history = messageService.getChatHistory(1L, 100);
            debug.put("MessageService経由の履歴件数", history.size());
            debug.put("MessageService経由の履歴", history.stream().map(m -> 
                m.getUser().getUsername() + ": " + m.getContent()
            ).toList());
            
            // 直接Repositoryから取得
            List<Message> directHistory = messageRepository.findByChatRoomIdOrderBySentAtAsc(1L);
            debug.put("Repository直接の履歴件数", directHistory.size());
            debug.put("Repository直接の履歴", directHistory.stream().map(m -> 
                m.getUser().getUsername() + ": " + m.getContent()
            ).toList());
            
        } catch (Exception e) {
            debug.put("エラー", e.getMessage());
        }
        
        return debug;
    }
}
