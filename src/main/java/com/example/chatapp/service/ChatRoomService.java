package com.example.chatapp.service;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.chatapp.repository.RoomJoinRequestRepository roomJoinRequestRepository;

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
        
        // パブリック（isPublic=true）かつユーザーが参加していないもの
        return allRooms.stream()
                .filter(room -> room.isPublic())
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
        newMainRoom.setPublic(true); // メインルームは必ずパブリック
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
        return createChatRoom(name, null, createdByUsername, type, true); // デフォルトはパブリック
    }

    /**
     * チャットルームを作成（パブリック/プライベート指定可能）
     */
    public ChatRoom createChatRoom(String name, String createdByUsername, ChatRoom.ChatRoomType type, boolean isPublic) {
        return createChatRoom(name, null, createdByUsername, type, isPublic);
    }

    /**
     * チャットルームを作成（説明文付き）
     */
    public ChatRoom createChatRoom(String name, String description, String createdByUsername, ChatRoom.ChatRoomType type, boolean isPublic) {
        if (debugEnabled) {
            System.out.println("チャットルーム作成: " + name + " by " + createdByUsername + " (public: " + isPublic + ")");
        }

        Optional<User> creator = userRepository.findByUsername(createdByUsername);
        if (creator.isEmpty()) {
            throw new RuntimeException("ユーザーが見つかりません: " + createdByUsername);
        }

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(name);
        chatRoom.setDescription(description);
        chatRoom.setType(type);
        chatRoom.setPublic(isPublic);
        chatRoom.setCreatedBy(creator.get());
        chatRoom.setCreatedAt(LocalDateTime.now());

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        // 作成者をルームに追加
        joinChatRoom(savedRoom.getId(), createdByUsername);

        return savedRoom;
    }

    /**
     * チャットルーム参加申請
     */
    public void requestJoinRoom(Long chatRoomId, String username) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatRoomId);
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (roomOpt.isEmpty() || userOpt.isEmpty()) {
            throw new RuntimeException("ルームまたはユーザーが見つかりません");
        }

        ChatRoom room = roomOpt.get();
        User user = userOpt.get();

        // 既に参加している場合はエラー
        if (room.getUsers().contains(user)) {
            throw new RuntimeException("既にこのルームに参加しています");
        }

        // 既に申請済みかチェック（重複データ対応）
        List<com.example.chatapp.entity.RoomJoinRequest> existingRequests =
                roomJoinRequestRepository.findAllByChatRoomAndUser(room, user);

        for (com.example.chatapp.entity.RoomJoinRequest request : existingRequests) {
            if (request.getStatus() == com.example.chatapp.entity.RoomJoinRequest.RequestStatus.PENDING) {
                throw new RuntimeException("既に申請済みです");
            }
        }

        // 新規申請を作成
        com.example.chatapp.entity.RoomJoinRequest newRequest =
                new com.example.chatapp.entity.RoomJoinRequest(room, user);
        roomJoinRequestRepository.save(newRequest);

        if (debugEnabled) {
            System.out.println(username + " がルーム " + room.getName() + " への参加を申請しました");
        }
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
            if ((room.getCreatedBy() != null && room.getCreatedBy().equals(user)) || "admin".equals(username)) {
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

    /**
     * ユーザーが申請中のルーム一覧を取得
     */
    public List<ChatRoom> getRequestedRooms(String username) {
        System.out.println("getRequestedRooms called for: " + username);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            System.out.println("User not found: " + username);
            return List.of();
        }

        User user = userOpt.get();
        System.out.println("User found: " + user.getId() + " - " + user.getUsername());

        // ユーザーのPENDING状態の申請を取得
        List<com.example.chatapp.entity.RoomJoinRequest> requests =
            roomJoinRequestRepository.findByUserAndStatus(user,
                com.example.chatapp.entity.RoomJoinRequest.RequestStatus.PENDING);
        System.out.println("PENDING requests found: " + requests.size());

        // デバッグ: 全申請も確認
        List<com.example.chatapp.entity.RoomJoinRequest> allRequests =
            roomJoinRequestRepository.findByUser(user);
        System.out.println("All requests for user: " + allRequests.size());
        for (com.example.chatapp.entity.RoomJoinRequest req : allRequests) {
            System.out.println("  - Room: " + req.getChatRoom().getName() + ", Status: " + req.getStatus());
        }

        // 申請からルーム情報を抽出
        return requests.stream()
                .map(com.example.chatapp.entity.RoomJoinRequest::getChatRoom)
                .collect(Collectors.toList());
    }

    /**
     * ルーム参加申請をキャンセル
     */
    public void cancelJoinRequest(Long chatRoomId, String username) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatRoomId);
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (roomOpt.isEmpty() || userOpt.isEmpty()) {
            throw new RuntimeException("ルームまたはユーザーが見つかりません");
        }

        ChatRoom room = roomOpt.get();
        User user = userOpt.get();

        // 申請を検索（重複データ対応）
        List<com.example.chatapp.entity.RoomJoinRequest> requests =
                roomJoinRequestRepository.findAllByChatRoomAndUser(room, user);

        if (requests.isEmpty()) {
            throw new RuntimeException("申請が見つかりません");
        }

        // PENDING状態の申請を全て削除
        boolean foundPending = false;
        for (com.example.chatapp.entity.RoomJoinRequest request : requests) {
            if (request.getStatus() == com.example.chatapp.entity.RoomJoinRequest.RequestStatus.PENDING) {
                roomJoinRequestRepository.delete(request);
                foundPending = true;
            }
        }

        if (!foundPending) {
            throw new RuntimeException("キャンセルできる申請がありません");
        }

        if (debugEnabled) {
            System.out.println(username + " がルーム " + room.getName() + " への申請をキャンセルしました");
        }
    }

    /**
     * 自分が作成したルームへの承認待ち申請一覧を取得
     */
    public List<Map<String, Object>> getPendingApprovalsForCreator(String creatorUsername) {
        Optional<User> creatorOpt = userRepository.findByUsername(creatorUsername);
        if (!creatorOpt.isPresent()) {
            return List.of();
        }

        User creator = creatorOpt.get();

        // 自分が作成したルームを取得
        List<ChatRoom> createdRooms = chatRoomRepository.findAll().stream()
                .filter(room -> room.getCreatedBy() != null && room.getCreatedBy().equals(creator))
                .collect(Collectors.toList());

        // 各ルームへのPENDING状態の申請を取得
        List<Map<String, Object>> approvals = new ArrayList<>();
        for (ChatRoom room : createdRooms) {
            List<com.example.chatapp.entity.RoomJoinRequest> requests =
                    roomJoinRequestRepository.findByChatRoomAndStatus(room,
                            com.example.chatapp.entity.RoomJoinRequest.RequestStatus.PENDING);

            for (com.example.chatapp.entity.RoomJoinRequest request : requests) {
                Map<String, Object> approval = new HashMap<>();
                User applicant = request.getUser();

                approval.put("requestId", request.getId());
                approval.put("roomId", room.getId());
                approval.put("roomName", room.getName());
                approval.put("username", applicant.getUsername());
                approval.put("displayName", applicant.getProfile() != null ? applicant.getProfile().getDisplayName() : applicant.getUsername());
                approval.put("avatarUrl", applicant.getProfile() != null ? applicant.getProfile().getAvatarUrl() : null);
                approval.put("bio", applicant.getProfile() != null ? applicant.getProfile().getBio() : null);
                approval.put("requestedAt", request.getCreatedAt());

                approvals.add(approval);
            }
        }

        return approvals;
    }

    /**
     * ルーム参加申請を承認
     */
    public void approveJoinRequest(Long chatRoomId, String applicantUsername, String creatorUsername) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatRoomId);
        Optional<User> applicantOpt = userRepository.findByUsername(applicantUsername);
        Optional<User> creatorOpt = userRepository.findByUsername(creatorUsername);

        if (roomOpt.isEmpty() || applicantOpt.isEmpty() || creatorOpt.isEmpty()) {
            throw new RuntimeException("ルームまたはユーザーが見つかりません");
        }

        ChatRoom room = roomOpt.get();
        User applicant = applicantOpt.get();
        User creator = creatorOpt.get();

        // ルームの作成者かチェック
        if (room.getCreatedBy() == null || !room.getCreatedBy().equals(creator)) {
            throw new RuntimeException("このルームの作成者ではありません");
        }

        // 申請を検索（重複データ対応）
        List<com.example.chatapp.entity.RoomJoinRequest> requests =
                roomJoinRequestRepository.findAllByChatRoomAndUser(room, applicant);

        if (requests.isEmpty()) {
            throw new RuntimeException("申請が見つかりません");
        }

        // PENDING状態の申請を全て承認済みに
        boolean foundPending = false;
        for (com.example.chatapp.entity.RoomJoinRequest request : requests) {
            if (request.getStatus() == com.example.chatapp.entity.RoomJoinRequest.RequestStatus.PENDING) {
                request.setStatus(com.example.chatapp.entity.RoomJoinRequest.RequestStatus.APPROVED);
                roomJoinRequestRepository.save(request);
                foundPending = true;
            }
        }

        if (!foundPending) {
            throw new RuntimeException("承認できる申請がありません");
        }

        // ユーザーをルームに追加
        joinChatRoom(chatRoomId, applicantUsername);

        System.out.println("✅ 申請を承認しました: " + applicantUsername + " -> " + room.getName());
    }

    /**
     * ルーム参加申請を拒否
     */
    public void rejectJoinRequest(Long chatRoomId, String applicantUsername, String creatorUsername) {
        System.out.println("=== rejectJoinRequest 開始 ===");
        System.out.println("roomId: " + chatRoomId + ", applicant: " + applicantUsername + ", creator: " + creatorUsername);

        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatRoomId);
        Optional<User> applicantOpt = userRepository.findByUsername(applicantUsername);
        Optional<User> creatorOpt = userRepository.findByUsername(creatorUsername);

        System.out.println("Room found: " + roomOpt.isPresent());
        System.out.println("Applicant found: " + applicantOpt.isPresent());
        System.out.println("Creator found: " + creatorOpt.isPresent());

        if (roomOpt.isEmpty() || applicantOpt.isEmpty() || creatorOpt.isEmpty()) {
            String detail = "roomOpt.isEmpty()=" + roomOpt.isEmpty() +
                           ", applicantOpt.isEmpty()=" + applicantOpt.isEmpty() +
                           ", creatorOpt.isEmpty()=" + creatorOpt.isEmpty();
            System.err.println("❌ エラー: ルームまたはユーザーが見つかりません - " + detail);
            throw new RuntimeException("ルームまたはユーザーが見つかりません");
        }

        ChatRoom room = roomOpt.get();
        User applicant = applicantOpt.get();
        User creator = creatorOpt.get();

        System.out.println("Room: " + room.getName() + " (ID: " + room.getId() + ")");
        System.out.println("Room creator: " + (room.getCreatedBy() != null ? room.getCreatedBy().getUsername() : "null"));
        System.out.println("Authenticated creator: " + creator.getUsername());

        // ルームの作成者かチェック
        if (room.getCreatedBy() == null || !room.getCreatedBy().equals(creator)) {
            System.err.println("❌ エラー: ルーム作成者ではありません");
            throw new RuntimeException("このルームの作成者ではありません");
        }

        // 申請を検索（重複データ対応）
        List<com.example.chatapp.entity.RoomJoinRequest> requests =
                roomJoinRequestRepository.findAllByChatRoomAndUser(room, applicant);

        System.out.println("Requests found: " + requests.size());

        if (requests.isEmpty()) {
            System.err.println("❌ エラー: 申請が見つかりません");
            throw new RuntimeException("申請が見つかりません");
        }

        // PENDING状態の申請を全て拒否済みに
        boolean foundPending = false;
        for (com.example.chatapp.entity.RoomJoinRequest request : requests) {
            System.out.println("Request ID: " + request.getId() + ", Status: " + request.getStatus());
            if (request.getStatus() == com.example.chatapp.entity.RoomJoinRequest.RequestStatus.PENDING) {
                request.setStatus(com.example.chatapp.entity.RoomJoinRequest.RequestStatus.REJECTED);
                roomJoinRequestRepository.save(request);
                foundPending = true;
            }
        }

        if (!foundPending) {
            System.err.println("❌ エラー: PENDINGの申請がありません");
            throw new RuntimeException("拒否できる申請がありません");
        }

        System.out.println("✅ 申請を拒否しました: " + applicantUsername + " -> " + room.getName());
    }
}
