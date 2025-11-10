package com.example.chatapp.controller;

import com.example.chatapp.dto.MessageDto;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.OnlineUserService;
import com.example.chatapp.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private OnlineUserService onlineUserService;

    @Value("${app.debug.enabled:false}")
    private boolean debugEnabled;

    @GetMapping("/chat")
    public String chat(Model model, Principal principal, HttpServletRequest request, HttpSession session,
                      @RequestParam(value = "room", required = false) Long room) {
        if (debugEnabled) {
            System.out.println("=== チャットページアクセス ===");
            System.out.println("ユーザー: " + (principal != null ? principal.getName() : "null"));
            System.out.println("要求されたルーム: " + room);
        }
        
        // セッションからデバッグモードを取得
        Boolean debugMode = (Boolean) session.getAttribute("debugMode");
        boolean isDebugMode = debugMode != null && debugMode;
        
        if (principal != null) {
            String username = principal.getName();
            model.addAttribute("username", username);
            
            // ユーザーをオンライン状態にする（念のため）
            try {
                String sessionId = request.getSession().getId();
                onlineUserService.setUserOnline(username, sessionId);
                if (debugEnabled) {
                    System.out.println("ユーザーをオンライン状態に設定: " + username);
                }
            } catch (Exception e) {
                if (debugEnabled) {
                    System.err.println("オンライン状態設定エラー: " + e.getMessage());
                }
            }
            
            // ユーザーをメインルームに自動参加させる
            try {
                chatRoomService.ensureUserInMainRoom(username);
                if (debugEnabled) {
                    System.out.println("メインルーム自動参加を確認: " + username);
                }
            } catch (Exception e) {
                if (debugEnabled) {
                    System.err.println("メインルーム自動参加エラー: " + e.getMessage());
                }
            }
            
            // チャットルームIDを決定
            Long chatRoomId = room != null ? room : getDefaultChatRoomId();
            
            // ユーザーが参加中のルーム一覧を取得
            List<com.example.chatapp.entity.ChatRoom> userRooms = chatRoomService.getUserChatRooms(username);
            
            // 現在のチャットルーム情報を取得
            com.example.chatapp.entity.ChatRoom currentRoom = null;
            try {
                currentRoom = chatRoomService.getChatRoom(chatRoomId).orElse(null);
            } catch (Exception e) {
                if (debugEnabled) {
                    System.err.println("現在のルーム取得エラー: " + e.getMessage());
                }
            }
            
            if (debugEnabled) {
                System.out.println("使用するチャットルームID: " + chatRoomId);
                System.out.println("現在のルーム: " + (currentRoom != null ? currentRoom.getName() : "null"));
                System.out.println("参加中のルーム数: " + userRooms.size());
            }

            model.addAttribute("chatHistory", List.of()); // 空のリストを設定
            model.addAttribute("chatRoomId", chatRoomId);
            model.addAttribute("currentRoom", currentRoom);
            model.addAttribute("userRooms", userRooms);
            model.addAttribute("debugEnabled", debugEnabled);
            model.addAttribute("debugMode", isDebugMode);
        } else {
            if (debugEnabled) {
                System.out.println("未ログインユーザー");
            }
            model.addAttribute("username", "ゲスト");
            model.addAttribute("chatHistory", List.of());
            model.addAttribute("chatRoomId", 1L);
            model.addAttribute("currentRoom", null);
            model.addAttribute("userRooms", List.of());
            model.addAttribute("debugEnabled", debugEnabled);
            model.addAttribute("debugMode", isDebugMode);
        }
        
        return "chat";
    }

    @GetMapping("/test-messages")
    public String testMessages() {
        return "test-messages";
    }

    @GetMapping("/api/messages/{chatRoomId}")
    @ResponseBody
    public List<MessageDto> getRoomMessages(@PathVariable Long chatRoomId) {
        System.out.println("🚀🚀🚀 [API] メッセージ取得開始: /api/messages/" + chatRoomId);
        System.out.println("📥 リクエスト受信 - ルームID: " + chatRoomId + " (型: " + chatRoomId.getClass().getSimpleName() + ")");
        
        if (debugEnabled) {
            System.out.println("=== API: メッセージ履歴取得 ===");
            System.out.println("要求されたチャットルームID: " + chatRoomId);
        }
        
        try {
            System.out.println("💾 MessageServiceを呼び出し中...");
            List<Message> messages = messageService.getMessagesByChatRoom(chatRoomId);
            System.out.println("🔍 データベースから取得したメッセージ数: " + messages.size());
            
            System.out.println("🔄 DTOに変換中...");
            List<MessageDto> dtoList = messages.stream()
                    .map(messageService::convertToDto)
                    .collect(Collectors.toList());
            
            System.out.println("✅ APIで返却するメッセージ数: " + dtoList.size());
            
            if (debugEnabled && !dtoList.isEmpty()) {
                System.out.println("📝 取得したメッセージ:");
                dtoList.forEach(dto -> System.out.println("  - " + dto.getSenderUsername() + ": " + dto.getContent()));
            }
            
            System.out.println("🎯 [API] 応答完了: " + dtoList.size() + "件のメッセージを返却");
            return dtoList;
        } catch (Exception e) {
            System.err.println("❌ [API] メッセージ履歴取得エラー: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
        }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(MessageDto message, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "匿名ユーザー";
        
        try {
            Long chatRoomId = message.getChatRoomId() != null ? message.getChatRoomId() : getDefaultChatRoomId();
            
            if (debugEnabled) {
                System.out.println("=== メッセージ送信処理開始 ===");
                System.out.println("ユーザー: " + username);
                System.out.println("チャットルームID: " + chatRoomId);
                System.out.println("メッセージ内容: " + message.getContent());
            }
            
            // メッセージを保存
            Message savedMessage = messageService.saveMessage(
                message.getContent(), 
                username, 
                chatRoomId
            );
            
            if (debugEnabled) {
                System.out.println("メッセージ保存成功: ID=" + savedMessage.getId());
            }
            
            // DTOに変換してWebSocketで送信
            MessageDto messageDto = messageService.convertToDto(savedMessage);
            messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoomId, messageDto);
            
            if (debugEnabled) {
                System.out.println("WebSocket送信完了: " + messageDto.getContent() + " to room: " + chatRoomId);
                System.out.println("=== メッセージ送信処理完了 ===");
            }
            
        } catch (Exception e) {
            System.err.println("メッセージ送信エラー: " + e.getMessage());
            if (debugEnabled) {
                e.printStackTrace();
            }
            
            // エラー時のフォールバック処理
            message.setSenderUsername(username);
            message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messagingTemplate.convertAndSend("/topic/chatroom/" + message.getChatRoomId(), message);
        }
    }

    @MessageMapping("/chat.addUser")
    public void addUser(MessageDto message, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "匿名ユーザー";
        
        MessageDto joinMessage = new MessageDto();
        joinMessage.setSenderUsername("システム");
        joinMessage.setContent(username + "がチャットに参加しました");
        joinMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        joinMessage.setType("JOIN");
        joinMessage.setChatRoomId(message.getChatRoomId());
        
        messagingTemplate.convertAndSend("/topic/chatroom/" + message.getChatRoomId(), joinMessage);
        
        if (debugEnabled) {
            System.out.println("参加メッセージを送信しました: " + username + " to room: " + message.getChatRoomId());
        }
    }

    private Long getDefaultChatRoomId() {
        // メインルームのIDは固定で1
        return 1L;
    }
}
