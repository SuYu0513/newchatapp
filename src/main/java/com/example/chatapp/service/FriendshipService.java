package com.example.chatapp.service;

import com.example.chatapp.entity.Friendship;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.repository.FriendshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserProfileService userProfileService;

    /**
     * フレンド申請を送信
     */
    public Friendship sendFriendRequest(User requester, User addressee) {
        // 自分自身への申請をチェック
        if (requester.equals(addressee)) {
            throw new IllegalArgumentException("自分自身にフレンド申請はできません");
        }

        // 既存の関係をチェック
        Optional<Friendship> existingFriendship = friendshipRepository.findBetweenUsers(requester, addressee);
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            switch (friendship.getStatus()) {
                case PENDING:
                    throw new IllegalArgumentException("既にフレンド申請が送信されています");
                case ACCEPTED:
                    throw new IllegalArgumentException("既にフレンドです");
                case BLOCKED:
                    throw new IllegalArgumentException("この操作は実行できません");
                case DECLINED:
                    // 拒否された場合は新しい申請を許可
                    friendship.setStatus(Friendship.FriendshipStatus.PENDING);
                    friendship.setRequester(requester);
                    friendship.setAddressee(addressee);
                    return friendshipRepository.save(friendship);
            }
        }

        // 新しいフレンド申請を作成
        Friendship newFriendship = new Friendship(requester, addressee);
        return friendshipRepository.save(newFriendship);
    }

    /**
     * フレンド申請を承認（リクエスターのIDから検索）
     */
    public Friendship acceptFriendRequest(User requester, User addressee) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findBetweenUsers(requester, addressee);
        if (!friendshipOpt.isPresent()) {
            throw new IllegalArgumentException("フレンド申請が見つかりません");
        }
        
        Friendship friendship = friendshipOpt.get();
        
        // 申請の受取人であることを確認
        if (!friendship.getAddressee().equals(addressee)) {
            throw new IllegalArgumentException("この申請を承認する権限がありません");
        }

        // ステータスが PENDING であることを確認
        if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("この申請は既に処理済みです");
        }

        friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
        return friendshipRepository.save(friendship);
    }

    /**
     * フレンド申請を承認（フレンドシップIDから検索）
     */
    public Friendship acceptFriendRequestById(User addressee, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("フレンド申請が見つかりません"));

        // 申請の受取人であることを確認
        if (!friendship.getAddressee().equals(addressee)) {
            throw new IllegalArgumentException("この申請を承認する権限がありません");
        }

        // ステータスが PENDING であることを確認
        if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("この申請は既に処理済みです");
        }

        friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
        return friendshipRepository.save(friendship);
    }

    /**
     * フレンド申請を拒否（リクエスターのIDから検索）
     */
    public Friendship declineFriendRequest(User requester, User addressee) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findBetweenUsers(requester, addressee);
        if (!friendshipOpt.isPresent()) {
            throw new IllegalArgumentException("フレンド申請が見つかりません");
        }
        
        Friendship friendship = friendshipOpt.get();
        
        // 申請の受取人であることを確認
        if (!friendship.getAddressee().equals(addressee)) {
            throw new IllegalArgumentException("この申請を拒否する権限がありません");
        }

        // ステータスが PENDING であることを確認
        if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("この申請は既に処理済みです");
        }

        friendship.setStatus(Friendship.FriendshipStatus.DECLINED);
        return friendshipRepository.save(friendship);
    }

    /**
     * フレンド申請を拒否（フレンドシップIDから検索）
     */
    public Friendship declineFriendRequestById(User addressee, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("フレンド申請が見つかりません"));

        // 申請の受取人であることを確認
        if (!friendship.getAddressee().equals(addressee)) {
            throw new IllegalArgumentException("この申請を拒否する権限がありません");
        }

        // ステータスが PENDING であることを確認
        if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("この申請は既に処理済みです");
        }

        friendship.setStatus(Friendship.FriendshipStatus.DECLINED);
        return friendshipRepository.save(friendship);
    }

    /**
     * フレンド関係を削除（ユーザー間の関係を削除）
     */
    public void removeFriendByUsers(User currentUser, User friendUser) {
        Optional<Friendship> friendship = friendshipRepository.findBetweenUsers(currentUser, friendUser);
        
        if (!friendship.isPresent()) {
            throw new IllegalArgumentException("フレンド関係が見つかりません");
        }
        
        Friendship friendshipEntity = friendship.get();
        if (friendshipEntity.getStatus() != Friendship.FriendshipStatus.ACCEPTED) {
            throw new IllegalArgumentException("フレンド関係ではありません");
        }
        
        friendshipRepository.delete(friendshipEntity);
    }

    /**
     * フレンド申請を取り消し（PENDING状態の関係を削除）
     */
    public void cancelFriendRequest(User requester, User addressee) {
        Optional<Friendship> friendship = friendshipRepository.findBetweenUsers(requester, addressee);
        
        if (!friendship.isPresent()) {
            throw new IllegalArgumentException("フレンド申請が見つかりません");
        }
        
        Friendship friendshipEntity = friendship.get();
        
        // 申請者が自分で、状態がPENDINGであることを確認
        if (!friendshipEntity.getRequester().equals(requester)) {
            throw new IllegalArgumentException("この申請を取り消す権限がありません");
        }
        
        if (friendshipEntity.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("取り消せる申請ではありません（状態: " + friendshipEntity.getStatus() + "）");
        }
        
        friendshipRepository.delete(friendshipEntity);
    }

    /**
     * フレンド関係を削除
     */
    public void removeFriend(User user, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("フレンド関係が見つかりません"));

        // ユーザーがこの関係に含まれることを確認
        if (!friendship.involvesUser(user)) {
            throw new IllegalArgumentException("このフレンド関係を削除する権限がありません");
        }

        friendshipRepository.delete(friendship);
    }

    /**
     * ユーザーをブロック
     */
    public Friendship blockUser(User blocker, User blocked) {
        if (blocker.equals(blocked)) {
            throw new IllegalArgumentException("自分自身をブロックすることはできません");
        }

        Optional<Friendship> existingFriendship = friendshipRepository.findBetweenUsers(blocker, blocked);
        
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            friendship.setStatus(Friendship.FriendshipStatus.BLOCKED);
            friendship.setRequester(blocker);
            friendship.setAddressee(blocked);
            return friendshipRepository.save(friendship);
        } else {
            // 新しいブロック関係を作成
            Friendship blockRelation = new Friendship(blocker, blocked);
            blockRelation.setStatus(Friendship.FriendshipStatus.BLOCKED);
            return friendshipRepository.save(blockRelation);
        }
    }

    /**
     * ユーザーのブロックを解除
     */
    public void unblockUser(User unblocker, User unblockedUser) {
        Optional<Friendship> existingFriendship = friendshipRepository.findBetweenUsers(unblocker, unblockedUser);
        
        if (!existingFriendship.isPresent()) {
            throw new IllegalArgumentException("ブロック関係が見つかりません");
        }
        
        Friendship friendship = existingFriendship.get();
        if (friendship.getStatus() != Friendship.FriendshipStatus.BLOCKED) {
            throw new IllegalArgumentException("この関係はブロック状態ではありません");
        }
        
        // ブロック関係を削除
        friendshipRepository.delete(friendship);
    }

    /**
     * フレンド一覧を取得
     */
    @Transactional(readOnly = true)
    public List<User> getFriends(User user) {
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(user);
        return friendships.stream()
            .map(friendship -> friendship.getFriendOf(user))
            .collect(Collectors.toList());
    }

    /**
     * フレンド一覧をプロフィール付きで取得
     */
    @Transactional(readOnly = true)
    public List<UserProfile> getFriendsWithProfiles(User user) {
        List<User> friends = getFriends(user);
        return friends.stream()
            .map(userProfileService::getOrCreateProfile)
            .collect(Collectors.toList());
    }

    /**
     * 受信したフレンド申請一覧を取得
     */
    @Transactional(readOnly = true)
    public List<Friendship> getPendingRequests(User user) {
        return friendshipRepository.findPendingRequests(user);
    }

    /**
     * 送信したフレンド申請一覧を取得
     */
    @Transactional(readOnly = true)
    public List<Friendship> getSentRequests(User user) {
        return friendshipRepository.findSentRequests(user);
    }

    /**
     * 二人のユーザー間の関係を取得
     */
    @Transactional(readOnly = true)
    public Optional<Friendship> getFriendshipBetween(User user1, User user2) {
        return friendshipRepository.findBetweenUsers(user1, user2);
    }

    /**
     * フレンド関係の状態を取得
     */
    @Transactional(readOnly = true)
    public String getFriendshipStatus(User user1, User user2) {
        if (user1.equals(user2)) {
            return "self";
        }

        Optional<Friendship> friendship = friendshipRepository.findBetweenUsers(user1, user2);
        if (friendship.isEmpty()) {
            return "none";
        }

        Friendship relation = friendship.get();
        switch (relation.getStatus()) {
            case ACCEPTED:
                return "friends";
            case PENDING:
                if (relation.getRequester().equals(user1)) {
                    return "sent_request";
                } else {
                    return "received_request";
                }
            case BLOCKED:
                if (relation.getRequester().equals(user1)) {
                    return "blocked_by_you";
                } else {
                    return "blocked_by_them";
                }
            case DECLINED:
                return "declined";
            default:
                return "none";
        }
    }

    /**
     * フレンド数を取得
     */
    @Transactional(readOnly = true)
    public long getFriendCount(User user) {
        return friendshipRepository.countFriends(user);
    }

    /**
     * 未処理のフレンド申請数を取得
     */
    @Transactional(readOnly = true)
    public long getPendingRequestCount(User user) {
        return friendshipRepository.countPendingRequests(user);
    }

    /**
     * 二人がフレンドかどうかを確認
     */
    @Transactional(readOnly = true)
    public boolean areFriends(User user1, User user2) {
        return friendshipRepository.findBetweenUsersWithStatus(
            user1, user2, Friendship.FriendshipStatus.ACCEPTED
        ).isPresent();
    }

    /**
     * ユーザーがブロックされているかを確認
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(User user1, User user2) {
        Optional<Friendship> friendship = friendshipRepository.findBetweenUsers(user1, user2);
        return friendship.isPresent() && friendship.get().getStatus() == Friendship.FriendshipStatus.BLOCKED;
    }

    /**
     * フレンド申請が可能かどうかを確認
     */
    @Transactional(readOnly = true)
    public boolean canSendFriendRequest(User requester, User addressee) {
        if (requester.equals(addressee)) {
            return false;
        }

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(requester, addressee);
        if (existing.isEmpty()) {
            return true;
        }

        Friendship friendship = existing.get();
        return friendship.getStatus() == Friendship.FriendshipStatus.DECLINED;
    }

    /**
     * ユーザーのフレンド一覧を取得（ACCEPTED状態）
     */
    @Transactional(readOnly = true)
    public List<Friendship> getFriendshipsByUser(User user) {
        return friendshipRepository.findAcceptedFriendshipsByUser(user);
    }

    /**
     * 送信した申請一覧を取得（PENDING状態）
     */
    @Transactional(readOnly = true)
    public List<Friendship> getPendingRequestsByRequester(User requester) {
        return friendshipRepository.findByRequesterAndStatus(requester, Friendship.FriendshipStatus.PENDING);
    }

    /**
     * ブロック済みユーザーとの関係一覧を取得
     */
    @Transactional(readOnly = true)
    public List<Friendship> getBlockedUsersByUser(User user) {
        List<Friendship> result = friendshipRepository.findByRequesterAndStatus(user, Friendship.FriendshipStatus.BLOCKED);
        result.addAll(friendshipRepository.findByAddresseeAndStatus(user, Friendship.FriendshipStatus.BLOCKED));
        return result;
    }

    /**
     * 送信した申請一覧を取得（PENDING状態）
     */
    @Transactional(readOnly = true)
    public List<Friendship> getPendingRequestsBySender(User sender) {
        return friendshipRepository.findByRequesterAndStatus(sender, Friendship.FriendshipStatus.PENDING);
    }

    /**
     * 受信した申請一覧を取得（PENDING状態）
     */
    @Transactional(readOnly = true)
    public List<Friendship> getPendingRequestsByAddressee(User addressee) {
        return friendshipRepository.findByAddresseeAndStatus(addressee, Friendship.FriendshipStatus.PENDING);
    }

    /**
     * ブロック済みユーザー一覧を取得
     */
    @Transactional(readOnly = true)
    public List<User> getBlockedUsers(User user) {
        List<Friendship> blockedFriendships = friendshipRepository.findByRequesterAndStatus(user, Friendship.FriendshipStatus.BLOCKED);
        return blockedFriendships.stream()
            .map(Friendship::getAddressee)
            .collect(Collectors.toList());
    }
}
