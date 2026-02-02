package com.example.chatapp.controller;

import com.example.chatapp.dto.ChatRoomDto;
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
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/rooms")
public class ChatRoomController {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

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
        return "rooms"; // 新しいルーム画面を使用
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
     * チャットルーム作成処理（従来のフォーム送信用）
     */
    @PostMapping("/create")
    public String createRoom(@RequestParam String name,
                           @RequestParam(required = false) String description,
                           @RequestParam(defaultValue = "GROUP") String type,
                           @RequestParam(defaultValue = "true") boolean isPublic,
                           Principal principal) {
        if (principal != null) {
            ChatRoom.ChatRoomType roomType = ChatRoom.ChatRoomType.valueOf(type.toUpperCase());
            ChatRoom newRoom = chatRoomService.createChatRoom(name, description, principal.getName(), roomType, isPublic);
            return "redirect:/chat?room=" + newRoom.getId();
        }
        return "redirect:/login";
    }

    /**
     * チャットルーム作成処理（Ajax用API）
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createRoomApi(@RequestParam String name,
                                                             @RequestParam(required = false) String description,
                                                             @RequestParam(defaultValue = "GROUP") String type,
                                                             @RequestParam(defaultValue = "true") boolean isPublic,
                                                             @RequestParam(required = false) String invitedUsernames,
                                                             Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                ChatRoom.ChatRoomType roomType = ChatRoom.ChatRoomType.valueOf(type.toUpperCase());
                ChatRoom newRoom = chatRoomService.createChatRoom(name, description, authentication.getName(), roomType, isPublic);

                // 招待されたユーザーに招待を送信
                if (invitedUsernames != null && !invitedUsernames.trim().isEmpty()) {
                    String[] usernames = invitedUsernames.split(",");
                    for (String username : usernames) {
                        username = username.trim();
                        if (!username.isEmpty()) {
                            try {
                                chatRoomService.inviteUserToRoom(newRoom.getId(), authentication.getName(), username);

                                // WebSocketで招待通知を送信
                                User invitee = userService.findByUsername(username).orElse(null);
                                if (invitee != null) {
                                    Map<String, Object> notification = new HashMap<>();
                                    notification.put("type", "room_invitation");
                                    notification.put("roomId", newRoom.getId());
                                    notification.put("roomName", newRoom.getName());
                                    notification.put("inviterUsername", authentication.getName());
                                    notification.put("inviteeUsername", username);
                                    messagingTemplate.convertAndSend("/topic/rooms", notification);
                                }
                            } catch (Exception e) {
                                System.err.println("ユーザー " + username + " への招待に失敗: " + e.getMessage());
                            }
                        }
                    }
                }

                // WebSocketでルーム作成を全ユーザーに通知
                ChatRoomDto roomDto = new ChatRoomDto(newRoom);
                messagingTemplate.convertAndSend("/topic/rooms", Map.of(
                    "type", "room_created",
                    "room", roomDto
                ));

                response.put("success", true);
                response.put("message", "ルームを作成しました");
                response.put("roomId", newRoom.getId());
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "ルームの作成に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    @Autowired
    private UserService userService;

    /**
     * チャットルーム参加申請
     */
    @PostMapping("/{roomId}/request-join")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> requestJoinRoom(@PathVariable Long roomId,
                                                               Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                chatRoomService.requestJoinRoom(roomId, authentication.getName());

                // WebSocketでルーム作成者に通知
                ChatRoom room = chatRoomService.getChatRoom(roomId).orElse(null);
                if (room != null && room.getCreatedBy() != null) {
                    User applicant = userService.findByUsername(authentication.getName()).orElse(null);
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "join_request");
                    notification.put("roomId", roomId);
                    notification.put("roomName", room.getName());
                    notification.put("creatorUsername", room.getCreatedBy().getUsername());
                    notification.put("applicantUsername", authentication.getName());
                    notification.put("applicantDisplayName", applicant != null && applicant.getProfile() != null
                        ? applicant.getProfile().getDisplayName() : authentication.getName());
                    messagingTemplate.convertAndSend("/topic/rooms", notification);
                }

