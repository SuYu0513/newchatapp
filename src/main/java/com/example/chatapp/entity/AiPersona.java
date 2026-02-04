package com.example.chatapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ai_personas")
public class AiPersona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // 関係性: friend(友達), lover(恋人), senior(先輩), junior(後輩), teacher(先生), family(家族)
    @Column(length = 20)
    private String relationship;

    // 性別: male(男性), female(女性), neutral(中性)
    @Column(length = 20)
    private String gender;

    // 年齢層: teen(10代), twenties(20代), thirties(30代), forties(40代以上)
    @Column(length = 20)
    private String ageGroup;

    // 性格: cheerful(明るい), calm(落ち着いた), cool(クール), tsundere(ツンデレ), gentle(優しい), playful(おちゃめ)
    @Column(length = 20)
    private String personality;

    // 口調: casual(カジュアル), polite(丁寧), formal(フォーマル), dialect(方言)
    @Column(length = 20)
    private String speakingStyle;

    // AIの名前（ユーザーが設定可能）
    @Column(length = 50)
    private String aiName;

    // アバターURL
    @Column(length = 500)
    private String avatarUrl;

    // セットアップ完了フラグ
    private boolean setupCompleted = false;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }

    public String getSpeakingStyle() { return speakingStyle; }
    public void setSpeakingStyle(String speakingStyle) { this.speakingStyle = speakingStyle; }

    public String getAiName() { return aiName; }
    public void setAiName(String aiName) { this.aiName = aiName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isSetupCompleted() { return setupCompleted; }
    public void setSetupCompleted(boolean setupCompleted) { this.setupCompleted = setupCompleted; }
}
