package com.example.chatapp.service;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.debug.enabled:false}")
    private boolean debugEnabled;

    /**
     * 全てのチャットルームを取得
     */
    public List<ChatRoom> getAllChatRooms() {
        return chatRoomRepository.findAll();
    }

    /**
     * ユーザーが参加しているチャットルームを取得
     */
    public List<ChatRoom> getUserChatRooms(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return chatRoomRepository.findByUsersContaining(user.get());
        }
        return List.of();
    }

    /**
     * チャットルームを作成
     */
    public ChatRoom createChatRoom(String name, String createdByUsername, ChatRoom.ChatRoomType type) {
        if (debugEnabled) {
            System.out.println("チャットルーム作成: " + name + " by " + createdByUsername);
        }

        Optional<User> creator = userRepository.findByUsername(createdByUsername);
        if (creator.isEmpty()) {
            throw new RuntimeException("ユーザーが見つかりません: " + createdByUsername);
        }

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(name);
        chatRoom.setType(type);
        chatRoom.setCreatedBy(creator.get());
        chatRoom.setCreatedAt(LocalDateTime.now());

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        // 作成者をルームに追加
        joinChatRoom(savedRoom.getId(), createdByUsername);
        
        return savedRoom;
    }

    /**
     * チャットルームに参加
     */
    public void joinChatRoom(Long chatRoomId, String username) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatRoomId);
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (roomOpt.isPresent() && userOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            User user = userOpt.get();
            
            if (!room.getUsers().contains(user)) {
                room.addUser(user);
                chatRoomRepository.save(room);
                
                if (debugEnabled) {
                    System.out.println(username + " がルーム " + room.getName() + " に参加しました");
                }
            }
        }
    }

    /**
     * チャットルームから退出
     */
    public void leaveChatRoom(Long chatRoomId, String username) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatRoomId);
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (roomOpt.isPresent() && userOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            User user = userOpt.get();
            
            room.removeUser(user);
            chatRoomRepository.save(room);
            
            if (debugEnabled) {
                System.out.println(username + " がルーム " + room.getName() + " から退出しました");
            }
        }
    }

    /**
     * チャットルームを取得
     */
    public Optional<ChatRoom> getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId);
    }

    /**
     * チャットルームを削除（作成者のみ）
     */
    public boolean deleteChatRoom(Long chatRoomId, String username) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatRoomId);
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (roomOpt.isPresent() && userOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            User user = userOpt.get();
            
            // 作成者または管理者のみ削除可能
            if (room.getCreatedBy().equals(user) || "admin".equals(username)) {
                chatRoomRepository.delete(room);
                
                if (debugEnabled) {
                    System.out.println("ルーム " + room.getName() + " が削除されました");
                }
                return true;
            }
        }
        return false;
    }

    /**
     * デフォルトチャットルームのIDを取得
     */
    public Long getDefaultChatRoomId() {
        return chatRoomRepository.findAll().stream()
                .findFirst()
                .map(ChatRoom::getId)
                .orElse(1L);
    }

    /**
     * プライベートチャットルームを作成
     */
    public ChatRoom createPrivateRoom(String name, User creator) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(name);
        chatRoom.setType(ChatRoom.ChatRoomType.PRIVATE);
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom = chatRoomRepository.save(chatRoom);
        
        // 作成者をルームに追加
        chatRoom.getUsers().add(creator);
        return chatRoomRepository.save(chatRoom);
    }

    /**
     * ユーザーをルームに追加
     */
    public boolean addUserToRoom(Long roomId, User user) {
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(roomId);
        if (chatRoomOpt.isPresent()) {
            ChatRoom chatRoom = chatRoomOpt.get();
            if (!chatRoom.getUsers().contains(user)) {
                chatRoom.getUsers().add(user);
                chatRoomRepository.save(chatRoom);
                return true;
            }
        }
        return false;
    }

    /**
     * ユーザーをルームから削除
     */
    public boolean removeUserFromRoom(Long roomId, User user) {
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(roomId);
        if (chatRoomOpt.isPresent()) {
            ChatRoom chatRoom = chatRoomOpt.get();
            if (chatRoom.getUsers().contains(user)) {
                chatRoom.getUsers().remove(user);
                chatRoomRepository.save(chatRoom);
                return true;
            }
        }
        return false;
    }

    /**
     * ルームを非アクティブにする
     */
    public void deactivateRoom(Long roomId) {
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(roomId);
        if (chatRoomOpt.isPresent()) {
            ChatRoom chatRoom = chatRoomOpt.get();
            // ここでは論理削除や非アクティブフラグの設定を行う
            // 今回は簡単のため、ユーザーリストをクリア
            chatRoom.getUsers().clear();
            chatRoomRepository.save(chatRoom);
        }
    }
}
