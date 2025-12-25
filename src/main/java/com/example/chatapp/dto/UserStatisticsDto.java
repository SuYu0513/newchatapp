package com.example.chatapp.dto;

import java.time.LocalDateTime;

/**
 * ユーザー統計情報を格納するDTOクラス
 */
public class UserStatisticsDto {
    
    private Long userId;
    private String username;
    private int friendCount;           // フレンド数
    private int totalChatMessages;     // 送信したチャットメッセージ数
    private int totalChatRooms;        // 参加しているチャットルーム数
    private LocalDateTime lastActiveDate; // 最終活動日時
    private int daysActive;            // アクティブ日数
    private LocalDateTime joinDate;    // 参加日
    
    // デフォルトコンストラクタ
    public UserStatisticsDto() {}
    
    // 全項目コンストラクタ
    public UserStatisticsDto(Long userId, String username, int friendCount, 
                           int totalChatMessages, int totalChatRooms, 
                           LocalDateTime lastActiveDate, int daysActive, 
                           LocalDateTime joinDate) {
        this.userId = userId;
        this.username = username;
        this.friendCount = friendCount;
        this.totalChatMessages = totalChatMessages;
        this.totalChatRooms = totalChatRooms;
        this.lastActiveDate = lastActiveDate;
        this.daysActive = daysActive;
        this.joinDate = joinDate;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public int getFriendCount() {
        return friendCount;
    }
    
    public void setFriendCount(int friendCount) {
        this.friendCount = friendCount;
    }
    
    public int getTotalChatMessages() {
        return totalChatMessages;
    }
    
    public void setTotalChatMessages(int totalChatMessages) {
        this.totalChatMessages = totalChatMessages;
    }
    
    public int getTotalChatRooms() {
        return totalChatRooms;
    }
    
    public void setTotalChatRooms(int totalChatRooms) {
        this.totalChatRooms = totalChatRooms;
    }
    
    public LocalDateTime getLastActiveDate() {
        return lastActiveDate;
    }
    
    public void setLastActiveDate(LocalDateTime lastActiveDate) {
        this.lastActiveDate = lastActiveDate;
    }
    
    public int getDaysActive() {
        return daysActive;
    }
    
    public void setDaysActive(int daysActive) {
        this.daysActive = daysActive;
    }
    
    public LocalDateTime getJoinDate() {
        return joinDate;
    }
    
    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }
    
    @Override
    public String toString() {
        return "UserStatisticsDto{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", friendCount=" + friendCount +
                ", totalChatMessages=" + totalChatMessages +
                ", totalChatRooms=" + totalChatRooms +
                ", lastActiveDate=" + lastActiveDate +
                ", daysActive=" + daysActive +
                ", joinDate=" + joinDate +
                '}';
    }
}