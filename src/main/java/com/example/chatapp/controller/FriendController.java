package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.Friendship;
import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.FriendshipService;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/friends")
public class FriendController {

    @Autowired
    private FriendshipService friendshipService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ChatRoomService chatRoomService;
    
    @Autowired
    private OnlineUserService onlineUserService;

    // フレンド一覧ページ
    @GetMapping
    public String friendsList(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        String username = principal.getName();
        Optional<User> userOpt = userService.findByUsername(username);
        
        if (!userOpt.isPresent()) {
            model.addAttribute("errorMessage", "ユーザー情報が見つかりません");
            return "error";
        }
        
        User currentUser = userOpt.get();
        
        try {
            // フレンド一覧を取得
            List<User> friends = friendshipService.getFriends(currentUser);
            
            // 送信した申請一覧（PENDING状態）
            List<Friendship> sentRequestsEntities = friendshipService.getPendingRequestsBySender(currentUser);
            List<User> sentRequests = sentRequestsEntities.stream()
                .map(Friendship::getAddressee)
                .collect(Collectors.toList());
            
            // 受信した申請一覧（PENDING状態）
            List<Friendship> receivedRequestsEntities = friendshipService.getPendingRequestsByAddressee(currentUser);
            List<User> receivedRequests = receivedRequestsEntities.stream()
                .map(Friendship::getRequester)
                .collect(Collectors.toList());
            
            // ブロック済みユーザー一覧
            List<User> blockedUsers = friendshipService.getBlockedUsers(currentUser);
            
            model.addAttribute("friends", friends);
            model.addAttribute("sentRequests", sentRequests);
            model.addAttribute("receivedRequests", receivedRequests);
            model.addAttribute("receivedRequestsEntities", receivedRequestsEntities);
            model.addAttribute("blockedUsers", blockedUsers);
            model.addAttribute("currentUser", currentUser);
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "フレンド情報の取得に失敗しました: " + e.getMessage());
        }
        
