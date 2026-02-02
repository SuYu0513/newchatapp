package com.example.chatapp.repository;

import com.example.chatapp.entity.MatchLike;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchLikeRepository extends JpaRepository<MatchLike, Long> {

    /** 重複チェック */
    Optional<MatchLike> findByLikerAndLiked(User liker, User liked);
    boolean existsByLikerAndLiked(User liker, User liked);

    /** 自分がいいねした相手一覧 */
    List<MatchLike> findByLikerOrderByCreatedAtDesc(User liker);

    /** 自分にいいねしてくれた人一覧 */
    List<MatchLike> findByLikedOrderByCreatedAtDesc(User liked);

    /** 相互いいね（マッチ成立）一覧 — 自分がlikerで、相手も自分をlikeしている */
    @Query("SELECT ml FROM MatchLike ml WHERE ml.liker = :user " +
           "AND EXISTS (SELECT 1 FROM MatchLike ml2 WHERE ml2.liker = ml.liked AND ml2.liked = :user)")
    List<MatchLike> findMutualLikes(@Param("user") User user);

    /** 特定ユーザーペアで相互likeかチェック */
    @Query("SELECT CASE WHEN COUNT(ml) > 0 THEN true ELSE false END " +
           "FROM MatchLike ml WHERE ml.liker = :a AND ml.liked = :b " +
           "AND EXISTS (SELECT 1 FROM MatchLike ml2 WHERE ml2.liker = :b AND ml2.liked = :a)")
    boolean isMutualLike(@Param("a") User a, @Param("b") User b);
}
