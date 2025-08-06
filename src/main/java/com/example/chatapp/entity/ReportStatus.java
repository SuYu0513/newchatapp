package com.example.chatapp.entity;

public enum ReportStatus {
    PENDING("審査中"),
    REVIEWED("審査済み"),
    RESOLVED("解決済み"),
    REJECTED("却下"),
    ESCALATED("エスカレーション");

    private final String description;

    ReportStatus(String description) {
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
