package com.example.chatapp.service;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RandomMatch;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.repository.RandomMatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
