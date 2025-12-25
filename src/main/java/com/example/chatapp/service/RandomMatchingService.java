package com.example.chatapp.service;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RandomMatch;
import com.example.chatapp.entity.RandomMatch.MatchStatus;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.RandomMatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RandomMatchingService {

    @Autowired
    private RandomMatchRepository randomMatchRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ChatRoomService chatRoomService;
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    private final Random random = new Random();
    private static final int MATCH_TIMEOUT_MINUTES = 30;

    /**
     * 最適なマッチング相手を見つける（相性重視）
     * 既存のマッチ（チャットルーム）がある相手は除外
     */
    public User findBestMatch(User user) {
        UserProfile userProfile = userProfileService.getOrCreateProfile(user);
        
        // ランダムマッチング許可済みで、現在アクティブでないユーザーを取得
        List<UserProfile> candidates = userProfileService.getAvailableForRandomMatching(user);
        
        if (candidates.isEmpty()) {
            return null;
        }

        // 既存のマッチ（チャットルーム）がある相手を除外
        candidates = candidates.stream()
            .filter(profile -> !hasExistingMatch(user, profile.getUser()))
            .collect(java.util.stream.Collectors.toList());
        
        if (candidates.isEmpty()) {
            return null;
        }

        // 相性スコアを計算してソート
        List<MatchCandidate> scoredCandidates = candidates.stream()
            .map(profile -> new MatchCandidate(profile.getUser(), calculateCompatibilityScore(userProfile, profile)))
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .collect(java.util.stream.Collectors.toList());

        // 上位30%からランダムに選択（完全な相性マッチではなく、適度なランダム性を保つ）
        int topCandidatesCount = Math.max(1, scoredCandidates.size() / 3);
        List<MatchCandidate> topCandidates = scoredCandidates.subList(0, topCandidatesCount);
        
        return topCandidates.get(random.nextInt(topCandidates.size())).user;
    }

    /**
     * 二人のユーザー間に既存のマッチ（チャットルーム）があるかチェック
     */
    private boolean hasExistingMatch(User user1, User user2) {
        // ランダムマッチの履歴をチェック
        List<RandomMatch> existingMatches = randomMatchRepository.findByUser1AndUser2OrUser2AndUser1(user1, user2);
        
        // 既存のマッチがあり、かつそのマッチにチャットルームが存在するかチェック
        for (RandomMatch match : existingMatches) {
            if (match.getChatRoom() != null) {
                return true; // 既存のチャットルームがある
            }
        }
        
        return false; // 既存のマッチなし、またはチャットルームがまだ作成されていない
    }

    /**
     * 相性スコアを計算
     */
    private double calculateCompatibilityScore(UserProfile profile1, UserProfile profile2) {
        double score = 0.0;
        int factors = 0;

        // 年齢層の相性
        if (profile1.getAgeGroup() != null && profile2.getAgeGroup() != null) {
            if (profile1.getAgeGroup() == profile2.getAgeGroup()) {
                score += 20.0;
            } else {
                // 隣接する年齢層は半分のスコア
                if (areAdjacentAgeGroups(profile1.getAgeGroup(), profile2.getAgeGroup())) {
                    score += 10.0;
                }
            }
            factors++;
        }

        // チャットスタイルの相性
        if (profile1.getChatStyle() != null && profile2.getChatStyle() != null) {
            if (profile1.getChatStyle() == profile2.getChatStyle()) {
                score += 15.0;
            }
            factors++;
        }

        // 興味・関心の一致度
        score += calculateTextSimilarity(profile1.getInterests(), profile2.getInterests()) * 15.0;
        factors++;

        // 趣味の一致度
        score += calculateTextSimilarity(profile1.getHobbies(), profile2.getHobbies()) * 15.0;
        factors++;

        // 好きなものの一致度
        score += calculateTextSimilarity(profile1.getFavoriteThings(), profile2.getFavoriteThings()) * 10.0;
        factors++;

        // 話せる言語の一致度
        score += calculateTextSimilarity(profile1.getLanguages(), profile2.getLanguages()) * 5.0;
        factors++;

        return factors > 0 ? score / factors : 0.0;
    }

    /**
     * 隣接する年齢層かどうかを判定
     */
    private boolean areAdjacentAgeGroups(UserProfile.AgeGroup age1, UserProfile.AgeGroup age2) {
        List<UserProfile.AgeGroup> ageOrder = Arrays.asList(
            UserProfile.AgeGroup.TEENS,
            UserProfile.AgeGroup.TWENTIES,
            UserProfile.AgeGroup.THIRTIES,
            UserProfile.AgeGroup.FORTIES,
            UserProfile.AgeGroup.FIFTIES_PLUS
        );
        
        int index1 = ageOrder.indexOf(age1);
        int index2 = ageOrder.indexOf(age2);
        
        return Math.abs(index1 - index2) == 1;
    }

    /**
     * テキストの類似度を計算（カンマ区切りのリスト用）
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.trim().isEmpty() || text2.trim().isEmpty()) {
            return 0.0;
        }

        Set<String> set1 = Arrays.stream(text1.toLowerCase().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(java.util.stream.Collectors.toSet());

        Set<String> set2 = Arrays.stream(text2.toLowerCase().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(java.util.stream.Collectors.toSet());

        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * マッチングを作成（ルームは作成しない）
     */
    public RandomMatch createMatch(User user1, User user2) {
        // ランダムマッチを作成（ルームは後で作成）
        RandomMatch randomMatch = new RandomMatch(user1, user2);
        return randomMatchRepository.save(randomMatch);
    }

    /**
     * チャット開始時にルームを作成または既存ルームを取得（トランザクション強化）
     */
    @Transactional
    public ChatRoom getOrCreateChatRoom(RandomMatch match) {
        try {
            User user1 = match.getUser1();
            User user2 = match.getUser2();
            
            System.out.println("ランダムマッチのチャットルーム作成/取得開始: " + user1.getUsername() + " & " + user2.getUsername());
            
            // 既存のプライベートルームを検索
            Optional<ChatRoom> existingRoom = chatRoomService.findExistingPrivateRoom(user1, user2);
            
            if (existingRoom.isPresent()) {
                // 既存ルームがある場合はそれを使用
                ChatRoom room = existingRoom.get();
                System.out.println("既存のチャットルームを使用: " + room.getName() + " (ID: " + room.getId() + ")");
                
                // マッチにルームを関連付け
                match.setChatRoom(room);
                randomMatchRepository.save(match);
                randomMatchRepository.flush(); // 確実に保存
                
                return room;
            } else {
                // 新しいルームを作成
                String roomName = "ランダムチャット - " + user1.getUsername() + " & " + user2.getUsername();
                ChatRoom chatRoom = chatRoomService.createPrivateRoom(roomName, user1);
                
                // 相手をルームに追加
                chatRoomService.addUserToRoom(chatRoom.getId(), user2);
                
                // ルームタイプをRANDOMに設定
                chatRoom.setType(ChatRoom.ChatRoomType.RANDOM);
                chatRoom = chatRoomRepository.save(chatRoom);
                chatRoomRepository.flush(); // 確実に保存
                
                System.out.println("新しいランダムチャットルームを作成: " + chatRoom.getName() + " (ID: " + chatRoom.getId() + ")");
                
                // マッチにルームを設定
                match.setChatRoom(chatRoom);
                match.setStatus(MatchStatus.ACTIVE);
                randomMatchRepository.save(match);
                randomMatchRepository.flush(); // 確実に保存
                
                System.out.println("マッチをアクティブ状態に更新: MatchID=" + match.getId());
                
                return chatRoom;
            }
        } catch (Exception e) {
            System.err.println("チャットルーム作成/取得エラー: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("チャットルームの作成に失敗しました", e);
        }
    }

    /**
     * マッチング候補のヘルパークラス
     */
    private static class MatchCandidate {
        final User user;
        final double score;

        MatchCandidate(User user, double score) {
            this.user = user;
            this.score = score;
        }
    }

    /**
     * ランダムマッチングを開始
     */
    public Optional<RandomMatch> findRandomMatch(User user) {
        // マッチング可能なユーザーを取得（複数マッチングを許可するため、アクティブマッチチェックを削除）
        List<UserProfile> availableProfiles = userProfileService.getAvailableForRandomMatching(user);
        
        if (availableProfiles.isEmpty()) {
            return Optional.empty();
        }

        // ランダムに相手を選択
        UserProfile matchedProfile = availableProfiles.get(random.nextInt(availableProfiles.size()));
        User matchedUser = matchedProfile.getUser();

        // ランダムマッチを作成（ルームは後で作成）
        RandomMatch randomMatch = new RandomMatch(user, matchedUser);
        return Optional.of(randomMatchRepository.save(randomMatch));
    }

    /**
     * マッチを取得
     */
    @Transactional(readOnly = true)
    public Optional<RandomMatch> getMatch(Long matchId) {
        return randomMatchRepository.findById(matchId);
    }

    /**
     * アクティブなマッチを取得
     */
    @Transactional(readOnly = true)
    public Optional<RandomMatch> getActiveMatch(User user) {
        return randomMatchRepository.findActiveMatchForUser(user);
    }

    /**
     * マッチを終了
     */
    public void endMatch(Long matchId, User endedByUser) {
        RandomMatch match = randomMatchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("マッチが見つかりません"));

        // ユーザーがこのマッチに参加していることを確認
        if (!match.involvesUser(endedByUser)) {
            throw new IllegalArgumentException("このマッチを終了する権限がありません");
        }

        // マッチを終了
        match.endMatch(endedByUser);
        randomMatchRepository.save(match);

        // チャットルームも非アクティブにする
        if (match.getChatRoom() != null) {
            chatRoomService.deactivateRoom(match.getChatRoom().getId());
        }
    }

    /**
     * マッチからの退出
     */
    public void leaveMatch(User user) {
        Optional<RandomMatch> activeMatch = getActiveMatch(user);
        if (activeMatch.isPresent()) {
            RandomMatch match = activeMatch.get();
            match.setStatus(RandomMatch.MatchStatus.ABANDONED);
            match.setEndedByUser(user);
            match.setEndedAt(LocalDateTime.now());
            randomMatchRepository.save(match);

            // チャットルームからも退出
            if (match.getChatRoom() != null) {
                chatRoomService.removeUserFromRoom(match.getChatRoom().getId(), user);
            }
        }
    }

    /**
     * メッセージ数を増加
     */
    public void incrementMessageCount(Long chatRoomId) {
        Optional<RandomMatch> match = randomMatchRepository.findByChatRoomId(chatRoomId);
        if (match.isPresent() && match.get().isActive()) {
            match.get().incrementMessageCount();
            randomMatchRepository.save(match.get());
        }
    }

    /**
     * ユーザーの全マッチ履歴を取得
     */
    @Transactional(readOnly = true)
    public List<RandomMatch> getMatchHistory(User user) {
        return randomMatchRepository.findAllMatchesForUser(user);
    }

    /**
     * ユーザーの全マッチ履歴を取得（同じ相手をまとめて）
     */
    @Transactional(readOnly = true)
    public List<GroupedMatchHistory> getGroupedMatchHistory(User user) {
        List<RandomMatch> allMatches = randomMatchRepository.findAllMatchesForUser(user);
        
        // ユーザーごとにグループ化
        Map<User, List<RandomMatch>> groupedMatches = allMatches.stream()
            .collect(Collectors.groupingBy(match -> match.getOtherUser(user)));
        
        // GroupedMatchHistoryに変換
        return groupedMatches.entrySet().stream()
            .map(entry -> {
                User otherUser = entry.getKey();
                List<RandomMatch> matches = entry.getValue();
                
                // 最新のマッチを代表として使用
                RandomMatch latestMatch = matches.stream()
                    .max(Comparator.comparing(RandomMatch::getCreatedAt))
                    .orElse(matches.get(0));
                
                return new GroupedMatchHistory(
                    otherUser,
                    matches.size(),
                    latestMatch,
                    matches.stream().mapToInt(m -> m.getMessageCount() != null ? m.getMessageCount() : 0).sum(),
                    matches.stream().filter(m -> m.getStatus() == RandomMatch.MatchStatus.ACTIVE).count()
                );
            })
            .sorted(Comparator.comparing((GroupedMatchHistory gh) -> gh.getLatestMatch().getCreatedAt()).reversed())
            .collect(Collectors.toList());
    }

    /**
     * 完了したマッチ履歴を取得
     */
    @Transactional(readOnly = true)
    public List<RandomMatch> getCompletedMatches(User user) {
        return randomMatchRepository.findCompletedMatchesForUser(user);
    }

    /**
     * アクティブなマッチ数を取得
     */
    @Transactional(readOnly = true)
    public long getActiveMatchCount() {
        return randomMatchRepository.countActiveMatches();
    }
    
    /**
     * 総マッチ数を取得
     */
    @Transactional(readOnly = true)
    public long getTotalMatches() {
        return randomMatchRepository.count();
    }
    
    /**
     * アクティブなマッチ数を取得（統計用）
     */
    @Transactional(readOnly = true)
    public long getActiveMatches() {
        return getActiveMatchCount();
    }

    /**
     * タイムアウトしたマッチを処理
     */
    @Transactional
    public void processTimedOutMatches() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(MATCH_TIMEOUT_MINUTES);
        List<RandomMatch> timedOutMatches = randomMatchRepository.findTimedOutMatches(timeoutThreshold);
        
        for (RandomMatch match : timedOutMatches) {
            match.setStatus(RandomMatch.MatchStatus.TIMEOUT);
            match.setEndedAt(LocalDateTime.now());
            randomMatchRepository.save(match);
            
            // チャットルームも非アクティブにする
            if (match.getChatRoom() != null) {
                chatRoomService.deactivateRoom(match.getChatRoom().getId());
            }
        }
    }

    /**
     * マッチング統計を取得
     */
    @Transactional(readOnly = true)
    public MatchingStatistics getStatistics() {
        MatchingStatistics stats = new MatchingStatistics();
        stats.setActiveMatches(randomMatchRepository.countActiveMatches());
        stats.setAverageDuration(randomMatchRepository.getAverageMatchDuration());
        stats.setAverageMessageCount(randomMatchRepository.getAverageMessageCount());
        
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);
        stats.setTodayMatches(randomMatchRepository.countMatchesBetween(today, tomorrow));
        
        return stats;
    }

    /**
     * チャットルームIDからマッチを取得
     */
    @Transactional(readOnly = true)
    public Optional<RandomMatch> getMatchByChatRoom(Long chatRoomId) {
        return randomMatchRepository.findByChatRoomId(chatRoomId);
    }

    /**
     * ユーザーがランダムマッチング可能かどうかを確認
     */
    @Transactional(readOnly = true)
    public boolean canStartRandomMatch(User user) {
        // プロフィール設定をチェック（複数マッチング許可のため、アクティブマッチの存在チェックは削除）
        UserProfile profile = userProfileService.getOrCreateProfile(user);
        return profile.getAllowRandomMatching();
    }

    /**
     * マッチング統計クラス
     */
    public static class MatchingStatistics {
        private long activeMatches;
        private Double averageDuration;
        private Double averageMessageCount;
        private long todayMatches;

        // ゲッター・セッター
        public long getActiveMatches() {
            return activeMatches;
        }

        public void setActiveMatches(long activeMatches) {
            this.activeMatches = activeMatches;
        }

        public Double getAverageDuration() {
            return averageDuration;
        }

        public void setAverageDuration(Double averageDuration) {
            this.averageDuration = averageDuration;
        }

        public Double getAverageMessageCount() {
            return averageMessageCount;
        }

        public void setAverageMessageCount(Double averageMessageCount) {
            this.averageMessageCount = averageMessageCount;
        }

        public long getTodayMatches() {
            return todayMatches;
        }

        public void setTodayMatches(long todayMatches) {
            this.todayMatches = todayMatches;
        }
    }

    /**
     * グループ化されたマッチ履歴クラス
     */
    public static class GroupedMatchHistory {
        private User otherUser;
        private int totalMatches;
        private RandomMatch latestMatch;
        private int totalMessages;
        private long activeMatches;

        public GroupedMatchHistory(User otherUser, int totalMatches, RandomMatch latestMatch, 
                                 int totalMessages, long activeMatches) {
            this.otherUser = otherUser;
            this.totalMatches = totalMatches;
            this.latestMatch = latestMatch;
            this.totalMessages = totalMessages;
            this.activeMatches = activeMatches;
        }

        // ゲッター・セッター
        public User getOtherUser() {
            return otherUser;
        }

        public void setOtherUser(User otherUser) {
            this.otherUser = otherUser;
        }

        public int getTotalMatches() {
            return totalMatches;
        }

        public void setTotalMatches(int totalMatches) {
            this.totalMatches = totalMatches;
        }

        public RandomMatch getLatestMatch() {
            return latestMatch;
        }

        public void setLatestMatch(RandomMatch latestMatch) {
            this.latestMatch = latestMatch;
        }

        public int getTotalMessages() {
            return totalMessages;
        }

        public void setTotalMessages(int totalMessages) {
            this.totalMessages = totalMessages;
        }

        public long getActiveMatches() {
            return activeMatches;
        }

        public void setActiveMatches(long activeMatches) {
            this.activeMatches = activeMatches;
        }

        // 表示用のヘルパーメソッド
        public String getDisplayName() {
            return otherUser.getUsername(); // シンプルにusernameを使用
        }

        public String getStatusDisplay() {
            if (activeMatches > 0) {
                return "アクティブ (" + activeMatches + ")";
            } else {
                return latestMatch.getStatus().getDisplayName();
            }
        }
    }
}
