package com.example.chatapp.repository;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.Message;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByChatRoomOrderBySentAtAsc(ChatRoom chatRoom);
    
    List<Message> findByUserOrderBySentAtDesc(User user);
    
    List<Message> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);
}
