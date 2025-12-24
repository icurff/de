package com.example.demo.service;


import com.example.demo.repository.LivestreamKeyRepository;
import com.example.demo.repository.LivestreamRepository;
import com.example.demo.repository.ServerRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private LivestreamRepository livestreamRepository;

    @Autowired
    private LivestreamKeyRepository livestreamKeyRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ActivityLogService activityLogService;

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getTotalVideoCount() {
        return videoRepository.count();
    }

    public long getTotalLivestreamCount() {
        return livestreamRepository.count();
    }

    public long getActiveLivestreamCount() {
        return livestreamKeyRepository.countByIsLive(true);
    }

    public List<Map<String, Object>> getServerQueueInfo() {
        return serverRepository.findAll().stream()
                .map(server -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", server.getId());
                    info.put("name", server.getName());
                    info.put("ip", server.getIp());
                    info.put("status", server.getStatus().name());
                    info.put("taskCount", server.getCurrent_load()); // current_load represents pending tasks
                    return info;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRecentActivityLogs(int limit) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
                .withZone(ZoneId.systemDefault());
        
        return activityLogService.getRecentLogs(limit).stream()
                .map(log -> {
                    Map<String, Object> logMap = new HashMap<>();
                    logMap.put("id", log.getId());
                    logMap.put("action", log.getAction());
                    logMap.put("serverName", log.getServerName());
                    logMap.put("serverIp", log.getServerIp());
                    logMap.put("username", log.getUsername());
                    logMap.put("details", log.getDetails());
                    logMap.put("createdAt", log.getCreatedAt() != null ? formatter.format(log.getCreatedAt()) : null);
                    return logMap;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalUsers", getTotalUserCount());
        dashboard.put("totalVideos", getTotalVideoCount());
        dashboard.put("totalLivestreams", getTotalLivestreamCount());
        dashboard.put("activeLivestreams", getActiveLivestreamCount());
        dashboard.put("serverQueue", getServerQueueInfo());
        dashboard.put("activityLogs", getRecentActivityLogs(20));
        return dashboard;
    }
}
