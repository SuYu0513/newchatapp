package com.example.chatapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * フォロー関係を管理するエンティティ
 * follower → following の単方向フォロー関係を表現
 * 相互フォローの場合は2つのレコードが存在する
 */
@Entity
@Table(name = "friendships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"follower_id", "following_id"})
})
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * フォローしている人（自分）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    /**
     * フォローされている人（相手）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    /**
     * フォロー状態（PENDING, ACCEPTED, BLOCKEDなど）
     * 現在の実装ではシンプルにACCEPTEDのみ使用
     */
    @Column(name = "status", nullable = false)
    private String status = "ACCEPTED";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // コンストラクタ
    public Friendship() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Friendship(User follower, User following) {
        this();
        this.follower = follower;
        this.following = following;
    }

    // ゲッター・セッター
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getFollower() {
        return follower;
    }

    public void setFollower(User follower) {
        this.follower = follower;
    }

    public User getFollowing() {
        return following;
    }

    public void setFollowing(User following) {
        this.following = following;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
    
    // 後方互換性のためのメソッド（既存コードとの互換性）
    @Deprecated
    public User getRequester() {
        return follower;
    }
    
    @Deprecated
    public User getAddressee() {
        return following;
    }
}
