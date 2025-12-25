package com.example.chatapp.controller;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.UserStatisticsService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/rooms")
public class ChatRoomController {

    @Autowired
    private ChatRoomService chatRoomService;

    /**
     * チャットルーム一覧ページ
     */
    @GetMapping
    public String roomList(Model model, Principal principal, 
                          @RequestParam(value = "search", required = false) String searchKeyword) {
        if (principal != null) {
            // ユーザーが参加しているルーム
            List<ChatRoom> userRooms = chatRoomService.getUserChatRooms(principal.getName());
            
            // 参加可能なパブリックルーム（検索キーワードがあれば検索結果、なければ全て）
            List<ChatRoom> availableRooms;
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                availableRooms = chatRoomService.searchAvailablePublicRooms(principal.getName(), searchKeyword);
                model.addAttribute("searchKeyword", searchKeyword);
                model.addAttribute("searchResult", true);
            } else {
                availableRooms = chatRoomService.getAvailablePublicRooms(principal.getName());
                model.addAttribute("searchResult", false);
            }
            
            model.addAttribute("userRooms", userRooms);
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("username", principal.getName());
        }
        return "room-list";
    }

    /**
     * チャットルーム作成ページ
     */
    @GetMapping("/create")
    public String createRoomPage(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
        }
        return "create-room";
    }

    /**
     * チャットルーム作成処理
     */
    @PostMapping("/create")
    public String createRoom(@RequestParam String name,
                           @RequestParam(defaultValue = "GROUP") String type,
                           Principal principal) {
        if (principal != null) {
            ChatRoom.ChatRoomType roomType = ChatRoom.ChatRoomType.valueOf(type.toUpperCase());
            ChatRoom newRoom = chatRoomService.createChatRoom(name, principal.getName(), roomType);
            return "redirect:/chat?room=" + newRoom.getId();
        }
        return "redirect:/login";
    }

    /**
     * チャットルーム参加
     */
    @PostMapping("/{roomId}/join")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> joinRoom(@PathVariable Long roomId, 
                                                       Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication != null) {
            try {
                chatRoomService.joinChatRoom(roomId, authentication.getName());
                response.put("success", true);
                response.put("message", "チャットルームに参加しました");
                response.put("redirectUrl", "/chat?room=" + roomId);
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "参加に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * チャットルーム退出
     */
    @PostMapping("/{roomId}/leave")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> leaveRoom(@PathVariable Long roomId, 
                                                        Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication != null) {
            try {
                chatRoomService.leaveChatRoom(roomId, authentication.getName());
                response.put("success", true);
                response.put("message", "チャットルームから退出しました");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "退出に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * チャットルーム削除
     */
    @DeleteMapping("/{roomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteRoom(@PathVariable Long roomId, 
                                                         Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication != null) {
            boolean deleted = chatRoomService.deleteChatRoom(roomId, authentication.getName());
            if (deleted) {
                response.put("success", true);
                response.put("message", "チャットルームを削除しました");
            } else {
                response.put("success", false);
                response.put("message", "削除権限がありません");
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * API: チャットルーム一覧取得
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<ChatRoom>> getRoomList(Authentication authentication) {
        if (authentication != null) {
            List<ChatRoom> rooms = chatRoomService.getUserChatRooms(authentication.getName());
            return ResponseEntity.ok(rooms);
        }
        return ResponseEntity.ok(List.of());
    }
}
