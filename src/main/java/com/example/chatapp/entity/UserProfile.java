package com.example.chatapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ユーザープロフィール情報を管理するエンティティ
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "display_name", length = 50)
    private String displayName;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "status", length = 100)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status")
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "birth_date")
    private LocalDateTime birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_level")
    private PrivacyLevel privacyLevel = PrivacyLevel.PUBLIC;

    @Column(name = "is_searchable")
    private Boolean isSearchable = true;

    @Column(name = "allow_random_matching")
    private Boolean allowRandomMatching = true;

    // ランダムマッチング用の情報
    @Column(name = "interests", columnDefinition = "TEXT")
    private String interests; // カンマ区切りの興味・趣味リスト

    @Column(name = "hobbies", columnDefinition = "TEXT")
    private String hobbies; // カンマ区切りの趣味リスト

    @Column(name = "favorite_things", columnDefinition = "TEXT")
    private String favoriteThings; // カンマ区切りの好きなものリスト

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group")
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_style")
    private ChatStyle chatStyle;

    @Column(name = "music_genres", columnDefinition = "TEXT")
    private String musicGenres; // カンマ区切りの音楽ジャンル

    @Column(name = "movie_genres", columnDefinition = "TEXT")
    private String movieGenres; // カンマ区切りの映画ジャンル

    @Column(name = "languages", columnDefinition = "TEXT")
    private String languages; // カンマ区切りの話せる言語

    @Column(name = "personality_traits", columnDefinition = "TEXT")
    private String personalityTraits; // カンマ区切りの性格特性

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // コンストラクタ
    public UserProfile() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    public UserProfile(User user) {
        this();
        this.user = user;
        this.displayName = user.getUsername();
    }

    // ゲッター・セッター
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OnlineStatus getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(OnlineStatus onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDateTime birthDate) {
        this.birthDate = birthDate;
    }

    public PrivacyLevel getPrivacyLevel() {
        return privacyLevel;
    }

    public void setPrivacyLevel(PrivacyLevel privacyLevel) {
        this.privacyLevel = privacyLevel;
    }

    public Boolean getIsSearchable() {
        return isSearchable;
    }

    public void setIsSearchable(Boolean isSearchable) {
        this.isSearchable = isSearchable;
    }

    public Boolean getAllowRandomMatching() {
        return allowRandomMatching;
    }

    public void setAllowRandomMatching(Boolean allowRandomMatching) {
        this.allowRandomMatching = allowRandomMatching;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getHobbies() {
        return hobbies;
    }

    public void setHobbies(String hobbies) {
        this.hobbies = hobbies;
    }

    public String getFavoriteThings() {
        return favoriteThings;
    }

    public void setFavoriteThings(String favoriteThings) {
        this.favoriteThings = favoriteThings;
    }

    public AgeGroup getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(AgeGroup ageGroup) {
        this.ageGroup = ageGroup;
    }

    public ChatStyle getChatStyle() {
        return chatStyle;
    }

    public void setChatStyle(ChatStyle chatStyle) {
        this.chatStyle = chatStyle;
    }

    public String getMusicGenres() {
        return musicGenres;
    }

    public void setMusicGenres(String musicGenres) {
        this.musicGenres = musicGenres;
    }

    public String getMovieGenres() {
        return movieGenres;
    }

    public void setMovieGenres(String movieGenres) {
        this.movieGenres = movieGenres;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public String getPersonalityTraits() {
        return personalityTraits;
    }

    public void setPersonalityTraits(String personalityTraits) {
        this.personalityTraits = personalityTraits;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
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

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // オンラインステータスの列挙型
    public enum OnlineStatus {
        ONLINE("オンライン"),
        AWAY("離席中"),
        BUSY("取り込み中"),
        OFFLINE("オフライン");

        private final String displayName;

        OnlineStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // プライバシーレベルの列挙型
    public enum PrivacyLevel {
        PUBLIC("公開"),
        FRIENDS_ONLY("フレンドのみ"),
        PRIVATE("非公開");

        private final String displayName;

        PrivacyLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 年齢層の列挙型
    public enum AgeGroup {
        TEENS("10代"),
        TWENTIES("20代"),
        THIRTIES("30代"),
        FORTIES("40代"),
        FIFTIES_PLUS("50代以上"),
        NOT_SPECIFIED("未設定");

        private final String displayName;

        AgeGroup(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // チャットスタイルの列挙型
    public enum ChatStyle {
        CASUAL("カジュアル"),
        SERIOUS("真面目"),
        FUNNY("面白い"),
        DEEP("深い話"),
        LIGHT("軽い話"),
        CREATIVE("創造的"),
        NOT_SPECIFIED("未設定");

        private final String displayName;

        ChatStyle(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ヘルパーメソッド
    public String getAvatarUrlOrDefault() {
        return avatarUrl != null && !avatarUrl.isEmpty() 
            ? avatarUrl 
            : "/images/default-avatar.svg";
    }

    public String getDisplayNameOrUsername() {
        return displayName != null && !displayName.isEmpty() 
            ? displayName 
            : (user != null ? user.getUsername() : "Unknown User");
    }

    public boolean isOnline() {
        return onlineStatus == OnlineStatus.ONLINE;
    }

    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
