package com.example.chatapp.entity;

public enum NotificationType {
    REPORT("通報"),
    MULTIPLE_REPORTS("複数通報"),
    USER_SUSPENDED("ユーザー停止"),
    SYSTEM_ALERT("システム警告"),
    FRIENDSHIP_ISSUE("フレンド関係の問題"),
    SPAM_DETECTED("スパム検出"),
    CONTENT_VIOLATION("コンテンツ違反");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
