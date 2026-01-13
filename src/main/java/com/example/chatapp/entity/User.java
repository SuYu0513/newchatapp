package com.example.chatapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "ユーザー名は必須です")
    @Size(min = 1, max = 50, message = "ユーザー名は1文字以上50文字以下で入力してください")
    @Column(nullable = false, length = 50)
    private String username;
    
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 6, message = "パスワードは6文字以上で入力してください")
    @Column(nullable = false)
    private String password;
    
    @Email(message = "有効なメールアドレスを入力してください")
    @NotBlank(message = "メールアドレスは必須です")
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(unique = true, nullable = false, name = "friend_code")
    private Integer friendCode;  // 8桁のフレンドコード
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> messages = new HashSet<>();
    
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatRoom> createdChatRooms = new HashSet<>();
    
    @ManyToMany(mappedBy = "users")
    private Set<ChatRoom> chatRooms = new HashSet<>();
    
    // デフォルトコンストラクタ
    public User() {}
    
    // コンストラクタ
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Integer getFriendCode() {
        return friendCode;
    }
    
    public void setFriendCode(Integer friendCode) {
        this.friendCode = friendCode;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Set<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(Set<Message> messages) {
        this.messages = messages;
    }
    
    public Set<ChatRoom> getCreatedChatRooms() {
        return createdChatRooms;
    }
    
    public void setCreatedChatRooms(Set<ChatRoom> createdChatRooms) {
        this.createdChatRooms = createdChatRooms;
    }
    
    public Set<ChatRoom> getChatRooms() {
        return chatRooms;
    }
    
    public void setChatRooms(Set<ChatRoom> chatRooms) {
        this.chatRooms = chatRooms;
    }
}
