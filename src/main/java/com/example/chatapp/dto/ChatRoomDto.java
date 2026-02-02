package com.example.chatapp.dto;

import com.example.chatapp.entity.ChatRoom;

import java.time.LocalDateTime;

/**
 * チャットルーム情報を転送するためのDTO
 * エンティティの循環参照を避けるために使用
 */
public class ChatRoomDto {
    private Long id;
    private String name;
    private String description;
    private String type;
    private boolean isPublic;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private int memberCount;
    private String iconUrl;

    // デフォルトコンストラクタ
    public ChatRoomDto() {}

    // ChatRoomエンティティから生成するコンストラクタ
    public ChatRoomDto(ChatRoom chatRoom) {
        this.id = chatRoom.getId();
        this.name = chatRoom.getName();
        this.description = chatRoom.getDescription();
        this.type = chatRoom.getType() != null ? chatRoom.getType().name() : null;
        this.isPublic = chatRoom.isPublic();
        this.createdByUsername = chatRoom.getCreatedBy() != null ? chatRoom.getCreatedBy().getUsername() : null;
        this.createdAt = chatRoom.getCreatedAt();
        // 最終アクティビティ時間は、最新メッセージのタイムスタンプまたは作成時間
        this.lastActivityAt = chatRoom.getCreatedAt(); // 仮設定：後でメッセージから取得
        this.memberCount = chatRoom.getUsers() != null ? chatRoom.getUsers().size() : 0;
        this.iconUrl = chatRoom.getIconUrl();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
