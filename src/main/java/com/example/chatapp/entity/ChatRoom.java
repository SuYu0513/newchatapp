package com.example.chatapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Size(max = 100, message = "ルーム名は100文字以下で入力してください")
    @Column(length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @NotNull(message = "チャットルームのタイプは必須です")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ChatRoomType type;
    
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true; // デフォルトはパブリック
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    private User createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> messages = new HashSet<>();
    
    @ManyToMany
    @JoinTable(
        name = "chat_room_users",
        joinColumns = @JoinColumn(name = "chat_room_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();
    
    // チャットルームタイプの列挙型
    public enum ChatRoomType {
        PRIVATE,    // 1対1チャット
        GROUP,      // グループチャット
        RANDOM      // ランダムマッチ
    }
    
    // デフォルトコンストラクタ
    public ChatRoom() {}
    
    // コンストラクタ
    public ChatRoom(String name, ChatRoomType type, User createdBy) {
        this.name = name;
        this.type = type;
        this.createdBy = createdBy;
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

    public ChatRoomType getType() {
        return type;
    }
    
    public void setType(ChatRoomType type) {
        this.type = type;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Set<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(Set<Message> messages) {
        this.messages = messages;
    }
    
    public Set<User> getUsers() {
        return users;
    }
    
    public void setUsers(Set<User> users) {
        this.users = users;
    }
    
    // ユーザーをチャットルームに追加
    public void addUser(User user) {
        this.users.add(user);
        user.getChatRooms().add(this);
    }
    
    // ユーザーをチャットルームから削除
    public void removeUser(User user) {
        this.users.remove(user);
        user.getChatRooms().remove(this);
    }
}
