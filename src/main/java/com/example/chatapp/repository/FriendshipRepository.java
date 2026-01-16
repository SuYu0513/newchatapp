package com.example.chatapp.repository;

import com.example.chatapp.entity.Friendship;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /**
     * フォロー関係を取得（follower → following）
     */
    Optional<Friendship> findByFollowerAndFollowing(User follower, User following);

    /**
     * フォローしているユーザー一覧を取得
     */
    @Query("SELECT f.following FROM Friendship f WHERE f.follower = :user")
    List<User> findFollowing(@Param("user") User user);

    /**
     * フォロワー一覧を取得
     */
    @Query("SELECT f.follower FROM Friendship f WHERE f.following = :user")
    List<User> findFollowers(@Param("user") User user);

    /**
     * 相互フォロー（友達）一覧を取得
     */
    @Query("SELECT f1.following FROM Friendship f1 WHERE f1.follower = :user AND " +
           "EXISTS (SELECT f2 FROM Friendship f2 WHERE f2.follower = f1.following AND f2.following = :user)")
    List<User> findMutualFollows(@Param("user") User user);

    /**
     * 相互フォローかどうか確認
     */
    @Query("SELECT CASE WHEN COUNT(f1) > 0 AND COUNT(f2) > 0 THEN true ELSE false END " +
           "FROM Friendship f1, Friendship f2 " +
           "WHERE f1.follower = :user1 AND f1.following = :user2 " +
           "AND f2.follower = :user2 AND f2.following = :user1")
    boolean areMutualFollows(@Param("user1") User user1, @Param("user2") User user2);

    /**
     * フォローしているかどうか確認
     */
    boolean existsByFollowerAndFollowing(User follower, User following);

    /**
     * フォロー数をカウント
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "false"))
    long countByFollower(User follower);

    /**
     * フォロワー数をカウント
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "false"))
    long countByFollowing(User following);

    /**
     * フォロー関係を削除
     */
    void deleteByFollowerAndFollowing(User follower, User following);

    // 後方互換性のためのメソッド（既存コード用）
    @Deprecated
    @Query("SELECT f FROM Friendship f WHERE f.follower = :user OR f.following = :user")
    List<Friendship> findAcceptedFriendships(@Param("user") User user);

    @Deprecated
    default Optional<Friendship> findBetweenUsers(User user1, User user2) {
        return findByFollowerAndFollowing(user1, user2);
    }

    @Deprecated
    default long countFriends(User user) {
        return countByFollower(user);
    }
}
