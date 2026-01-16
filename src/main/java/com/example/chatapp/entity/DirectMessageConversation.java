package com.example.chatapp.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * DM会話エンティティ - 2人のユーザー間のDM会話を管理
 */
@Entity
@Table(name = "direct_message_conversations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}))
public class DirectMessageConversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;
    
    @Column(name = "last_message_content", columnDefinition = "TEXT")
    private String lastMessageContent;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_sender_id")
    private User lastMessageSender;
    
    // デフォルトコンストラクタ
    public DirectMessageConversation() {}
    
    // コンストラクタ
    public DirectMessageConversation(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
    }
    
    // Getters and Setters
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }
    
    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
    
    public String getLastMessageContent() {
        return lastMessageContent;
    }
    
    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }
    
    public User getLastMessageSender() {
        return lastMessageSender;
    }
    
    public void setLastMessageSender(User lastMessageSender) {
        this.lastMessageSender = lastMessageSender;
    }
    
    /**
     * 指定したユーザーがこの会話に含まれているか確認
     */
    public boolean containsUser(User user) {
        return (user1 != null && user1.getId().equals(user.getId())) || 
               (user2 != null && user2.getId().equals(user.getId()));
    }
    
    /**
     * 指定したユーザーに対する相手ユーザーを取得
     */
    public User getOtherUser(User user) {
        if (user == null) {
            return null;
        }
        if (user1 != null && user1.getId().equals(user.getId())) {
            return user2;
        } else if (user2 != null && user2.getId().equals(user.getId())) {
            return user1;
        }
        return null;
    }
}
