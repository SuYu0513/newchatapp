package com.example.chatapp.service;

import com.example.chatapp.entity.Report;
import com.example.chatapp.entity.ReportReason;
import com.example.chatapp.entity.ReportStatus;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    // 通報作成
    public Report createReport(User reporter, User reportedUser, ReportReason reason, String description) {
        // 同じユーザーから同じユーザーへの重複通報チェック
        if (reportRepository.existsByReporterAndReportedUser(reporter, reportedUser)) {
            throw new RuntimeException("既にこのユーザーを通報済みです");
        }
        
        Report report = new Report(reporter, reportedUser, reason, description);
        return reportRepository.save(report);
    }

    // 通報者による通報一覧取得
    public List<Report> getReportsByReporter(User reporter) {
        return reportRepository.findByReporter(reporter);
    }

    // 通報されたユーザーによる通報一覧取得
    public List<Report> getReportsByReportedUser(User reportedUser) {
        return reportRepository.findByReportedUser(reportedUser);
    }

    // ステータス別通報一覧取得
    public List<Report> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    // 未処理の通報取得
    public List<Report> getPendingReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING);
    }

    // 通報ステータス更新
    public Report updateReportStatus(Long reportId, ReportStatus status, User resolvedBy, String resolutionNote) {
        Optional<Report> reportOpt = reportRepository.findById(reportId);
        if (reportOpt.isPresent()) {
            Report report = reportOpt.get();
            report.setStatus(status);
            report.setResolvedAt(LocalDateTime.now());
            report.setResolvedBy(resolvedBy);
            report.setResolutionNote(resolutionNote);
            return reportRepository.save(report);
        }
        return null;
    }

    // 通報取得
    public Optional<Report> findById(Long id) {
        return reportRepository.findById(id);
    }

    // 全通報取得
    public List<Report> findAll() {
        return reportRepository.findAll();
    }
    
    // 特定ユーザーの通報回数取得
    public long getReportCountForUser(User user) {
        return reportRepository.countDistinctReportersByReportedUser(user);
    }
    
    // 重複通報チェック
    public boolean hasAlreadyReported(User reporter, User reportedUser) {
        return reportRepository.existsByReporterAndReportedUser(reporter, reportedUser);
    }
}