                response.put("success", true);
                response.put("message", "参加申請を送信しました");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "申請に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
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

                // WebSocketでルーム参加を全ユーザーに通知
                ChatRoom room = chatRoomService.getChatRoom(roomId).orElse(null);
                if (room != null) {
                    ChatRoomDto roomDto = new ChatRoomDto(room);
                    messagingTemplate.convertAndSend("/topic/rooms", Map.of(
                        "type", "user_joined",
                        "room", roomDto,
                        "username", authentication.getName()
                    ));
                }

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

                // WebSocketでルーム退出を全ユーザーに通知
                ChatRoom room = chatRoomService.getChatRoom(roomId).orElse(null);
                if (room != null) {
                    ChatRoomDto roomDto = new ChatRoomDto(room);
                    messagingTemplate.convertAndSend("/topic/rooms", Map.of(
                        "type", "user_left",
                        "room", roomDto,
                        "username", authentication.getName()
                    ));
                }

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

    /**
     * API: 参加中のルーム一覧取得
     */
    @GetMapping("/api/joined")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getJoinedRooms(Authentication authentication) {
        System.out.println("=== /rooms/api/joined にアクセス ===");
        System.out.println("認証情報: " + (authentication != null ? authentication.getName() : "null"));

        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                List<ChatRoom> rooms = chatRoomService.getUserChatRooms(authentication.getName());
                System.out.println("取得したルーム数: " + rooms.size());

                // ChatRoomエンティティをDTOに変換（循環参照を回避）
                List<ChatRoomDto> roomDtos = rooms.stream()
                    .map(ChatRoomDto::new)
                    .collect(Collectors.toList());
                response.put("success", true);
                response.put("rooms", roomDtos);

                System.out.println("✅ 正常にJSON返却");
            } catch (Exception e) {
                System.err.println("❌ エラー発生: " + e.getMessage());
                e.printStackTrace();
                response.put("success", false);
                response.put("message", "ルーム一覧の取得に失敗しました: " + e.getMessage());
            }
        } else {
            System.err.println("❌ 認証情報がnull");
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: 参加可能なパブリックルーム一覧取得（自分が参加していないルームのみ）
     */
    @GetMapping("/api/available")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAvailableRooms(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                List<ChatRoom> rooms = chatRoomService.getAvailablePublicRooms(authentication.getName());
                // ChatRoomエンティティをDTOに変換（循環参照を回避）
                List<ChatRoomDto> roomDtos = rooms.stream()
                    .map(ChatRoomDto::new)
                    .collect(Collectors.toList());
                response.put("success", true);
                response.put("rooms", roomDtos);
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "ルーム一覧の取得に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: 申請中のルーム一覧取得
     */
    @GetMapping("/api/my-requests")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyRequests(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        System.out.println("=== /rooms/api/my-requests にアクセス ===");
        System.out.println("認証ユーザー: " + (authentication != null ? authentication.getName() : "null"));

        if (authentication != null) {
            try {
                List<ChatRoom> rooms = chatRoomService.getRequestedRooms(authentication.getName());
                System.out.println("申請中のルーム数: " + rooms.size());

                // ChatRoomエンティティをDTOに変換
                List<ChatRoomDto> roomDtos = rooms.stream()
                    .map(ChatRoomDto::new)
                    .collect(Collectors.toList());
                response.put("success", true);
                response.put("rooms", roomDtos);
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "申請中のルーム一覧の取得に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: 自分が作成したルームへの参加申請一覧取得
     */
    @GetMapping("/api/pending-approvals")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPendingApprovals(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                List<Map<String, Object>> approvals = chatRoomService.getPendingApprovalsForCreator(authentication.getName());
                response.put("success", true);
                response.put("approvals", approvals);
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "承認待ち一覧の取得に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: ルーム参加申請を承認
     */
    @PostMapping("/{roomId}/approve-request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveRequest(@PathVariable Long roomId,
                                                              @RequestParam String username,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                chatRoomService.approveJoinRequest(roomId, username, authentication.getName());

                // WebSocketで申請者とルーム作成者に通知
                ChatRoom room = chatRoomService.getChatRoom(roomId).orElse(null);
                if (room != null) {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "request_approved");
                    notification.put("roomId", roomId);
                    notification.put("roomName", room.getName());
                    notification.put("applicantUsername", username);
                    notification.put("creatorUsername", authentication.getName());
                    messagingTemplate.convertAndSend("/topic/rooms", notification);
                }

                response.put("success", true);
                response.put("message", "参加申請を承認しました");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "承認に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: ルーム参加申請を拒否
     */
    @PostMapping("/{roomId}/reject-request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectRequest(@PathVariable Long roomId,
                                                             @RequestParam String username,
                                                             Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        System.out.println("=== /rooms/" + roomId + "/reject-request にアクセス ===");
        System.out.println("申請者: " + username);
        System.out.println("認証ユーザー: " + (authentication != null ? authentication.getName() : "null"));

        if (authentication != null) {
            try {
                chatRoomService.rejectJoinRequest(roomId, username, authentication.getName());

                // WebSocketで申請者とルーム作成者に通知
                ChatRoom room = chatRoomService.getChatRoom(roomId).orElse(null);
                if (room != null) {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "request_rejected");
                    notification.put("roomId", roomId);
                    notification.put("roomName", room.getName());
                    notification.put("applicantUsername", username);
                    notification.put("creatorUsername", authentication.getName());
                    messagingTemplate.convertAndSend("/topic/rooms", notification);
                    System.out.println("✅ WebSocket通知送信完了");
                }

                response.put("success", true);
                response.put("message", "参加申請を拒否しました");
                System.out.println("✅ 拒否処理完了");
            } catch (Exception e) {
                System.err.println("❌ 拒否処理でエラー: " + e.getMessage());
                e.printStackTrace();
                response.put("success", false);
                response.put("message", "拒否に失敗しました: " + e.getMessage());
            }
        } else {
            System.err.println("❌ 認証情報がnull");
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: ルーム参加申請をキャンセル
     */
    @DeleteMapping("/{roomId}/cancel-request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelRequest(@PathVariable Long roomId,
                                                             Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                chatRoomService.cancelJoinRequest(roomId, authentication.getName());
                response.put("success", true);
                response.put("message", "参加申請をキャンセルしました");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "申請のキャンセルに失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: ルーム参加者一覧を取得
     */
    @GetMapping("/{roomId}/api/members")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomMembers(@PathVariable Long roomId,
                                                               Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                ChatRoom room = chatRoomService.getChatRoom(roomId).orElse(null);
                if (room == null) {
                    response.put("success", false);
                    response.put("message", "ルームが見つかりません");
                    return ResponseEntity.ok(response);
                }

                // ルーム情報
                response.put("roomId", room.getId());
                response.put("roomName", room.getName());
                response.put("isCreator", room.getCreatedBy() != null &&
                    room.getCreatedBy().getUsername().equals(authentication.getName()));
                response.put("creatorUsername", room.getCreatedBy() != null ?
                    room.getCreatedBy().getUsername() : null);

                // 参加者一覧
                List<Map<String, Object>> members = room.getUsers().stream()
                    .map(user -> {
                        Map<String, Object> memberInfo = new HashMap<>();
                        memberInfo.put("username", user.getUsername());
                        memberInfo.put("displayName", user.getProfile() != null ?
                            user.getProfile().getDisplayName() : user.getUsername());
                        memberInfo.put("avatarUrl", user.getProfile() != null ?
                            user.getProfile().getAvatarUrl() : null);
                        // 自己紹介文の最初の10文字
                        String bio = user.getProfile() != null ? user.getProfile().getBio() : null;
                        if (bio != null && bio.length() > 10) {
                            bio = bio.substring(0, 10) + "...";
                        }
                        memberInfo.put("shortBio", bio);
                        memberInfo.put("isCreator", room.getCreatedBy() != null &&
                            room.getCreatedBy().equals(user));
                        return memberInfo;
                    })
                    .collect(Collectors.toList());

                response.put("success", true);
                response.put("members", members);
                response.put("memberCount", members.size());
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "参加者一覧の取得に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: ルームからユーザーを退会させる（ルーム作成者のみ）
     */
    @PostMapping("/{roomId}/kick-member")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> kickMember(@PathVariable Long roomId,
                                                          @RequestParam String username,
                                                          Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                ChatRoom room = chatRoomService.getChatRoom(roomId).orElse(null);
                if (room == null) {
                    response.put("success", false);
                    response.put("message", "ルームが見つかりません");
                    return ResponseEntity.ok(response);
                }

                // ルーム作成者かチェック
                if (room.getCreatedBy() == null ||
                    !room.getCreatedBy().getUsername().equals(authentication.getName())) {
                    response.put("success", false);
                    response.put("message", "退会させる権限がありません");
                    return ResponseEntity.ok(response);
                }

                // 自分自身は退会させられない
                if (username.equals(authentication.getName())) {
                    response.put("success", false);
                    response.put("message", "自分自身を退会させることはできません");
                    return ResponseEntity.ok(response);
                }

                // ユーザーを退会させる
                chatRoomService.leaveChatRoom(roomId, username);

                // WebSocketで通知
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "member_kicked");
                notification.put("roomId", roomId);
                notification.put("roomName", room.getName());
                notification.put("kickedUsername", username);
                notification.put("kickedBy", authentication.getName());
                messagingTemplate.convertAndSend("/topic/rooms", notification);

                response.put("success", true);
                response.put("message", username + "さんをルームから退会させました");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "退会処理に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: 受け取った招待一覧取得
     */
    @GetMapping("/api/invitations")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReceivedInvitations(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                List<Map<String, Object>> invitations = chatRoomService.getReceivedInvitations(authentication.getName());
                response.put("success", true);
                response.put("invitations", invitations);
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "招待一覧の取得に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: 招待を承認
     */
    @PostMapping("/invitation/{invitationId}/accept")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> acceptInvitation(@PathVariable Long invitationId,
                                                                 Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                chatRoomService.acceptInvitation(invitationId, authentication.getName());

                response.put("success", true);
                response.put("message", "招待を承認しました");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "招待の承認に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: 招待を拒否
     */
    @PostMapping("/invitation/{invitationId}/reject")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectInvitation(@PathVariable Long invitationId,
                                                                 Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                chatRoomService.rejectInvitation(invitationId, authentication.getName());

                response.put("success", true);
                response.put("message", "招待を拒否しました");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "招待の拒否に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: ルームにユーザーを招待
     */
    @PostMapping("/{roomId}/invite")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> inviteUserToRoom(@PathVariable Long roomId,
                                                                 @RequestParam String username,
                                                                 Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                chatRoomService.inviteUserToRoom(roomId, authentication.getName(), username);

                // WebSocketで招待通知を送信
                ChatRoom room = chatRoomService.getChatRoom(roomId).orElse(null);
                if (room != null) {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "room_invitation");
                    notification.put("roomId", roomId);
                    notification.put("roomName", room.getName());
                    notification.put("inviterUsername", authentication.getName());
                    notification.put("inviteeUsername", username);
                    messagingTemplate.convertAndSend("/topic/rooms", notification);
                }

                response.put("success", true);
                response.put("message", "招待を送信しました");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API: ルームへの送信済み招待一覧取得
     */
    @GetMapping("/api/invitations-sent")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSentInvitations(@RequestParam Long roomId,
                                                                   Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null) {
            try {
                List<Map<String, Object>> invitations = chatRoomService.getSentInvitationsForRoom(roomId, authentication.getName());
                response.put("success", true);
                response.put("invitations", invitations);
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "招待一覧の取得に失敗しました: " + e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("message", "ログインが必要です");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * ルームアイコンをアップロード（作成者のみ）
     */
    @PostMapping("/{roomId}/upload-icon")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadRoomIcon(
            @PathVariable Long roomId,
            @RequestParam("icon") MultipartFile file,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.ok(response);
        }

        try {
            ChatRoom room = chatRoomService.uploadRoomIcon(roomId, authentication.getName(), file);
            response.put("success", true);
            response.put("iconUrl", room.getIconUrl());
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "アイコンのアップロードに失敗しました");
        }

        return ResponseEntity.ok(response);
    }
}
