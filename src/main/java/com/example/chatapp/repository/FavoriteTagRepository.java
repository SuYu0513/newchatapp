package com.example.chatapp.repository;

import com.example.chatapp.entity.FavoriteTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteTagRepository extends JpaRepository<FavoriteTag, Long> {
    Optional<FavoriteTag> findByTagName(String tagName);
    
    @Query("SELECT f FROM FavoriteTag f ORDER BY f.usageCount DESC")
    List<FavoriteTag> findTop10ByOrderByUsageCountDesc(org.springframework.data.domain.Pageable pageable);
}
