package com.example.demo.service;

import com.example.demo.model.Video;
import com.example.demo.model.WatchHistory;
import com.example.demo.repository.VideoRepository;
import com.example.demo.repository.WatchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WatchHistoryService {
    @Autowired
    private WatchHistoryRepository watchHistoryRepository;

    @Autowired
    private VideoRepository videoRepository;

    public void addToHistory(String username, String videoId) {
        // Check if video exists
        if (!videoRepository.existsById(videoId)) {
            return;
        }

        // Check if already exists
        watchHistoryRepository.findByUsernameAndVideoId(username, videoId)
                .ifPresentOrElse(
                        existing -> {
                            // Update watchedAt timestamp
                            existing.setWatchedAt(java.time.Instant.now());
                            watchHistoryRepository.save(existing);
                        },
                        () -> {
                            // Create new history entry
                            WatchHistory history = new WatchHistory(username, videoId);
                            watchHistoryRepository.save(history);
                        }
                );
    }

    public List<Map<String, Object>> getHistoryForUser(String username, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "watchedAt"));
        List<WatchHistory> historyList = watchHistoryRepository.findByUsernameOrderByWatchedAtDesc(username, pageable);

        List<String> videoIds = historyList.stream()
                .map(WatchHistory::getVideoId)
                .collect(Collectors.toList());

        if (videoIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch videos
        List<Video> videos = videoRepository.findAllById(videoIds);

        // Create a map for quick lookup
        Map<String, Video> videoMap = videos.stream()
                .collect(Collectors.toMap(Video::getId, v -> v));

        // Combine history with video data
        return historyList.stream()
                .filter(h -> videoMap.containsKey(h.getVideoId()))
                .map(h -> {
                    Video video = videoMap.get(h.getVideoId());
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("id", video.getId());
                    result.put("title", video.getTitle());
                    result.put("description", video.getDescription());
                    result.put("thumbnail", video.getThumbnail());
                    result.put("uploadedDate", video.getUploadedDate());
                    result.put("privacy", video.getPrivacy());
                    result.put("username", video.getUsername());
                    result.put("watchedAt", h.getWatchedAt());
                    result.put("duration", video.getDuration());
                    return result;
                })
                .collect(Collectors.toList());
    }
}

