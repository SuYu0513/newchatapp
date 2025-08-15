package com.example.chatapp.service;

import com.example.chatapp.entity.Message;
import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.dto.MessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Value("${app.debug.enabled:false}")
    private boolean debugEnabled;

    /**
     * メッセージを保存
     */
    public Message saveMessage(String content, String username, Long chatRoomId) {
        if (debugEnabled) {
            System.out.println("=== メッセージ保存開始 ===");
            System.out.println("内容: " + content);
            System.out.println("ユーザー: " + username);
            System.out.println("チャットルームID: " + chatRoomId);
        }
        
        // ユーザーを取得
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("ユーザーが見つかりません: " + username);
        }
        if (debugEnabled) {
            System.out.println("ユーザー取得成功: " + userOpt.get().getUsername());
        }

        // チャットルームを取得（存在しない場合は最初のルームを使用）
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseGet(() -> {
                    if (debugEnabled) {
                        System.out.println("指定されたチャットルームが見つからないため、代替ルームを検索中...");
                    }
                    List<ChatRoom> rooms = chatRoomRepository.findAll();
                    if (!rooms.isEmpty()) {
                        if (debugEnabled) {
                            System.out.println("代替ルームを使用: " + rooms.get(0).getName());
                        }
                        return rooms.get(0); // 最初のルームを使用
                    } else {
                        if (debugEnabled) {
                            System.out.println("ルームが存在しないため、新しいルームを作成中...");
                        }
                        return createDefaultChatRoom(); // なければ作成
                    }
                });
        if (debugEnabled) {
            System.out.println("チャットルーム取得成功: " + chatRoom.getName() + " (ID: " + chatRoom.getId() + ")");
        }

        // メッセージエンティティを作成
        Message message = new Message();
        message.setContent(content);
        message.setUser(userOpt.get());
        message.setChatRoom(chatRoom);
        message.setSentAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        if (debugEnabled) {
            System.out.println("メッセージ保存成功: ID=" + savedMessage.getId());
            System.out.println("=== メッセージ保存完了 ===");
        }
        
        return savedMessage;
    }

    /**
     * チャットルームの履歴を取得（最新順）
     */
    public List<Message> getChatHistory(Long chatRoomId, int limit) {
        if (debugEnabled) {
            System.out.println("=== チャット履歴取得開始 ===");
            System.out.println("チャットルームID: " + chatRoomId);
            System.out.println("取得件数: " + limit);
        }
        
        try {
            // まず、シンプルなクエリでテスト
            List<Message> allMessages = messageRepository.findAll();
            if (debugEnabled) {
                System.out.println("全メッセージ数: " + allMessages.size());
            }
            
            // 指定されたチャットルームのメッセージのみフィルタ
            List<Message> roomMessages = allMessages.stream()
                .filter(msg -> msg.getChatRoom().getId().equals(chatRoomId))
                .sorted((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()))
                .toList();
            
            if (debugEnabled) {
                System.out.println("チャットルーム" + chatRoomId + "のメッセージ数: " + roomMessages.size());
                for (Message msg : roomMessages) {
                    System.out.println("- " + msg.getUser().getUsername() + ": " + msg.getContent() + " (時刻: " + msg.getSentAt() + ")");
                }
            }
            
            // 最新のlimit件のみ返す
            List<Message> limitedMessages = roomMessages.stream()
                .skip(Math.max(0, roomMessages.size() - limit))
                .toList();
            
            if (debugEnabled) {
                System.out.println("返却するメッセージ数: " + limitedMessages.size());
                System.out.println("=== チャット履歴取得完了 ===");
            }
            return limitedMessages;
        } catch (Exception e) {
            System.err.println("チャット履歴取得エラー: " + e.getMessage());
            if (debugEnabled) {
                e.printStackTrace();
            }
            return List.of();
        }
    }

    /**
     * 全てのメッセージ履歴を取得
     */
    public List<Message> getAllChatHistory(Long chatRoomId) {
        return messageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId);
    }

    /**
     * デフォルトチャットルームを作成
     */
    private ChatRoom createDefaultChatRoom() {
        ChatRoom defaultRoom = new ChatRoom();
        defaultRoom.setName("メインチャット");
        defaultRoom.setType(ChatRoom.ChatRoomType.GROUP);
        
        // システムユーザーとして保存（後でadminユーザーを作成予定）
        Optional<User> adminUser = userRepository.findByUsername("admin");
        if (adminUser.isEmpty()) {
            // 管理者ユーザーが存在しない場合は最初のユーザーを取得
            List<User> users = userRepository.findAll();
            if (!users.isEmpty()) {
                defaultRoom.setCreatedBy(users.get(0));
            } else {
                // 暫定的にダミーユーザーを作成
                User dummyUser = new User();
                dummyUser.setUsername("system");
                dummyUser.setEmail("system@chatapp.com");
                dummyUser.setPassword("dummy");
                User savedUser = userRepository.save(dummyUser);
                defaultRoom.setCreatedBy(savedUser);
            }
        } else {
            defaultRoom.setCreatedBy(adminUser.get());
        }
        
        defaultRoom.setCreatedAt(LocalDateTime.now());
        return chatRoomRepository.save(defaultRoom);
    }

    /**
     * 指定されたチャットルームのメッセージを取得
     */
    public List<Message> getMessagesByChatRoom(Long chatRoomId) {
        if (debugEnabled) {
            System.out.println("=== チャットルームのメッセージ取得 ===");
            System.out.println("チャットルームID: " + chatRoomId);
        }
        
        try {
            List<Message> messages = messageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId);
            if (debugEnabled) {
                System.out.println("取得したメッセージ数: " + messages.size());
            }
            return messages;
        } catch (Exception e) {
            if (debugEnabled) {
                System.err.println("メッセージ取得エラー: " + e.getMessage());
                e.printStackTrace();
            }
            throw new RuntimeException("メッセージの取得に失敗しました", e);
        }
    }

    /**
     * MessageエンティティをDTOに変換
     */
    public MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setContent(message.getContent());
        dto.setSenderUsername(message.getUser().getUsername());
        dto.setUserId(message.getUser().getId());
        dto.setTimestamp(message.getSentAt().toString());
        dto.setChatRoomId(message.getChatRoom().getId());
        
        // ユーザープロフィール情報を取得してアバターと表示名を設定
        try {
            com.example.chatapp.entity.UserProfile profile = userProfileService.getOrCreateProfile(message.getUser());
            dto.setSenderAvatarUrl(profile.getAvatarUrlOrDefault());
            dto.setSenderDisplayName(profile.getDisplayNameOrUsername());
        } catch (Exception e) {
            // プロフィール取得エラーの場合はデフォルト値を設定
            dto.setSenderAvatarUrl("/images/default-avatar.svg");
            dto.setSenderDisplayName(message.getUser().getUsername());
        }
        
        return dto;
    }
}
