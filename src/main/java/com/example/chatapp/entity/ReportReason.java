package com.example.chatapp.entity;

public enum ReportReason {
    SPAM("スパム・迷惑行為"),
    HARASSMENT("ハラスメント・嫌がらせ"),
    INAPPROPRIATE_CONTENT("不適切なコンテンツ"),
    HATE_SPEECH("ヘイトスピーチ"),
    IMPERSONATION("なりすまし"),
    VIOLENCE_THREAT("暴力的な脅迫"),
    PRIVACY_VIOLATION("プライバシー侵害"),
    FRAUD("詐欺・不正行為"),
    OTHER("その他");

    private final String description;

    ReportReason(String description) {
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
