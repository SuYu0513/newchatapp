package com.example.chatapp.repository;

import com.example.chatapp.entity.DirectMessage;
import com.example.chatapp.entity.DirectMessageConversation;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    
    /**
     * 指定した会話の全メッセージを時系列順に取得
     */
    List<DirectMessage> findByConversationOrderBySentAtAsc(DirectMessageConversation conversation);
    
    /**
     * 指定した会話の未読メッセージ数を取得
     */
    @Query("SELECT COUNT(m) FROM DirectMessage m WHERE " +
           "m.conversation = :conversation AND " +
           "m.receiver = :receiver AND " +
           "m.isRead = false")
    long countUnreadMessages(@Param("conversation") DirectMessageConversation conversation, 
                             @Param("receiver") User receiver);
    
    /**
     * 指定ユーザーの全未読メッセージ数を取得
     */
    @Query("SELECT COUNT(m) FROM DirectMessage m WHERE " +
           "m.receiver = :receiver AND " +
           "m.isRead = false")
    long countAllUnreadMessages(@Param("receiver") User receiver);
    
    /**
     * 指定した会話のメッセージを既読にする
     */
    @Modifying
    @Query("UPDATE DirectMessage m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP WHERE " +
           "m.conversation = :conversation AND " +
           "m.receiver = :receiver AND " +
           "m.isRead = false")
    void markConversationAsRead(@Param("conversation") DirectMessageConversation conversation, 
                                @Param("receiver") User receiver);
}
