package com.example.chatapp.config;

import com.example.chatapp.service.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * WebSocketセッションのイベントを処理するリスナー
 */
@Component
public class WebSocketEventListener {

    @Autowired
    private OnlineUserService onlineUserService;

    /**
     * WebSocket接続時の処理
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            
            // セッションからユーザー情報を取得
            Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                Authentication auth = (Authentication) sessionAttributes.get("SPRING_SECURITY_CONTEXT");
                if (auth != null && auth.isAuthenticated()) {
                    String username = auth.getName();
                    System.out.println("WebSocket接続: ユーザー " + username + " (セッション: " + sessionId + ")");
                    
                    // ユーザーをオンラインに設定
                    onlineUserService.setUserOnline(username, sessionId);
                }
            }
        } catch (Exception e) {
            System.err.println("WebSocket接続処理中にエラー: " + e.getMessage());
        }
    }

    /**
     * WebSocket切断時の処理
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            
            System.out.println("WebSocket切断: セッション " + sessionId);
            
            // セッションIDに基づいてユーザーをオフラインに設定
            onlineUserService.setUserOfflineBySessionId(sessionId);
        } catch (Exception e) {
            System.err.println("WebSocket切断処理中にエラー: " + e.getMessage());
        }
    }
}
