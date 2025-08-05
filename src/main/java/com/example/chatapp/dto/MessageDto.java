package com.example.chatapp.dto;

import jakarta.validation.constraints.NotBlank;

public class MessageDto {
    
    @NotBlank(message = "メッセージ内容は必須です")
    private String content;
    
    private Long chatRoomId;
    private String senderUsername;
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
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
}
