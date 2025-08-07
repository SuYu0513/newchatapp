package com.example.chatapp.service;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RandomMatch;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.repository.RandomMatchRepository;
import com.example.chatapp.repository.UserProfileRepository;
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

    private final Random random = new Random();
    private static final int MATCH_TIMEOUT_MINUTES = 30;

    /**
     * 最適なマッチング相手を見つける（相性重視）
     */
    public User findBestMatch(User user) {
        UserProfile userProfile = userProfileService.getOrCreateProfile(user);
        
        // ランダムマッチング許可済みで、現在アクティブでないユーザーを取得
        List<UserProfile> candidates = userProfileService.getAvailableForRandomMatching(user);
        
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

        // 音楽ジャンルの一致度
        score += calculateTextSimilarity(profile1.getMusicGenres(), profile2.getMusicGenres()) * 10.0;
        factors++;

        // 映画ジャンルの一致度
        score += calculateTextSimilarity(profile1.getMovieGenres(), profile2.getMovieGenres()) * 10.0;
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
     * マッチングを作成
     */
    public RandomMatch createMatch(User user1, User user2) {
        // 既存のアクティブマッチをチェック
        Optional<RandomMatch> existingMatch1 = randomMatchRepository.findActiveMatchForUser(user1);
        Optional<RandomMatch> existingMatch2 = randomMatchRepository.findActiveMatchForUser(user2);

        if (existingMatch1.isPresent() || existingMatch2.isPresent()) {
            throw new IllegalStateException("既にアクティブなマッチが存在します");
        }

        // チャットルームを作成
        String roomName = "ランダムチャット - " + user1.getUsername() + " & " + user2.getUsername();
        ChatRoom chatRoom = chatRoomService.createPrivateRoom(roomName, user1);
        
        // 相手をルームに追加
        chatRoomService.addUserToRoom(chatRoom.getId(), user2);

        // ランダムマッチを作成
        RandomMatch randomMatch = new RandomMatch(user1, user2, chatRoom);
        return randomMatchRepository.save(randomMatch);
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
        // 既にアクティブなマッチがないかチェック
        Optional<RandomMatch> existingMatch = randomMatchRepository.findActiveMatchForUser(user);
        if (existingMatch.isPresent()) {
            return existingMatch;
        }

        // マッチング可能なユーザーを取得
        List<UserProfile> availableProfiles = userProfileService.getAvailableForRandomMatching(user);
        
        if (availableProfiles.isEmpty()) {
            return Optional.empty();
        }

        // ランダムに相手を選択
        UserProfile matchedProfile = availableProfiles.get(random.nextInt(availableProfiles.size()));
        User matchedUser = matchedProfile.getUser();

        // 相手が既にマッチング中でないかチェック
        Optional<RandomMatch> partnerMatch = randomMatchRepository.findActiveMatchForUser(matchedUser);
        if (partnerMatch.isPresent()) {
            // 相手が既にマッチング中の場合、再帰的に他の相手を探す
            availableProfiles.remove(matchedProfile);
            if (availableProfiles.isEmpty()) {
                return Optional.empty();
            }
            return findRandomMatch(user);
        }

        // チャットルームを作成
        String roomName = "ランダムチャット - " + user.getUsername() + " & " + matchedUser.getUsername();
        ChatRoom chatRoom = chatRoomService.createPrivateRoom(roomName, user);
        
        // 相手をルームに追加
        chatRoomService.addUserToRoom(chatRoom.getId(), matchedUser);

        // ランダムマッチを作成
        RandomMatch randomMatch = new RandomMatch(user, matchedUser, chatRoom);
        return Optional.of(randomMatchRepository.save(randomMatch));
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
        // 既にアクティブなマッチがないかチェック
        Optional<RandomMatch> existingMatch = randomMatchRepository.findActiveMatchForUser(user);
        if (existingMatch.isPresent()) {
            return false;
        }

        // プロフィール設定をチェック
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
}
