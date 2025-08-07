package com.example.chatapp.repository;

import com.example.chatapp.entity.Friendship;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user OR f.addressee = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("user") User user);

    @Query("SELECT f FROM Friendship f WHERE f.addressee = :user AND f.status = 'PENDING'")
    List<Friendship> findPendingRequests(@Param("user") User user);

    @Query("SELECT f FROM Friendship f WHERE f.requester = :user AND f.status = 'PENDING'")
    List<Friendship> findSentRequests(@Param("user") User user);

    @Query("SELECT f FROM Friendship f WHERE " +
           "((f.requester = :user1 AND f.addressee = :user2) OR " +
           "(f.requester = :user2 AND f.addressee = :user1))")
    Optional<Friendship> findBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT f FROM Friendship f WHERE " +
           "((f.requester = :user1 AND f.addressee = :user2) OR " +
           "(f.requester = :user2 AND f.addressee = :user1)) AND " +
           "f.status = :status")
    Optional<Friendship> findBetweenUsersWithStatus(@Param("user1") User user1, 
                                                   @Param("user2") User user2, 
                                                   @Param("status") Friendship.FriendshipStatus status);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE (f.requester = :user OR f.addressee = :user) AND f.status = 'ACCEPTED'")
    long countFriends(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.addressee = :user AND f.status = 'PENDING'")
    long countPendingRequests(@Param("user") User user);

    @Query("SELECT f FROM Friendship f WHERE " +
           "f.status = 'ACCEPTED' AND " +
           "(f.requester = :user OR f.addressee = :user)")
    List<Friendship> findAllFriends(@Param("user") User user);

    void deleteByRequesterAndAddressee(User requester, User addressee);

    // Spring Data JPA の命名規則に基づくメソッド
    List<Friendship> findByAddresseeAndStatus(User addressee, Friendship.FriendshipStatus status);
    
    List<Friendship> findByRequesterAndStatus(User requester, Friendship.FriendshipStatus status);
    
    List<Friendship> findByRequesterAndAddresseeAndStatus(User requester, User addressee, Friendship.FriendshipStatus status);
    
    boolean existsByRequesterAndAddresseeAndStatus(User requester, User addressee, Friendship.FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user OR f.addressee = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendshipsByUser(@Param("user") User user);
}
