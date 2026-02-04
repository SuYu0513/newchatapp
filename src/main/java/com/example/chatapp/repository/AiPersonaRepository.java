package com.example.chatapp.repository;

import com.example.chatapp.entity.AiPersona;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiPersonaRepository extends JpaRepository<AiPersona, Long> {
    Optional<AiPersona> findByUser(User user);
    boolean existsByUser(User user);
}
