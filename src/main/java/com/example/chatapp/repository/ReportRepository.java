package com.example.chatapp.repository;

import com.example.chatapp.entity.Report;
import com.example.chatapp.entity.ReportStatus;
import com.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    // 通報者でフィルタ
    List<Report> findByReporter(User reporter);
    
    // 通報されたユーザーでフィルタ
    List<Report> findByReportedUser(User reportedUser);
    
    // ステータスでフィルタ
    List<Report> findByStatus(ReportStatus status);
    
    // 未処理の通報を取得
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    
    // 特定の通報者と被通報者の組み合わせをチェック（重複防止）
    boolean existsByReporterAndReportedUser(User reporter, User reportedUser);
    
    // 特定のユーザーに対する通報数をカウント
    long countByReportedUser(User reportedUser);
    
    // 特定のユーザーに対するユニークな通報者数をカウント
    @Query("SELECT COUNT(DISTINCT r.reporter) FROM Report r WHERE r.reportedUser = :reportedUser")
    long countDistinctReportersByReportedUser(@Param("reportedUser") User reportedUser);
}
