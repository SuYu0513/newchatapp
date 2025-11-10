package com.example.chatapp.repository;

import com.example.chatapp.entity.RandomMatch;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RandomMatchRepository extends JpaRepository<RandomMatch, Long> {

    @Query("SELECT rm FROM RandomMatch rm WHERE (rm.user1 = :user OR rm.user2 = :user) AND rm.status = 'ACTIVE'")
    Optional<RandomMatch> findActiveMatchForUser(@Param("user") User user);

    @Query("SELECT rm FROM RandomMatch rm WHERE (rm.user1 = :user OR rm.user2 = :user)")
    List<RandomMatch> findAllMatchesForUser(@Param("user") User user);

    @Query("SELECT rm FROM RandomMatch rm WHERE rm.status = 'ACTIVE'")
    List<RandomMatch> findActiveMatches();

    @Query("SELECT COUNT(rm) FROM RandomMatch rm WHERE rm.status = 'ACTIVE'")
    long countActiveMatches();

    @Query("SELECT rm FROM RandomMatch rm WHERE " +
           "(rm.user1 = :user OR rm.user2 = :user) AND " +
           "rm.status = 'ACTIVE' AND " +
           "rm.matchedAt > :since")
    List<RandomMatch> findRecentActiveMatchesForUser(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT rm FROM RandomMatch rm WHERE " +
           "rm.matchedAt < :timeout AND " +
           "rm.status = 'ACTIVE' AND " +
           "rm.messageCount = 0")
    List<RandomMatch> findTimedOutMatches(@Param("timeout") LocalDateTime timeout);

    @Query("SELECT rm FROM RandomMatch rm WHERE " +
           "(rm.user1 = :user OR rm.user2 = :user) AND " +
           "rm.status IN ('ENDED', 'ABANDONED', 'TIMEOUT') " +
           "ORDER BY rm.endedAt DESC")
    List<RandomMatch> findCompletedMatchesForUser(@Param("user") User user);

    @Query("SELECT rm FROM RandomMatch rm WHERE rm.chatRoom.id = :chatRoomId")
    Optional<RandomMatch> findByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    // 二人のユーザー間の既存マッチをチェック
    @Query("SELECT rm FROM RandomMatch rm WHERE " +
           "((rm.user1 = :user1 AND rm.user2 = :user2) OR (rm.user1 = :user2 AND rm.user2 = :user1))")
    List<RandomMatch> findByUser1AndUser2OrUser2AndUser1(@Param("user1") User user1, @Param("user2") User user2);

    // 統計用クエリ
    @Query("SELECT AVG(rm.durationMinutes) FROM RandomMatch rm WHERE rm.durationMinutes IS NOT NULL")
    Double getAverageMatchDuration();

    @Query("SELECT AVG(rm.messageCount) FROM RandomMatch rm WHERE rm.messageCount > 0")
    Double getAverageMessageCount();

    @Query("SELECT COUNT(rm) FROM RandomMatch rm WHERE rm.matchedAt >= :startDate AND rm.matchedAt < :endDate")
    long countMatchesBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
