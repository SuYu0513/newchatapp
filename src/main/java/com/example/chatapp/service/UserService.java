package com.example.chatapp.service;

import com.example.chatapp.entity.User;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String username, String password, String email) {
        // ユーザー名とメールアドレスの重複チェック
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("このユーザー名は既に使用されています");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("このメールアドレスは既に使用されています");
        }

        // パスワードをハッシュ化
        String hashedPassword = passwordEncoder.encode(password);

        // ユーザーを作成・保存
        User user = new User(username, hashedPassword, email);
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
