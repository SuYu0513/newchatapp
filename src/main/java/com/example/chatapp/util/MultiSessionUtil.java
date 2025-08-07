package com.example.chatapp.util;

import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class MultiSessionUtil {
    
    // タブID -> セッション情報のマップ
    private final Map<String, TabSession> tabSessions = new ConcurrentHashMap<>();
    
    /**
     * タブセッション情報
     */
    public static class TabSession {
        private String tabId;
        private String sessionId;
        private String username;
        private long lastAccessed;
        
        public TabSession(String tabId, String sessionId) {
            this.tabId = tabId;
            this.sessionId = sessionId;
            this.lastAccessed = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getTabId() { return tabId; }
        public void setTabId(String tabId) { this.tabId = tabId; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public long getLastAccessed() { return lastAccessed; }
        public void setLastAccessed(long lastAccessed) { this.lastAccessed = lastAccessed; }
        
        public void updateLastAccessed() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }
    
    /**
     * 新しいタブセッションを作成
     */
    public String createTabSession(HttpServletRequest request) {
        String tabId = "tab-" + UUID.randomUUID().toString().substring(0, 8);
        String sessionId = request.getSession(true).getId();
        
        TabSession tabSession = new TabSession(tabId, sessionId);
        tabSessions.put(tabId, tabSession);
        
        return tabId;
    }
    
    /**
     * タブセッションを取得
     */
    public TabSession getTabSession(String tabId) {
        TabSession session = tabSessions.get(tabId);
        if (session != null) {
            session.updateLastAccessed();
        }
        return session;
    }
    
    /**
     * タブセッションにユーザー名を設定
     */
    public void setTabUser(String tabId, String username) {
        TabSession session = tabSessions.get(tabId);
        if (session != null) {
            session.setUsername(username);
            session.updateLastAccessed();
        }
    }
    
    /**
     * タブセッションからユーザー名を取得
     */
    public String getTabUser(String tabId) {
        TabSession session = tabSessions.get(tabId);
        return session != null ? session.getUsername() : null;
    }
    
    /**
     * タブセッションを削除
     */
    public void removeTabSession(String tabId) {
        tabSessions.remove(tabId);
    }
    
    /**
     * 古いセッションをクリーンアップ（1時間以上アクセスなし）
     */
    public void cleanupOldSessions() {
        long currentTime = System.currentTimeMillis();
        long timeout = 3600000; // 1時間
        
        tabSessions.entrySet().removeIf(entry -> {
            TabSession session = entry.getValue();
            return (currentTime - session.getLastAccessed()) > timeout;
        });
    }
    
    /**
     * 全てのタブセッション情報を取得
     */
    public Map<String, TabSession> getAllTabSessions() {
        return new ConcurrentHashMap<>(tabSessions);
    }
    
    /**
     * 特定ユーザーのタブセッション数を取得
     */
    public long getUserTabCount(String username) {
        if (username == null) return 0;
        return tabSessions.values().stream()
            .filter(session -> username.equals(session.getUsername()))
            .count();
    }
}
