package com.example.chatapp.service;

import com.example.chatapp.dto.UserStatisticsDto;
import com.example.chatapp.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * ユーザー統計情報処理サービス
 */
@Service
public class UserStatisticsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 指定されたユーザーの統計情報を取得
     * @param userId ユーザーID
     * @return 統計情報DTO（存在しない場合はEmpty）
     */
    public Optional<UserStatisticsDto> getUserStatistics(Long userId) {
        try {
            String sql = """
                SELECT 
                    u.id as user_id,
                    u.username,
                    u.created_at as join_date,
                    u.last_seen,
                    COUNT(DISTINCT f.id) as friend_count,
                    COALESCE(msg_stats.total_messages, 0) as total_messages,
                    COALESCE(room_stats.total_rooms, 0) as total_rooms
                FROM users u
                LEFT JOIN friendships f ON (f.user_id = u.id OR f.friend_id = u.id) 
                    AND f.status = 'ACCEPTED'
                LEFT JOIN (
                    SELECT 
                        sender_id,
                        COUNT(*) as total_messages
                    FROM chat_messages 
                    WHERE sender_id = ?
                    GROUP BY sender_id
                ) msg_stats ON msg_stats.sender_id = u.id
                LEFT JOIN (
                    SELECT 
                        user_id,
                        COUNT(DISTINCT room_id) as total_rooms
                    FROM chat_room_members 
                    WHERE user_id = ?
                    GROUP BY user_id
                ) room_stats ON room_stats.user_id = u.id
                WHERE u.id = ?
                GROUP BY u.id, u.username, u.created_at, u.last_seen, 
                         msg_stats.total_messages, room_stats.total_rooms
                """;

            return jdbcTemplate.query(sql, 
                    (rs, rowNum) -> {
                        UserStatisticsDto stats = new UserStatisticsDto();
                        stats.setUserId(rs.getLong("user_id"));
                        stats.setUsername(rs.getString("username"));
                        stats.setFriendCount(rs.getInt("friend_count"));
                        stats.setTotalChatMessages(rs.getInt("total_messages"));
                        stats.setTotalChatRooms(rs.getInt("total_rooms"));
                        
                        // 参加日時の設定
                        if (rs.getTimestamp("join_date") != null) {
                            stats.setJoinDate(rs.getTimestamp("join_date").toLocalDateTime());
                        }
                        
                        // 最終活動日時の設定
                        if (rs.getTimestamp("last_seen") != null) {
                            LocalDateTime lastSeen = rs.getTimestamp("last_seen").toLocalDateTime();
                            stats.setLastActiveDate(lastSeen);
                            
                            // アクティブ日数を計算（参加日から最終活動日まで）
                            if (stats.getJoinDate() != null) {
                                long daysBetween = ChronoUnit.DAYS.between(
                                    stats.getJoinDate().toLocalDate(), 
                                    lastSeen.toLocalDate()
                                ) + 1; // +1は初日を含む
                                stats.setDaysActive((int) daysBetween);
                            }
                        } else {
                            // last_seenがnullの場合は、参加日のみでアクティブ日数を1に設定
                            stats.setDaysActive(1);
                        }
                        
                        return stats;
                    }, userId, userId, userId)
                    .stream()
                    .findFirst();
                    
        } catch (Exception e) {
            // ログに出力してEmptyを返す
            System.err.println("統計情報取得エラー (UserID: " + userId + "): " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 基本的な統計情報を取得（データベースエラー時のフォールバック）
     * @param user ユーザーエンティティ
     * @return 基本統計情報DTO
     */
    public UserStatisticsDto getBasicStatistics(User user) {
        UserStatisticsDto stats = new UserStatisticsDto();
        stats.setUserId(user.getId());
        stats.setUsername(user.getUsername());
        stats.setFriendCount(0);
        stats.setTotalChatMessages(0);
        stats.setTotalChatRooms(0);
        stats.setJoinDate(user.getCreatedAt());
        stats.setLastActiveDate(null);
        
        // アクティブ日数の計算
        if (user.getCreatedAt() != null) {
            LocalDateTime endDate = LocalDateTime.now();
            long daysBetween = ChronoUnit.DAYS.between(
                user.getCreatedAt().toLocalDate(), 
                endDate.toLocalDate()
            ) + 1;
            stats.setDaysActive((int) daysBetween);
        } else {
            stats.setDaysActive(1);
        }
        
        return stats;
    }

    /**
     * 統計情報を更新（ユーザーアクティビティ時に呼び出し）
     * @param userId ユーザーID
     */
    public void updateUserActivity(Long userId) {
        try {
            String sql = "UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE id = ?";
            jdbcTemplate.update(sql, userId);
        } catch (Exception e) {
            System.err.println("ユーザーアクティビティ更新エラー (UserID: " + userId + "): " + e.getMessage());
        }
    }

    /**
     * メッセージ送信時の統計更新
     * @param userId ユーザーID
     */
    public void incrementMessageCount(Long userId) {
        // メッセージ送信時にlast_seenも更新
        updateUserActivity(userId);
    }

    /**
     * チャットルーム参加時の統計更新
     * @param userId ユーザーID
     */
    public void updateRoomParticipation(Long userId) {
        // ルーム参加時にlast_seenも更新
        updateUserActivity(userId);
    }
}