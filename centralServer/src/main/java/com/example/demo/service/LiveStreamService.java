package com.example.demo.service;

import com.example.demo.model.LiveStream;
import com.example.demo.repository.LiveStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveStreamService {
    private final LiveStreamRepository liveStreamRepository;

    public LiveStream getLiveStream(String userId) {
        return liveStreamRepository.findByUserId(userId)
                .orElseGet(() -> createLiveStream(userId));
    }

    private LiveStream createLiveStream(String userId) {
        LiveStream liveStream = LiveStream.builder()
                .userId(userId)
                .streamKey(UUID.randomUUID().toString())
                .isLive(false)
                .build();
        return liveStreamRepository.save(liveStream);
    }

    public LiveStream updateLiveStream(String userId, String title, String description) {
        LiveStream liveStream = getLiveStream(userId);
        liveStream.setTitle(title);
        liveStream.setDescription(description);
        return liveStreamRepository.save(liveStream);
    }

    public LiveStream resetStreamKey(String userId) {
        LiveStream liveStream = getLiveStream(userId);
        liveStream.setStreamKey(UUID.randomUUID().toString());
        return liveStreamRepository.save(liveStream);
    }
}
