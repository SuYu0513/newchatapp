package com.example.chatapp.repository;

import com.example.chatapp.entity.DirectMessageConversation;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectMessageConversationRepository extends JpaRepository<DirectMessageConversation, Long> {
    
    /**
     * 2人のユーザー間の会話を検索（順序は問わない）
     */
    @Query("SELECT c FROM DirectMessageConversation c WHERE " +
           "(c.user1 = :user1 AND c.user2 = :user2) OR " +
           "(c.user1 = :user2 AND c.user2 = :user1)")
    Optional<DirectMessageConversation> findByUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    /**
     * 指定ユーザーが参加している全ての会話を取得（最新メッセージ順）
     */
    @Query("SELECT c FROM DirectMessageConversation c WHERE " +
           "c.user1 = :user OR c.user2 = :user " +
           "ORDER BY c.lastMessageAt DESC")
    List<DirectMessageConversation> findByUserOrderByLastMessageAtDesc(@Param("user") User user);
    
    /**
     * 指定ユーザーが参加している会話数を取得
     */
    @Query("SELECT COUNT(c) FROM DirectMessageConversation c WHERE c.user1 = :user OR c.user2 = :user")
    long countByUser(@Param("user") User user);
}
