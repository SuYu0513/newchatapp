package com.example.chatapp.repository;

import com.example.chatapp.entity.Post;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.QueryHint;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @QueryHints(@QueryHint(name = "javax.persistence.cache.storeMode", value = "REFRESH"))
    @Query("SELECT p FROM Post p JOIN FETCH p.user ORDER BY p.createdAt DESC")
    List<Post> findAllByOrderByCreatedAtDesc();

    @QueryHints(@QueryHint(name = "javax.persistence.cache.storeMode", value = "REFRESH"))
    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.user IN :users ORDER BY p.createdAt DESC")
    List<Post> findByUserInOrderByCreatedAtDesc(@Param("users") List<User> users);

    @QueryHints(@QueryHint(name = "javax.persistence.cache.storeMode", value = "REFRESH"))
    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.user = :user ORDER BY p.createdAt DESC")
    List<Post> findByUserOrderByCreatedAtDesc(@Param("user") User user);
}
