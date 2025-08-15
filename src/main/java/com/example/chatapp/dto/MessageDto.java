package com.example.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class MessageDto {
    
    @NotBlank(message = "メッセージ内容は必須です")
    private String content;
    
    private Long chatRoomId;
    private String senderUsername;
    private Long userId; // ユーザーID追加
    private String senderAvatarUrl; // 送信者のアバターURL
    private String senderDisplayName; // 送信者の表示名
    private String timestamp;
    private String type; // JOIN, LEAVE, CHAT
    
    // デフォルトコンストラクタ
    public MessageDto() {}
    
    // コンストラクタ
    public MessageDto(String content, Long chatRoomId, String senderUsername, String timestamp) {
        this.content = content;
        this.chatRoomId = chatRoomId;
        this.senderUsername = senderUsername;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Long getChatRoomId() {
        return chatRoomId;
    }
    
    public void setChatRoomId(Long chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
    
    public String getSenderUsername() {
        return senderUsername;
    }
    
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    // senderの別名メソッドを追加
    public String getSender() {
        return senderUsername;
    }
    
    public void setSender(String sender) {
        this.senderUsername = sender;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    // LocalDateTimeを受け取るsetTimestampメソッドを追加
    public void setTimestamp(LocalDateTime timestamp) {
        if (timestamp != null) {
            this.timestamp = timestamp.toString();
        }
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }
    
    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }
    
    public String getSenderDisplayName() {
        return senderDisplayName;
    }
    
    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }
}
