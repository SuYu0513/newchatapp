package com.example.chatapp.controller;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.MatchLike;
import com.example.chatapp.entity.RandomMatch;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.repository.MatchLikeRepository;
import com.example.chatapp.service.FriendshipService;
import com.example.chatapp.service.RandomMatchingService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ランダムマッチング機能のコントローラー
 */
@Controller
@RequestMapping("/random-matching")
public class RandomMatchingController {

    @Autowired
    private RandomMatchingService randomMatchingService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private MatchLikeRepository matchLikeRepository;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * ランダムマッチングページの表示
     */
    @GetMapping
    public String showRandomMatching(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        UserProfile profile = userProfileService.getOrCreateProfile(user);
        
        // ランダムマッチングが許可されているかチェック
        if (!profile.getAllowRandomMatching()) {
            model.addAttribute("error", "ランダムマッチングを使用するには、プロフィール設定で許可してください");
            return "random-matching/not-allowed";
        }

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);

        return "random-matching/index";
    }

    /**
     * マッチング相手の検索とランダムマッチ開始
     */
    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startMatching(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            UserProfile profile = userProfileService.getOrCreateProfile(user);
            
            // ランダムマッチングが許可されているかチェック
            if (!profile.getAllowRandomMatching()) {
                response.put("success", false);
                response.put("message", "ランダムマッチングが許可されていません");
                return ResponseEntity.ok(response);
            }

            // 相性の良いマッチング相手を探す
            User matchedUser = randomMatchingService.findBestMatch(user);
            
            if (matchedUser == null) {
                response.put("success", false);
                response.put("message", "現在マッチング可能なユーザーが見つかりません。既にマッチした相手や、マッチング条件に合うユーザーがオンラインにいない可能性があります。");
                return ResponseEntity.ok(response);
            }

            // ランダムマッチを作成（ルームは作成しない）
            RandomMatch match = randomMatchingService.createMatch(user, matchedUser);

            response.put("success", true);
            response.put("matchedUser", Map.of(
                "id", matchedUser.getId(),
                "username", matchedUser.getUsername(),
                "displayName", userProfileService.getOrCreateProfile(matchedUser).getDisplayNameOrUsername()
            ));
            response.put("matchId", match.getId());
            // roomIdは返さない（チャット開始時に作成）

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "マッチングに失敗しました: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * チャット開始（ルーム作成または既存ルーム使用）
     */
    @PostMapping("/start-chat/{matchId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startChat(@PathVariable Long matchId, 
                                                        Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            // マッチ情報を取得
            RandomMatch match = randomMatchingService.getMatch(matchId)
                .orElseThrow(() -> new RuntimeException("マッチが見つかりません"));

            // ユーザーがこのマッチに参加しているかチェック
            if (!match.involvesUser(user)) {
                response.put("success", false);
                response.put("message", "このマッチにアクセスする権限がありません");
                return ResponseEntity.ok(response);
            }

            // ルームを作成または既存ルームを取得
            ChatRoom chatRoom = randomMatchingService.getOrCreateChatRoom(match);

            response.put("success", true);
            response.put("roomId", chatRoom.getId());
            response.put("roomName", chatRoom.getName());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "チャット開始に失敗しました: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * マッチング履歴の表示
     */
    @GetMapping("/history")
    public String showHistory(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        List<RandomMatchingService.GroupedMatchHistory> groupedHistory = 
            randomMatchingService.getGroupedMatchHistory(user);
        model.addAttribute("groupedHistory", groupedHistory);

        return "random-matching/history";
    }

    /**
     * マッチング統計情報の取得
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMatchingStats(Authentication auth) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // グローバル統計データを取得
            long totalMatches = randomMatchingService.getTotalMatches();
            long activeMatches = randomMatchingService.getActiveMatches();
            long totalUsers = userService.getTotalUserCount();
            
            stats.put("totalMatches", totalMatches);
            stats.put("activeMatches", activeMatches);
            stats.put("totalUsers", totalUsers);
            stats.put("success", true);

        } catch (Exception e) {
            stats.put("error", "統計情報の取得に失敗しました");
            stats.put("success", false);
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * マッチング設定の表示
     */
    @GetMapping("/settings")
    public String showSettings(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        UserProfile profile = userProfileService.getOrCreateProfile(user);
        model.addAttribute("profile", profile);

        return "random-matching/settings";
    }

    /**
     * マッチング設定の保存
     */
    @PostMapping("/settings")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveSettings(
            @RequestParam(value = "allowRandomMatching", defaultValue = "false") boolean allowRandomMatching,
            @RequestParam(value = "autoMatching", defaultValue = "false") boolean autoMatching,
            @RequestParam(value = "minAge", defaultValue = "18") int minAge,
            @RequestParam(value = "maxAge", defaultValue = "50") int maxAge,
            @RequestParam(value = "profileVisibilityLevel", defaultValue = "detailed") String profileVisibilityLevel,
            @RequestParam(value = "avoidBlockedUsers", defaultValue = "false") boolean avoidBlockedUsers,
            @RequestParam(value = "matchNotification", defaultValue = "false") boolean matchNotification,
            @RequestParam(value = "messageNotification", defaultValue = "false") boolean messageNotification,
            Authentication auth) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            UserProfile profile = userProfileService.getOrCreateProfile(user);
            
            // 設定を更新
            profile.setAllowRandomMatching(allowRandomMatching);
            
            // プロフィールを保存
            userProfileService.updateProfile(user, profile);

            response.put("success", true);
            response.put("message", "設定を保存しました");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "設定の保存に失敗しました: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * マッチ候補者一覧を取得（スワイプ用）
     */
    @GetMapping("/api/candidates")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCandidates(
            @RequestParam(value = "type", defaultValue = "nearby") String type,
            Authentication auth) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            UserProfile myProfile = userProfileService.getOrCreateProfile(user);

            // マッチング可能な候補者を取得
            List<UserProfile> candidates = userProfileService.getAvailableForRandomMatching(user);

            // マッチング写真が登録されている候補者のみに絞る
            candidates = candidates.stream()
                .filter(p -> p.getMatchingPhotos() != null && !p.getMatchingPhotos().isEmpty())
                .collect(java.util.stream.Collectors.toList());

            // フレンド・フォロー・フォロワー・マッチ成功者を除外
            java.util.Set<Long> excludeUserIds = new java.util.HashSet<>();
            for (User u : friendshipService.getFriends(user)) {
                excludeUserIds.add(u.getId());
            }
            for (User u : friendshipService.getFollowing(user)) {
                excludeUserIds.add(u.getId());
            }
            for (User u : friendshipService.getFollowers(user)) {
                excludeUserIds.add(u.getId());
            }
            for (MatchLike ml : matchLikeRepository.findMutualLikes(user)) {
                excludeUserIds.add(ml.getLiked().getId());
            }
            candidates = candidates.stream()
                .filter(p -> !excludeUserIds.contains(p.getUser().getId()))
                .collect(java.util.stream.Collectors.toList());

            // タイプに応じてソート
            switch (type) {
                case "nearby": {
                    // 住所が一致 → 先頭
                    String myLocation = myProfile.getLocation();
                    candidates.sort((a, b) -> {
                        boolean aMatch = myLocation != null && myLocation.equals(a.getLocation());
                        boolean bMatch = myLocation != null && myLocation.equals(b.getLocation());
                        if (aMatch && !bMatch) return -1;
                        if (!aMatch && bMatch) return 1;
                        return 0;
                    });
                    break;
                }
                case "hobby": {
                    // 好きなものタグの一致数が多い順
                    java.util.Set<String> myTags = parseTags(myProfile.getFavoriteThings());
                    candidates.sort((a, b) -> {
                        int aCount = countCommonTags(myTags, parseTags(a.getFavoriteThings()));
                        int bCount = countCommonTags(myTags, parseTags(b.getFavoriteThings()));
                        return Integer.compare(bCount, aCount); // 降順
                    });
                    break;
                }
                case "age": {
                    // 生年月日が近い順（birthDate が無ければ ageGroup で比較）
                    candidates.sort((a, b) -> {
                        long aDiff = ageDifference(myProfile, a);
                        long bDiff = ageDifference(myProfile, b);
                        return Long.compare(aDiff, bDiff); // 差が小さい順
                    });
                    break;
                }
                case "destiny": {
                    // 総合スコア（住所 + タグ一致 + 年齢近さ）の高い順
                    String myLoc = myProfile.getLocation();
                    java.util.Set<String> myTagsD = parseTags(myProfile.getFavoriteThings());
                    candidates.sort((a, b) -> {
                        int scoreA = destinyScore(myProfile, a, myLoc, myTagsD);
                        int scoreB = destinyScore(myProfile, b, myLoc, myTagsD);
                        return Integer.compare(scoreB, scoreA); // 降順
                    });
                    break;
                }
            }

            // レスポンス用にデータ変換
            List<Map<String, Object>> candidateList = new java.util.ArrayList<>();
            java.util.Random rand = new java.util.Random();
            for (UserProfile p : candidates) {
                Map<String, Object> c = new HashMap<>();
                c.put("userId", p.getUser().getId());
                c.put("username", p.getUser().getUsername());
                c.put("displayName", p.getDisplayNameOrUsername());
                c.put("bio", p.getBio());
                c.put("location", p.getLocation());
                c.put("favoriteThings", p.getFavoriteThings());

                // マッチング写真からランダムで1枚選択
                String[] photos = p.getMatchingPhotos().split(",");
                c.put("photo", photos[rand.nextInt(photos.length)].trim());
                c.put("allPhotos", List.of(photos));

                candidateList.add(c);
            }

            response.put("success", true);
            response.put("candidates", candidateList);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * マッチング用写真のアップロード
     */
    @PostMapping("/photos/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadMatchingPhoto(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            String photoUrl = userProfileService.uploadMatchingPhoto(user, file);

            response.put("success", true);
            response.put("photoUrl", photoUrl);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * マッチング用写真の削除
     */
    @PostMapping("/photos/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteMatchingPhoto(
            @RequestParam("photoUrl") String photoUrl,
            Authentication auth) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            userProfileService.deleteMatchingPhoto(user, photoUrl);

            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * マッチングの終了
     */
    @PostMapping("/end/{matchId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> endMatch(@PathVariable Long matchId, 
                                                       Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            randomMatchingService.endMatch(matchId, user);

            response.put("success", true);
            response.put("message", "マッチングを終了しました");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "マッチング終了に失敗しました: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // ========================================
    // いいね API
    // ========================================

    /**
     * いいねを送る
     */
    @PostMapping("/api/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendLike(
            @RequestParam("targetUserId") Long targetUserId,
            Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        try {
            User me = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            User target = userService.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("対象ユーザーが見つかりません"));

            if (!matchLikeRepository.existsByLikerAndLiked(me, target)) {
                matchLikeRepository.save(new MatchLike(me, target));
            }

            boolean mutual = matchLikeRepository.isMutualLike(me, target);
            response.put("success", true);
            response.put("mutual", mutual);

            // WebSocketでリアルタイム通知を送信
            UserProfile myProfile = userProfileService.getOrCreateProfile(me);
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", mutual ? "match" : "like");
            notification.put("fromUserId", me.getId());
            notification.put("fromUsername", me.getUsername());
            notification.put("fromDisplayName", myProfile.getDisplayNameOrUsername());
            notification.put("fromAvatarUrl", myProfile.getAvatarUrl());
            messagingTemplate.convertAndSendToUser(
                target.getUsername(), "/queue/match-like", notification);

            // マッチ成立時は相手にも通知
            if (mutual) {
                UserProfile targetProfile = userProfileService.getOrCreateProfile(target);
                Map<String, Object> mutualNotif = new HashMap<>();
                mutualNotif.put("type", "match");
                mutualNotif.put("fromUserId", target.getId());
                mutualNotif.put("fromUsername", target.getUsername());
                mutualNotif.put("fromDisplayName", targetProfile.getDisplayNameOrUsername());
                mutualNotif.put("fromAvatarUrl", targetProfile.getAvatarUrl());
                messagingTemplate.convertAndSendToUser(
                    me.getUsername(), "/queue/match-like", mutualNotif);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * いいねを取り消す（自分が送ったいいねを削除）
     */
    @PostMapping("/api/unlike")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unlike(
            @RequestParam("targetUserId") Long targetUserId,
            Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        try {
            User me = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            User target = userService.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("対象ユーザーが見つかりません"));

            matchLikeRepository.findByLikerAndLiked(me, target)
                .ifPresent(matchLikeRepository::delete);

            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * もらったいいねを拒否する（相手のいいねを削除）
     */
    @PostMapping("/api/reject")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectLike(
            @RequestParam("fromUserId") Long fromUserId,
            Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        try {
            User me = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            User from = userService.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("対象ユーザーが見つかりません"));

            matchLikeRepository.findByLikerAndLiked(from, me)
                .ifPresent(matchLikeRepository::delete);

            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 自分がいいねした人一覧
     */
    @GetMapping("/api/likes/sent")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLikesSent(Authentication auth) {
        return buildLikeListResponse(auth, "sent");
    }

    /**
     * 自分をいいねした人一覧
     */
    @GetMapping("/api/likes/received")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLikesReceived(Authentication auth) {
        return buildLikeListResponse(auth, "received");
    }

    /**
     * マッチ成立（相互いいね）一覧
     */
    @GetMapping("/api/likes/mutual")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLikesMutual(Authentication auth) {
        return buildLikeListResponse(auth, "mutual");
    }

    /** いいね一覧レスポンス共通ビルダー */
    private ResponseEntity<Map<String, Object>> buildLikeListResponse(Authentication auth, String kind) {
        Map<String, Object> response = new HashMap<>();
        try {
            User me = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            List<MatchLike> likes;
            switch (kind) {
                case "sent":     likes = matchLikeRepository.findByLikerOrderByCreatedAtDesc(me); break;
                case "received": likes = matchLikeRepository.findByLikedOrderByCreatedAtDesc(me); break;
                default:         likes = matchLikeRepository.findMutualLikes(me); break;
            }

            // sent/received からマッチ成立済み（相互いいね）を除外
            if ("sent".equals(kind) || "received".equals(kind)) {
                java.util.Set<Long> mutualUserIds = new java.util.HashSet<>();
                for (MatchLike ml : matchLikeRepository.findMutualLikes(me)) {
                    mutualUserIds.add(ml.getLiked().getId());
                }
                likes = likes.stream()
                    .filter(ml -> {
                        User other = "received".equals(kind) ? ml.getLiker() : ml.getLiked();
                        return !mutualUserIds.contains(other.getId());
                    })
                    .collect(java.util.stream.Collectors.toList());
            }

            List<Map<String, Object>> users = new java.util.ArrayList<>();
            for (MatchLike ml : likes) {
                User other = kind.equals("received") ? ml.getLiker() : ml.getLiked();
                UserProfile p = userProfileService.getOrCreateProfile(other);
                Map<String, Object> u = new HashMap<>();
                u.put("userId", other.getId());
                u.put("username", other.getUsername());
                u.put("displayName", p.getDisplayNameOrUsername());
                u.put("avatarUrl", p.getAvatarUrl());
                u.put("bio", p.getBio());
                u.put("location", p.getLocation());
                u.put("favoriteThings", p.getFavoriteThings());
                // マッチング写真の1枚目をサムネイルに
                if (p.getMatchingPhotos() != null && !p.getMatchingPhotos().isEmpty()) {
                    String[] photos = p.getMatchingPhotos().split(",");
                    u.put("photo", photos[new java.util.Random().nextInt(photos.length)].trim());
                } else {
                    u.put("photo", p.getAvatarUrl());
                }
                u.put("createdAt", ml.getCreatedAt().toString());
                users.add(u);
            }

            response.put("success", true);
            response.put("users", users);
            response.put("count", users.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ========================================
    // ソート用ヘルパーメソッド
    // ========================================

    /** カンマ区切り文字列をタグSetに変換 */
    private java.util.Set<String> parseTags(String csv) {
        java.util.Set<String> tags = new java.util.HashSet<>();
        if (csv == null || csv.isEmpty()) return tags;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) tags.add(t);
        }
        return tags;
    }

    /** 共通タグ数を数える */
    private int countCommonTags(java.util.Set<String> myTags, java.util.Set<String> otherTags) {
        int count = 0;
        for (String tag : otherTags) {
            if (myTags.contains(tag)) count++;
        }
        return count;
    }

    /** 年齢差（日数の差の絶対値）。birthDate が無い場合は ageGroup で概算 */
    private long ageDifference(UserProfile me, UserProfile other) {
        if (me.getBirthDate() != null && other.getBirthDate() != null) {
            return Math.abs(
                java.time.temporal.ChronoUnit.DAYS.between(me.getBirthDate(), other.getBirthDate())
            );
        }
        // birthDate が無い場合、ageGroup の序数差で概算（1グループ ≈ 10年 = 3650日）
        int meOrd = me.getAgeGroup() != null ? me.getAgeGroup().ordinal() : 99;
        int otherOrd = other.getAgeGroup() != null ? other.getAgeGroup().ordinal() : 99;
        return (long) Math.abs(meOrd - otherOrd) * 3650;
    }

    /** 運命スコア（高いほど相性が良い） */
    private int destinyScore(UserProfile me, UserProfile other,
                             String myLocation, java.util.Set<String> myTags) {
        int score = 0;

        // 住所一致 +30
        if (myLocation != null && myLocation.equals(other.getLocation())) {
            score += 30;
        }

        // タグ一致 1つにつき +15
        score += countCommonTags(myTags, parseTags(other.getFavoriteThings())) * 15;

        // 年齢差が近い +20〜0（差0日=+20, 差3650日以上=+0）
        long daysDiff = ageDifference(me, other);
        score += Math.max(0, 20 - (int)(daysDiff / 183));

        // ageGroup 一致 +10
        if (me.getAgeGroup() != null && me.getAgeGroup() == other.getAgeGroup()) {
            score += 10;
        }

        // chatStyle 一致 +10
        if (me.getChatStyle() != null && me.getChatStyle() == other.getChatStyle()) {
            score += 10;
        }

        return score;
    }
}
