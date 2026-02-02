package com.example.chatapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * マッチング「いいね」を管理するエンティティ
 */
@Entity
@Table(name = "match_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"liker_id", "liked_id"})
})
public class MatchLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liker_id", nullable = false)
    private User liker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liked_id", nullable = false)
    private User liked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MatchLike() {
        this.createdAt = LocalDateTime.now();
    }

    public MatchLike(User liker, User liked) {
        this();
        this.liker = liker;
        this.liked = liked;
    }

    // ゲッター・セッター
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getLiker() { return liker; }
    public void setLiker(User liker) { this.liker = liker; }

    public User getLiked() { return liked; }
    public void setLiked(User liked) { this.liked = liked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
