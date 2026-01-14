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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
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
     * メッセージを保存（トランザクション管理強化）
     */
    @Transactional
    public Message saveMessage(String content, String username, Long chatRoomId) {
        if (debugEnabled) {
            System.out.println("=== メッセージ保存開始 ===");
            System.out.println("内容: " + content);
            System.out.println("ユーザー: " + username);
            System.out.println("チャットルームID: " + chatRoomId);
        }
        
        try {
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

            // メッセージを保存
            Message savedMessage = messageRepository.save(message);
            
            // 保存後に強制的にフラッシュして確実にDBに反映
            messageRepository.flush();
            
            if (debugEnabled) {
                System.out.println("メッセージ保存成功: ID=" + savedMessage.getId());
                System.out.println("保存されたメッセージ: " + savedMessage.getContent());
                System.out.println("=== メッセージ保存完了 ===");
            }
            
            return savedMessage;
            
        } catch (Exception e) {
            System.err.println("メッセージ保存エラー: " + e.getMessage());
            if (debugEnabled) {
                e.printStackTrace();
            }
            throw new RuntimeException("メッセージの保存に失敗しました", e);
        }
    }

    /**
     * チャットルームの履歴を取得（最新順）
     */
    @Transactional(readOnly = true)
    public List<Message> getChatHistory(Long chatRoomId, int limit) {
        if (debugEnabled) {
            System.out.println("=== チャット履歴取得開始 ===");
            System.out.println("チャットルームID: " + chatRoomId);
            System.out.println("取得件数: " + limit);
        }
        
        try {
            // 直接リポジトリからデータを取得
            List<Message> messages = messageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId);
            
            if (debugEnabled) {
                System.out.println("チャットルーム " + chatRoomId + " のメッセージ数: " + messages.size());
                for (Message msg : messages) {
                    System.out.println("- " + msg.getUser().getUsername() + ": " + msg.getContent() + " (時刻: " + msg.getSentAt() + ")");
                }
            }

            // 最新のlimit件のみ返す
            List<Message> limitedMessages = messages.stream()
                .skip(Math.max(0, messages.size() - limit))
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
        defaultRoom.setName("メインルーム");
        defaultRoom.setType(ChatRoom.ChatRoomType.GROUP);
        
        // systemユーザーを取得または作成
        User systemUser = userRepository.findByUsername("system")
                .orElseGet(() -> {
                    System.out.println("注意: システムユーザーが存在しないため、作成します");
                    User newSystemUser = new User();
                    newSystemUser.setUsername("system");
                    newSystemUser.setEmail("system@chatapp.com");
                    // パスワードはエンコードしない（ログインできないダミーユーザー）
                    newSystemUser.setPassword("system_dummy_password");
                    newSystemUser.setFriendCode(999999); // 固定フレンドコード
                    return userRepository.save(newSystemUser);
                });
        
        defaultRoom.setCreatedBy(systemUser);
        defaultRoom.setCreatedAt(LocalDateTime.now());
        
        return chatRoomRepository.save(defaultRoom);
    }

    /**
     * 指定されたチャットルームのメッセージを取得
     */
    public List<Message> getMessagesByChatRoom(Long chatRoomId) {
        System.out.println("🔍🔍🔍 [DB] 検索開始: チャットルームID=" + chatRoomId);
        System.out.println("📊 [DB] 検索クエリ: findByChatRoomIdOrderBySentAtAsc(" + chatRoomId + ")");
        System.out.println("💡 [DB] 実行SQLイメージ: SELECT * FROM message WHERE chat_room_id = " + chatRoomId + " ORDER BY sent_at ASC");
        
        if (debugEnabled) {
            System.out.println("=== チャットルームのメッセージ取得 ===");
            System.out.println("チャットルームID: " + chatRoomId);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            List<Message> messages = messageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("📊 [DB] 検索結果: " + messages.size() + "件のメッセージを取得 (実行時間: " + (endTime - startTime) + "ms)");
            
            if (messages.isEmpty()) {
                System.out.println("⚠️  [DB] メッセージが0件: ルーム" + chatRoomId + "にメッセージが存在しないか、ルームIDが間違っている可能性があります");
            } else {
                System.out.println("✅ [DB] 取得成功: 最古=" + messages.get(0).getSentAt() + 
                                 ", 最新=" + messages.get(messages.size()-1).getSentAt());
            }
            
            if (debugEnabled) {
                System.out.println("取得したメッセージ数: " + messages.size());
                if (!messages.isEmpty()) {
                    System.out.println("📝 取得したメッセージ一覧:");
                    for (int i = 0; i < Math.min(messages.size(), 5); i++) {  // 最大5件まで表示
                        Message msg = messages.get(i);
                        System.out.println("  [" + i + "] ID=" + msg.getId() + 
                                         ", 送信者=" + msg.getUser().getUsername() + 
                                         ", 内容=" + msg.getContent().substring(0, Math.min(msg.getContent().length(), 20)) + "..." + 
                                         ", 送信時刻=" + msg.getSentAt());
                    }
                    if (messages.size() > 5) {
                        System.out.println("  ... 他 " + (messages.size() - 5) + " 件");
                    }
                }
            }
            
            return messages;
        } catch (Exception e) {
            System.err.println("❌ [DB] 検索エラー: " + e.getMessage());
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
