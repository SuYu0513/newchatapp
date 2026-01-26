package com.example.chatapp.repository;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RoomJoinRequest;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomJoinRequestRepository extends JpaRepository<RoomJoinRequest, Long> {

    // ルームとユーザーで申請を検索（最初の1件）
    Optional<RoomJoinRequest> findFirstByChatRoomAndUser(ChatRoom chatRoom, User user);

    // ルームとユーザーで全ての申請を検索（重複対応）
    List<RoomJoinRequest> findAllByChatRoomAndUser(ChatRoom chatRoom, User user);

    // ルームの全申請を取得
    List<RoomJoinRequest> findByChatRoom(ChatRoom chatRoom);

    // ルームの承認待ち申請を取得
    List<RoomJoinRequest> findByChatRoomAndStatus(ChatRoom chatRoom, RoomJoinRequest.RequestStatus status);

    // ユーザーの申請を取得
    List<RoomJoinRequest> findByUser(User user);

    // ユーザーの承認待ち申請を取得
    List<RoomJoinRequest> findByUserAndStatus(User user, RoomJoinRequest.RequestStatus status);
}
