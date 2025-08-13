package com.example.chatapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ランダムマッチングを管理するエンティティ
 */
@Entity
@Table(name = "random_matches")
public class RandomMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MatchStatus status = MatchStatus.ACTIVE;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ended_by_user_id")
    private User endedByUser;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "message_count")
    private Integer messageCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // コンストラクタ
    public RandomMatch() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.matchedAt = LocalDateTime.now();
    }

    public RandomMatch(User user1, User user2, ChatRoom chatRoom) {
        this();
        this.user1 = user1;
        this.user2 = user2;
        this.chatRoom = chatRoom;
    }
    
    // ルームなしでマッチングを作成（後でルームを設定）
    public RandomMatch(User user1, User user2) {
        this();
        this.user1 = user1;
        this.user2 = user2;
        // chatRoomは後で設定
    }

    // ゲッター・セッター
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        
        if (status == MatchStatus.ENDED || status == MatchStatus.ABANDONED) {
            this.endedAt = LocalDateTime.now();
            this.calculateDuration();
        }
    }

    public LocalDateTime getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(LocalDateTime matchedAt) {
        this.matchedAt = matchedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public User getEndedByUser() {
        return endedByUser;
    }

    public void setEndedByUser(User endedByUser) {
        this.endedByUser = endedByUser;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // マッチステータスの列挙型
    public enum MatchStatus {
        ACTIVE("アクティブ"),
        ENDED("終了"),
        ABANDONED("放棄"),
        TIMEOUT("タイムアウト");

        private final String displayName;

        MatchStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ヘルパーメソッド
    public User getOtherUser(User currentUser) {
        if (user1.equals(currentUser)) {
            return user2;
        } else if (user2.equals(currentUser)) {
            return user1;
        }
        return null;
    }

    public boolean involvesUser(User user) {
        return user1.equals(user) || user2.equals(user);
    }

    public boolean isActive() {
        return status == MatchStatus.ACTIVE;
    }

    public void incrementMessageCount() {
        this.messageCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void endMatch(User endedBy) {
        this.status = MatchStatus.ENDED;
        this.endedByUser = endedBy;
        this.endedAt = LocalDateTime.now();
        this.calculateDuration();
    }

    private void calculateDuration() {
        if (matchedAt != null && endedAt != null) {
            long minutes = java.time.Duration.between(matchedAt, endedAt).toMinutes();
            this.durationMinutes = (int) minutes;
        }
    }
}
