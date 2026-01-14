package com.example.chatapp.service;

import com.example.chatapp.entity.Friendship;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.FriendshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * フォロー/フォロワー管理サービス
 * - フォロー: 自分が相手をフォロー
 * - フォロワー: 相手が自分をフォロー
 * - 友達: 相互フォロー（お互いがお互いをフォロー）
 */
@Service
@Transactional
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    /**
     * ユーザーをフォローする
     */
    public Friendship follow(User follower, User following) {
        // 自分自身をフォローできない
        if (follower.equals(following)) {
            throw new IllegalArgumentException("自分自身をフォローすることはできません");
        }

        // 既にフォローしているかチェック
        if (friendshipRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new IllegalArgumentException("既にフォローしています");
        }

        // フォロー関係を作成
        Friendship friendship = new Friendship(follower, following);
        return friendshipRepository.save(friendship);
    }

    /**
     * フォローを解除する
     */
    public void unfollow(User follower, User following) {
        Optional<Friendship> friendship = friendshipRepository.findByFollowerAndFollowing(follower, following);
        if (!friendship.isPresent()) {
            throw new IllegalArgumentException("フォロー関係が存在しません");
        }
        friendshipRepository.delete(friendship.get());
    }

    /**
     * フォローしているかチェック
     */
    public boolean isFollowing(User follower, User following) {
        return friendshipRepository.existsByFollowerAndFollowing(follower, following);
    }

    /**
     * 相互フォロー（友達）かチェック
     */
    public boolean areFriends(User user1, User user2) {
        return friendshipRepository.areMutualFollows(user1, user2);
    }

    /**
     * フォローしているユーザー一覧を取得
     */
    public List<User> getFollowing(User user) {
        return friendshipRepository.findFollowing(user);
    }

    /**
     * フォロワー一覧を取得
     */
    public List<User> getFollowers(User user) {
        return friendshipRepository.findFollowers(user);
    }

    /**
     * 友達（相互フォロー）一覧を取得
     */
    public List<User> getFriends(User user) {
        return friendshipRepository.findMutualFollows(user);
    }

    /**
     * フォロー数を取得
     */
    public long getFollowingCount(User user) {
        return friendshipRepository.countByFollower(user);
    }

    /**
     * フォロワー数を取得
     */
    public long getFollowersCount(User user) {
        return friendshipRepository.countByFollowing(user);
    }

    /**
     * 友達数を取得
     */
    public long getFriendsCount(User user) {
        return getFriends(user).size();
    }

    /**
     * ユーザー間の関係を取得
     * @return "friend" (相互フォロー), "following" (フォロー中), "follower" (フォローされている), "none" (関係なし)
     */
    public String getRelationship(User currentUser, User targetUser) {
        boolean isFollowing = isFollowing(currentUser, targetUser);
        boolean isFollower = isFollowing(targetUser, currentUser);

        if (isFollowing && isFollower) {
            return "friend"; // 相互フォロー
        } else if (isFollowing) {
            return "following"; // フォロー中
        } else if (isFollower) {
            return "follower"; // フォローされている
        } else {
            return "none"; // 関係なし
        }
    }

    /**
     * フレンドコードでユーザーを検索してフォロー
     */
    public Friendship followByFriendCode(User follower, Integer friendCode, UserService userService) {
        List<User> allUsers = userService.findAll();
        Optional<User> targetUserOpt = allUsers.stream()
            .filter(u -> u.getFriendCode() != null && u.getFriendCode().equals(friendCode))
            .findFirst();
        
        if (!targetUserOpt.isPresent()) {
            throw new IllegalArgumentException("フレンドコードが見つかりません");
        }
        
        User targetUser = targetUserOpt.get();
        return follow(follower, targetUser);
    }
}
