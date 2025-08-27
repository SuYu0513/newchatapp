package com.example.chatapp.service;

import com.example.chatapp.entity.User;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private FriendCodeService friendCodeService;

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
        
        // フレンドコードを生成
        Integer friendCode = friendCodeService.generateUniqueFriendCode();

        // ユーザーを作成・保存
        User user = new User(username, hashedPassword, email);
        user.setFriendCode(friendCode);
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

    // ユーザー検索メソッド
    public List<User> searchUsers(String query) {
        List<User> results = new ArrayList<>();
        
        try {
            // まずIDで検索を試行
            Long id = Long.parseLong(query);
            Optional<User> userById = userRepository.findById(id);
            if (userById.isPresent()) {
                results.add(userById.get());
                return results;
            }
        } catch (NumberFormatException e) {
            // IDでない場合は無視
        }
        
        // フレンドコードで検索（完全一致）
        try {
            Integer friendCode = Integer.parseInt(query);
            if (friendCodeService.isValidFriendCodeFormat(friendCode)) {
                Optional<User> userByFriendCode = userRepository.findByFriendCode(friendCode);
                if (userByFriendCode.isPresent()) {
                    results.add(userByFriendCode.get());
                    return results;
                }
            }
        } catch (NumberFormatException e) {
            // フレンドコードでない場合は無視
        }
        
        // ユーザー名で部分一致検索
        List<User> usersByUsername = userRepository.findByUsernameContainingIgnoreCase(query);
        results.addAll(usersByUsername);
        
        return results;
    }
    
    /**
     * 全ユーザー数を取得
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }
}
