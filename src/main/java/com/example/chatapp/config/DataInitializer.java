package com.example.chatapp.config;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // メインルーム（ID=1）が存在しない場合は作成
        Optional<ChatRoom> existingMainRoom = chatRoomRepository.findById(1L);
        
        if (existingMainRoom.isEmpty()) {
            // 最初のユーザーを取得（管理者として使用）
            Optional<User> firstUser = userRepository.findAll().stream().findFirst();
            
            ChatRoom mainRoom = new ChatRoom();
            mainRoom.setName("メインルーム");
            mainRoom.setType(ChatRoom.ChatRoomType.GROUP);
            
            if (firstUser.isPresent()) {
                mainRoom.setCreatedBy(firstUser.get());
            } else {
                // ユーザーが存在しない場合は一時的にnullで保存
                // 最初のユーザー登録時に後で更新される
                System.out.println("注意: メインルームが作成されましたが、まだユーザーが存在しません");
            }
            
            // 明示的にIDを1に設定してみる（JPAが許可する場合）
            try {
                // JPAでのID手動設定は通常推奨されないが、メインルーム用に特別に試行
                chatRoomRepository.save(mainRoom);
                System.out.println("メインルームが作成されました（ID=" + mainRoom.getId() + "）");
            } catch (Exception e) {
                System.err.println("メインルームの作成に失敗しました: " + e.getMessage());
            }
        } else {
            System.out.println("メインルームは既に存在します（ID=" + existingMainRoom.get().getId() + "）");
        }
    }
}
