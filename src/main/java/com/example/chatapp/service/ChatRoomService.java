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
import java.util.stream.Collectors;

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
     * ユーザーが参加していないパブリックルームを取得
     */
    public List<ChatRoom> getAvailablePublicRooms(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            return List.of();
        }
        
        User user = userOpt.get();
        List<ChatRoom> allRooms = chatRoomRepository.findAll();
        List<ChatRoom> userRooms = chatRoomRepository.findByUsersContaining(user);
        
        // パブリック（GROUP）ルームかつユーザーが参加していないもの
        return allRooms.stream()
                .filter(room -> room.getType() == ChatRoom.ChatRoomType.GROUP)
                .filter(room -> !userRooms.contains(room))
                .collect(Collectors.toList());
    }

    /**
     * ルーム名で検索（パブリックルームのみ、未参加のもの）
     */
    public List<ChatRoom> searchAvailablePublicRooms(String username, String keyword) {
        List<ChatRoom> availableRooms = getAvailablePublicRooms(username);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return availableRooms;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return availableRooms.stream()
                .filter(room -> room.getName().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }

    /**
     * メインルームを取得（なければ作成）
     * メインルームのIDは1を目指すが、名前でも検索する
     */
    public ChatRoom getOrCreateMainRoom() {
        // まずID=1のルームを探す
        Optional<ChatRoom> mainRoomById = chatRoomRepository.findById(1L);
        if (mainRoomById.isPresent()) {
            return mainRoomById.get();
        }
        
        // ID=1がない場合は名前で検索
        List<ChatRoom> allRooms = chatRoomRepository.findAll();
        Optional<ChatRoom> mainRoomByName = allRooms.stream()
                .filter(room -> "メインルーム".equals(room.getName()))
                .findFirst();
        
        if (mainRoomByName.isPresent()) {
            return mainRoomByName.get();
        }
        
        // メインルームが存在しない場合は作成
        ChatRoom newMainRoom = new ChatRoom();
        newMainRoom.setName("メインルーム");
        newMainRoom.setType(ChatRoom.ChatRoomType.GROUP);
        // システム管理者として作成（最初のユーザーまたは固定の管理者）
        Optional<User> firstUser = userRepository.findAll().stream().findFirst();
        if (firstUser.isPresent()) {
            newMainRoom.setCreatedBy(firstUser.get());
        }
        
        return chatRoomRepository.save(newMainRoom);
    }

    /**
     * ユーザーをメインルームに自動参加させる
     */
    public void ensureUserInMainRoom(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            return;
        }
        
        User user = userOpt.get();
        ChatRoom mainRoom = getOrCreateMainRoom();
        
        // ユーザーがメインルームに参加していない場合は参加させる
        if (!mainRoom.getUsers().contains(user)) {
            mainRoom.getUsers().add(user);
            chatRoomRepository.save(mainRoom);
            
            if (debugEnabled) {
                System.out.println("ユーザー " + username + " をメインルームに自動参加させました");
            }
        }
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
        chatRoom.setCreatedBy(creator);
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

    /**
     * ユーザーが参加しているルーム一覧を取得
     */
    public List<ChatRoom> getRoomsByUser(User user) {
        return chatRoomRepository.findByUsersContaining(user);
    }

    /**
     * 2人のユーザー間の既存プライベートルームを検索
     */
    public Optional<ChatRoom> findExistingPrivateRoom(User user1, User user2) {
        List<ChatRoom> user1Rooms = chatRoomRepository.findByUsersContaining(user1);
        
        return user1Rooms.stream()
                .filter(room -> room.getType() == ChatRoom.ChatRoomType.PRIVATE || 
                               room.getType() == ChatRoom.ChatRoomType.RANDOM)
                .filter(room -> room.getUsers().contains(user2))
                .filter(room -> room.getUsers().size() == 2) // 2人だけのルーム
                .findFirst();
    }
}
