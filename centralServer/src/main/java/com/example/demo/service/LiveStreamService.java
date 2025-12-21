package com.example.demo.service;

import com.example.demo.model.LiveStreamKey;
import com.example.demo.repository.LiveStreamKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveStreamService {
    private final LiveStreamKeyRepository liveStreamKeyRepository;

    public LiveStreamKey getLiveStream(String userId, String username) {
        return liveStreamKeyRepository.findByUserId(userId)
                .orElseGet(() -> createLiveStream(userId, username));
    }

    private LiveStreamKey createLiveStream(String userId, String username) {
        LiveStreamKey liveStreamKey = LiveStreamKey.builder()
                .userId(userId)
                .username(username)
                .streamKey(UUID.randomUUID().toString())
                .isLive(false)
                .build();
        return liveStreamKeyRepository.save(liveStreamKey);
    }

    public LiveStreamKey updateLiveStream(String userId, String username, String title, String description) {
        LiveStreamKey liveStreamKey = getLiveStream(userId, username);
        liveStreamKey.setTitle(title);
        liveStreamKey.setDescription(description);
        return liveStreamKeyRepository.save(liveStreamKey);
    }

    public LiveStreamKey resetStreamKey(String userId, String username) {
        LiveStreamKey liveStreamKey = getLiveStream(userId, username);
        liveStreamKey.setStreamKey(UUID.randomUUID().toString());
        return liveStreamKeyRepository.save(liveStreamKey);
    }

    public Optional<LiveStreamKey> findByStreamKey(String streamKey) {
        return liveStreamKeyRepository.findByStreamKey(streamKey);
    }

    public Optional<LiveStreamKey> findByUsername(String username) {
        return liveStreamKeyRepository.findByUsername(username);
    }

    public boolean isUserLive(String username) {
        return liveStreamKeyRepository.findByUsername(username)
                .map(LiveStreamKey::isLive)
                .orElse(false);
    }
}
