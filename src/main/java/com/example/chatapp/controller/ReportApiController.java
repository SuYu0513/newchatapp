package com.example.chatapp.controller;

import com.example.chatapp.entity.Report;
import com.example.chatapp.entity.ReportReason;
import com.example.chatapp.entity.User;
import com.example.chatapp.service.ReportService;
import com.example.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    // 通報送信エンドポイント
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitReport(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String username = principal.getName();
            Optional<User> reporterOpt = userService.findByUsername(username);
            if (!reporterOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long reportedUserId = Long.valueOf(payload.get("reportedUserId").toString());
            Optional<User> reportedUserOpt = userService.findById(reportedUserId);
            if (!reportedUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "通報対象ユーザーが見つかりません");
                return ResponseEntity.badRequest().body(response);
            }
            
            String reasonStr = payload.get("reason").toString();
            ReportReason reason;
            try {
                reason = ReportReason.valueOf(reasonStr);
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("message", "無効な通報理由です");
                return ResponseEntity.badRequest().body(response);
            }
            
            String description = payload.get("description") != null ? 
                payload.get("description").toString() : "";
            
            // 通報を保存
            Report report;
            try {
                report = reportService.createReport(
                    reporterOpt.get(), 
                    reportedUserOpt.get(), 
                    reason, 
                    description
                );
            } catch (RuntimeException e) {
                // 重複通報の場合
                response.put("success", false);
                response.put("message", e.getMessage());
                return ResponseEntity.ok(response);
            }
            
            if (report != null) {
                // 通報回数を取得
                long reportCount = reportService.getReportCountForUser(reportedUserOpt.get());
                
                // 管理者通知を作成（TODO: 実装）
                // createAdminNotification(report, reportCount);
                
                response.put("success", true);
                response.put("message", "通報を受け付けました。ご協力ありがとうございます。");
            } else {
                response.put("success", false);
                response.put("message", "通報の送信に失敗しました");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "エラーが発生しました: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