        return "friends/list";
    }

    // フレンド一覧ページ（/friends/listでも同じページを表示）
    @GetMapping("/list")
    public String friendsListAlt(Model model, Principal principal) {
        return friendsList(model, principal);
    }

    // フレンド申請送信 (AJAX API)
    @PostMapping("/request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendFriendRequest(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("=== フレンド申請API呼び出し ===");
        System.out.println("ペイロード: " + payload);
        
        if (principal == null) {
            System.out.println("エラー: ログインが必要");
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> requesterOpt = userService.findByUsername(username);
            if (!requesterOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long targetUserId = Long.valueOf(payload.get("targetUserId").toString());
            Optional<User> targetOpt = userService.findById(targetUserId);
            if (!targetOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "対象ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Friendship friendship = friendshipService.sendFriendRequest(requesterOpt.get(), targetOpt.get());
            
            if (friendship != null) {
                response.put("success", true);
                response.put("message", "フレンド申請を送信しました");
            } else {
                response.put("success", false);
                response.put("message", "フレンド申請の送信に失敗しました");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "エラーが発生しました: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // フレンド申請承認 (AJAX API)
    @PostMapping("/accept")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> acceptFriendRequest(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long requesterId = Long.valueOf(payload.get("requesterId").toString());
            Optional<User> requesterOpt = userService.findById(requesterId);
            if (!requesterOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "申請者が見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Friendship friendship = friendshipService.acceptFriendRequest(requesterOpt.get(), currentUserOpt.get());
            
            if (friendship != null) {
                response.put("success", true);
                response.put("message", "フレンド申請を承認しました");
            } else {
                response.put("success", false);
                response.put("message", "フレンド申請の承認に失敗しました");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "エラーが発生しました: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // フレンド申請拒否 (AJAX API)
    @PostMapping("/decline")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> declineFriendRequest(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long requesterId = Long.valueOf(payload.get("requesterId").toString());
            Optional<User> requesterOpt = userService.findById(requesterId);
            if (!requesterOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "申請者が見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Friendship friendship = friendshipService.declineFriendRequest(requesterOpt.get(), currentUserOpt.get());
            
            if (friendship != null) {
                response.put("success", true);
                response.put("message", "フレンド申請を拒否しました");
            } else {
                response.put("success", false);
                response.put("message", "フレンド申請の拒否に失敗しました");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "エラーが発生しました: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // フレンド削除 (AJAX API)
    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFriend(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long friendId = Long.valueOf(payload.get("friendId").toString());
            Optional<User> friendOpt = userService.findById(friendId);
            if (!friendOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "フレンドが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            friendshipService.removeFriendByUsers(currentUserOpt.get(), friendOpt.get());
            
            response.put("success", true);
            response.put("message", "フレンドを削除しました");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "エラーが発生しました: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // フレンド申請取り消し (AJAX API)
    @PostMapping("/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelFriendRequest(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("=== フレンド申請取り消しAPI呼び出し ===");
        System.out.println("ペイロード: " + payload);
        
        if (principal == null) {
            System.out.println("エラー: ログインが必要");
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long targetUserId = Long.valueOf(payload.get("targetUserId").toString());
            Optional<User> targetUserOpt = userService.findById(targetUserId);
            if (!targetUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "対象ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            User currentUser = currentUserOpt.get();
            User targetUser = targetUserOpt.get();
            
            System.out.println("現在のユーザー: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");
            System.out.println("対象ユーザー: " + targetUser.getUsername() + " (ID: " + targetUser.getId() + ")");
            
            // 自分が送信したPENDING状態の申請を検索して削除
            Optional<Friendship> pendingRequest = friendshipService.getFriendshipBetween(currentUser, targetUser);
            
            if (pendingRequest.isPresent()) {
                Friendship friendship = pendingRequest.get();
                
                // 自分が送信者で、状態がPENDINGであることを確認
                if (friendship.getRequester().equals(currentUser) && 
                    friendship.getStatus() == Friendship.FriendshipStatus.PENDING) {
                    
                    // 専用メソッドで申請を取り消し
                    friendshipService.cancelFriendRequest(currentUser, targetUser);
                    
                    response.put("success", true);
                    response.put("message", "フレンド申請を取り消しました");
                    System.out.println("申請取り消し成功");
                } else {
                    response.put("success", false);
                    response.put("message", "取り消せる申請が見つかりません");
                    System.out.println("取り消せる申請なし - Requester: " + friendship.getRequester().getUsername() + 
                                     ", Status: " + friendship.getStatus());
                }
            } else {
                response.put("success", false);
                response.put("message", "申請が見つかりません");
                System.out.println("申請が見つかりません");
            }
            
        } catch (Exception e) {
            System.out.println("エラー: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "エラーが発生しました: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // ユーザーブロック (AJAX API)
    @PostMapping("/block")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> blockUser(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long targetUserId = Long.valueOf(payload.get("targetUserId").toString());
            Optional<User> targetUserOpt = userService.findById(targetUserId);
            if (!targetUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "対象ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Friendship friendship = friendshipService.blockUser(currentUserOpt.get(), targetUserOpt.get());
            
            if (friendship != null) {
                response.put("success", true);
                response.put("message", "ユーザーをブロックしました");
            } else {
                response.put("success", false);
                response.put("message", "ユーザーのブロックに失敗しました");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "エラーが発生しました: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // プライベートチャット開始 (AJAX API)
    @PostMapping("/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startPrivateChat(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("=== プライベートチャットAPI呼び出し ===");
        System.out.println("ペイロード: " + payload);
        
        if (principal == null) {
            System.out.println("エラー: ログインが必要");
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long friendId = Long.valueOf(payload.get("friendId").toString());
            Optional<User> friendOpt = userService.findById(friendId);
            if (!friendOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "フレンドが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            User currentUser = currentUserOpt.get();
            User friend = friendOpt.get();
            
            // フレンド関係を確認
            if (!friendshipService.areFriends(currentUser, friend)) {
                response.put("success", false);
                response.put("message", "フレンド関係にないユーザーとはチャットできません");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 既存のプライベートチャットルームを検索
            List<ChatRoom> userRooms = chatRoomService.getRoomsByUser(currentUser);
            ChatRoom existingRoom = null;
            
            for (ChatRoom room : userRooms) {
                if (room.getType() == ChatRoom.ChatRoomType.PRIVATE && 
                    room.getUsers().contains(friend) && 
                    room.getUsers().size() == 2) {
                    existingRoom = room;
                    break;
                }
            }
            
            ChatRoom chatRoom;
            if (existingRoom != null) {
                // 既存のルームを使用
                chatRoom = existingRoom;
            } else {
                // 新しいプライベートチャットルームを作成
                String roomName = "Private: " + currentUser.getUsername() + " - " + friend.getUsername();
                chatRoom = chatRoomService.createPrivateRoom(roomName, currentUser);
                
                // 友達もルームに追加
                chatRoomService.addUserToRoom(chatRoom.getId(), friend);
            }
            
            response.put("success", true);
            response.put("roomId", chatRoom.getId());
            response.put("roomName", chatRoom.getName());
            response.put("message", "プライベートチャットを開始しました");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "エラーが発生しました: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // フレンド申請通知数取得 (AJAX API)
    @GetMapping("/api/notification-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationCount(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("count", 0);
            return ResponseEntity.ok(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                response.put("count", 0);
                return ResponseEntity.ok(response);
            }
            
            List<Friendship> pendingRequests = friendshipService.getPendingRequestsByAddressee(currentUserOpt.get());
            response.put("count", pendingRequests.size());
            
        } catch (Exception e) {
            response.put("count", 0);
        }
        
        return ResponseEntity.ok(response);
    }

    // フレンド一覧取得 (AJAX API)
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFriendsList(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<Friendship> friendships = friendshipService.getFriendshipsByUser(currentUserOpt.get());
            List<Map<String, Object>> result = friendships.stream()
                .map(friendship -> {
                    User friend = friendship.getRequester().equals(currentUserOpt.get()) 
                        ? friendship.getAddressee() : friendship.getRequester();
                    
                    Map<String, Object> friendData = new HashMap<>();
                    friendData.put("id", friend.getId());
                    friendData.put("username", friend.getUsername());
                    // OnlineUserServiceを使用してリアルタイムのオンライン状態を取得
                    String onlineStatus = onlineUserService.getUserStatusById(friend.getId());
                    friendData.put("online", "online".equals(onlineStatus));
                    friendData.put("status", onlineStatus);
                    
                    return friendData;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 送信済み申請一覧取得 (AJAX API)
    @GetMapping("/api/sent-requests")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSentRequests(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<Friendship> sentRequests = friendshipService.getPendingRequestsByRequester(currentUserOpt.get());
            List<Map<String, Object>> result = sentRequests.stream()
                .map(friendship -> {
                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("id", friendship.getId());
                    
                    Map<String, Object> addresseeData = new HashMap<>();
                    addresseeData.put("id", friendship.getAddressee().getId());
                    addresseeData.put("username", friendship.getAddressee().getUsername());
                    requestData.put("addressee", addresseeData);
                    
                    return requestData;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ブロック一覧取得 (AJAX API)
    @GetMapping("/api/blocked")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getBlockedUsers(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<Friendship> blockedFriendships = friendshipService.getBlockedUsersByUser(currentUserOpt.get());
            List<Map<String, Object>> result = blockedFriendships.stream()
                .map(friendship -> {
                    User blockedUser = friendship.getRequester().equals(currentUserOpt.get()) 
                        ? friendship.getAddressee() : friendship.getRequester();
                    
                    Map<String, Object> blockedData = new HashMap<>();
                    blockedData.put("id", blockedUser.getId());
                    blockedData.put("username", blockedUser.getUsername());
                    
                    return blockedData;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 受信申請一覧取得 (AJAX API)
    @GetMapping("/api/received-requests")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getReceivedRequests(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<Friendship> requests = friendshipService.getPendingRequestsByAddressee(currentUserOpt.get());
            List<Map<String, Object>> result = requests.stream()
                .map(friendship -> {
                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("id", friendship.getId());
                    
                    Map<String, Object> requesterData = new HashMap<>();
                    requesterData.put("id", friendship.getRequester().getId());
                    requesterData.put("username", friendship.getRequester().getUsername());
                    requestData.put("requester", requesterData);
                    
                    return requestData;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ユーザーブロック解除 (AJAX API)
    @PostMapping("/unblock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unblockUser(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> currentUserOpt = userService.findByUsername(username);
            if (!currentUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long targetUserId = Long.valueOf(payload.get("targetUserId").toString());
            Optional<User> targetUserOpt = userService.findById(targetUserId);
            if (!targetUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "対象ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            friendshipService.unblockUser(currentUserOpt.get(), targetUserOpt.get());
            
            response.put("success", true);
            response.put("message", "ブロックを解除しました");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "エラーが発生しました: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
