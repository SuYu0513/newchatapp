package com.example.chatapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "favorite_tags")
public class FavoriteTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String tagName;
    
    @Column(nullable = false)
    private Integer usageCount = 0;
    
    // デフォルトコンストラクタ
    public FavoriteTag() {}
    
    public FavoriteTag(String tagName) {
        this.tagName = tagName;
        this.usageCount = 1;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTagName() {
        return tagName;
    }
    
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
    
    public Integer getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
}
