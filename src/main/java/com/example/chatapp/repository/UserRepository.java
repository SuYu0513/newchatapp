package com.example.chatapp.repository;

import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    // フレンドコード関連メソッド
    boolean existsByFriendCode(Integer friendCode);
    
    Optional<User> findByFriendCode(Integer friendCode);
    
    // ユーザー検索メソッド（ユーザー名またはフレンドコード）
    List<User> findByUsernameContainingIgnoreCase(String username);
}
