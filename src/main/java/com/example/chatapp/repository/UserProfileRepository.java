package com.example.chatapp.repository;

import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUser(User user);

    Optional<UserProfile> findByUserId(Long userId);

    @Query("SELECT up FROM UserProfile up WHERE up.isSearchable = true AND " +
           "(LOWER(up.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(up.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<UserProfile> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT up FROM UserProfile up WHERE up.allowRandomMatching = true AND up.user.id != :excludeUserId")
    List<UserProfile> findAvailableForRandomMatching(@Param("excludeUserId") Long excludeUserId);

    @Query("SELECT up FROM UserProfile up WHERE up.onlineStatus = 'ONLINE'")
    List<UserProfile> findOnlineUsers();

    @Query("SELECT COUNT(up) FROM UserProfile up WHERE up.onlineStatus = 'ONLINE'")
    long countOnlineUsers();

    List<UserProfile> findByPrivacyLevel(UserProfile.PrivacyLevel privacyLevel);

    @Query("SELECT up FROM UserProfile up WHERE up.user.id IN :userIds")
    List<UserProfile> findByUserIds(@Param("userIds") List<Long> userIds);
}
