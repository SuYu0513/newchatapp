package com.example.chatapp.service;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FriendCodeService friendCodeService;

    @Override
    public void run(String... args) throws Exception {
        // システムユーザーが存在しない場合のみ作成
        if (userRepository.findByUsername("system").isEmpty()) {
            User systemUser = new User();
            systemUser.setUsername("system");
            systemUser.setEmail("system@chatapp.com");
            systemUser.setPassword(passwordEncoder.encode("system123"));
            systemUser.setFriendCode(friendCodeService.generateUniqueFriendCode());
            userRepository.save(systemUser);
            System.out.println("システムユーザーを作成しました。");
        }

        // デフォルトチャットルームが存在しない場合のみ作成
        if (chatRoomRepository.count() == 0) {
            User systemUser = userRepository.findByUsername("system").orElseThrow();
            
            ChatRoom defaultRoom = new ChatRoom();
            defaultRoom.setName("メインチャット");
            defaultRoom.setType(ChatRoom.ChatRoomType.GROUP);
            defaultRoom.setCreatedBy(systemUser);
            chatRoomRepository.save(defaultRoom);
            
            System.out.println("デフォルトチャットルーム「メインチャット」を作成しました。");
        } else {
            System.out.println("既存のチャットルームが見つかりました。データベースは正常に永続化されています。");
        }
    }
}
