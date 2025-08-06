package com.example.chatapp.controller;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.Friendship;
import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.FriendshipService;
import com.example.chatapp.service.ChatRoomService;
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

    // フレンド申請送信 (AJAX API)
    @PostMapping("/request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendFriendRequest(
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
            
            Friendship friendship = friendshipService.acceptFriendRequest(requesterOpt.get(), currentUserOpt.get().getId());
            
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
            
            Friendship friendship = friendshipService.declineFriendRequest(requesterOpt.get(), currentUserOpt.get().getId());
            
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
            
            friendshipService.removeFriend(currentUserOpt.get(), friendId);
            
            response.put("success", true);
            response.put("message", "フレンドを削除しました");
            
        } catch (Exception e) {
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
            
            // プライベートチャットルームを作成または取得
            String roomName = "Private: " + currentUserOpt.get().getUsername() + " - " + friendOpt.get().getUsername();
            ChatRoom chatRoom = chatRoomService.createPrivateRoom(roomName, currentUserOpt.get());
            
            if (chatRoom != null) {
                response.put("success", true);
                response.put("roomId", chatRoom.getId());
                response.put("roomName", chatRoom.getName());
                response.put("message", "プライベートチャットを開始しました");
            } else {
                response.put("success", false);
                response.put("message", "チャットルームの作成に失敗しました");
            }
            
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
}
