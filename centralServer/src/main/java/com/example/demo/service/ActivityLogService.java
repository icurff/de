package com.example.demo.service;

import com.example.demo.model.ActivityLog;
import com.example.demo.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    public void logActivity(String action, String serverName, String serverIp, String username, String details) {
        ActivityLog log = ActivityLog.builder()
                .action(action)
                .serverName(serverName)
                .serverIp(serverIp)
                .username(username)
                .details(details)
                .createdAt(Instant.now())
                .build();
        activityLogRepository.save(log);
    }

    public List<ActivityLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
