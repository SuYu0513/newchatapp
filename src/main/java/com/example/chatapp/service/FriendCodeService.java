package com.example.chatapp.service;

import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class FriendCodeService {

    @Autowired
    private UserRepository userRepository;

    private static final int MIN_CODE = 10000000; // 8桁の最小値
    private static final int MAX_CODE = 99999999; // 8桁の最大値
    private static final SecureRandom random = new SecureRandom();

    /**
     * ユニークな8桁のフレンドコードを生成
     */
    public Integer generateUniqueFriendCode() {
        Integer code;
        int attempts = 0;
        final int maxAttempts = 100;

        do {
            code = generateRandomCode();
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new RuntimeException("フレンドコードの生成に失敗しました（最大試行回数に達しました）");
            }
        } while (userRepository.existsByFriendCode(code));

        return code;
    }

    /**
     * ランダムな8桁のコードを生成
     */
    private Integer generateRandomCode() {
        return MIN_CODE + random.nextInt(MAX_CODE - MIN_CODE + 1);
    }

    /**
     * フレンドコードの形式をチェック
     */
    public boolean isValidFriendCodeFormat(Integer code) {
        if (code == null) {
            return false;
        }
        
        return code >= MIN_CODE && code <= MAX_CODE;
    }

    /**
     * フレンドコードが存在するかチェック
     */
    public boolean friendCodeExists(Integer code) {
        return userRepository.existsByFriendCode(code);
    }
}
