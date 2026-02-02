package com.example.chatapp.repository;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RoomInvitation;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomInvitationRepository extends JpaRepository<RoomInvitation, Long> {

    // ルームと招待されたユーザーで検索
    Optional<RoomInvitation> findFirstByChatRoomAndInvitee(ChatRoom chatRoom, User invitee);

    // ルームと招待されたユーザーで全て検索
    List<RoomInvitation> findAllByChatRoomAndInvitee(ChatRoom chatRoom, User invitee);

    // ルームの全招待を取得
    List<RoomInvitation> findByChatRoom(ChatRoom chatRoom);

    // ルームの承認待ち招待を取得
    List<RoomInvitation> findByChatRoomAndStatus(ChatRoom chatRoom, RoomInvitation.InvitationStatus status);

    // 招待されたユーザーの招待を取得
    List<RoomInvitation> findByInvitee(User invitee);

    // 招待されたユーザーの承認待ち招待を取得
    List<RoomInvitation> findByInviteeAndStatus(User invitee, RoomInvitation.InvitationStatus status);

    // 招待者の招待を取得
    List<RoomInvitation> findByInviter(User inviter);

    // 招待者の承認待ち招待を取得
    List<RoomInvitation> findByInviterAndStatus(User inviter, RoomInvitation.InvitationStatus status);
}
