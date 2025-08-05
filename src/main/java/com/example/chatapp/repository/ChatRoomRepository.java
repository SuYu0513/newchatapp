package com.example.chatapp.repository;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    List<ChatRoom> findByCreatedBy(User createdBy);
    
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.users u WHERE u.id = :userId")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);
    
    List<ChatRoom> findByType(ChatRoom.ChatRoomType type);
}
