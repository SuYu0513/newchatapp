package com.example.chatapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notifications")
public class AdminNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_user_id")
    private User relatedUser;  // 関連するユーザー（通報されたユーザーなど）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_report_id")
    private Report relatedReport;  // 関連する通報

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "read_by")
    private User readBy;  // 読んだ管理者

    // コンストラクタ
    public AdminNotification() {
        this.createdAt = LocalDateTime.now();
    }

    public AdminNotification(NotificationType type, String title, String message) {
        this();
        this.type = type;
        this.title = title;
        this.message = message;
    }

    public AdminNotification(NotificationType type, String title, String message, User relatedUser, Report relatedReport) {
        this(type, title, message);
        this.relatedUser = relatedUser;
        this.relatedReport = relatedReport;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getRelatedUser() {
        return relatedUser;
    }

    public void setRelatedUser(User relatedUser) {
        this.relatedUser = relatedUser;
    }

    public Report getRelatedReport() {
        return relatedReport;
    }

    public void setRelatedReport(Report relatedReport) {
        this.relatedReport = relatedReport;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public User getReadBy() {
        return readBy;
    }

    public void setReadBy(User readBy) {
        this.readBy = readBy;
    }

    // ヘルパーメソッド
    public void markAsRead(User admin) {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        this.readBy = admin;
    }

    @Override
    public String toString() {
        return "AdminNotification{" +
                "id=" + id +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}
