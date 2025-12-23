package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.LivestreamKey;
import com.example.demo.model.Livestream;
import com.example.demo.repository.LivestreamKeyRepository;
import com.example.demo.repository.LivestreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LivestreamService {
    private final LivestreamKeyRepository liveStreamKeyRepository;
    private final LivestreamRepository livestreamRepository;

    public LivestreamKey getLiveStream(String userId, String username) {
        return liveStreamKeyRepository.findByUserId(userId)
                .orElseGet(() -> createLiveStream(userId, username));
    }

    private LivestreamKey createLiveStream(String userId, String username) {
        LivestreamKey liveStreamKey = LivestreamKey.builder()
                .userId(userId)
                .username(username)
                .streamKey(UUID.randomUUID().toString())
                .isLive(false)
                .build();
        return liveStreamKeyRepository.save(liveStreamKey);
    }

    public LivestreamKey updateLiveStream(String userId, String username, String title, String description) {
        LivestreamKey liveStreamKey = getLiveStream(userId, username);
        liveStreamKey.setTitle(title);
        liveStreamKey.setDescription(description);
        return liveStreamKeyRepository.save(liveStreamKey);
    }

    public LivestreamKey resetStreamKey(String userId, String username) {
        LivestreamKey liveStreamKey = getLiveStream(userId, username);
        liveStreamKey.setStreamKey(UUID.randomUUID().toString());
        return liveStreamKeyRepository.save(liveStreamKey);
    }

    public Optional<LivestreamKey> findByUsername(String username) {
        return liveStreamKeyRepository.findByUsername(username);
    }

    public boolean isUserLive(String username) {
        return liveStreamKeyRepository.findByUsername(username)
                .map(LivestreamKey::isLive)
                .orElse(false);
    }

    public Optional<String> getLivestreamEndpoint(String username) {

        Optional<LivestreamKey> optKey = liveStreamKeyRepository.findByUsername(username);
        if (optKey.isEmpty() || !optKey.get().isLive()) {
            return Optional.empty();
        }

        LivestreamKey key = optKey.get();

        String livestreamId = key.getCurrentLivestreamId();
        if (livestreamId == null || livestreamId.isBlank()) {
            return Optional.empty();
        }

        Optional<Livestream> optLivestream = livestreamRepository.findById(livestreamId);
        if (optLivestream.isEmpty()) {
            return Optional.empty();
        }

        Livestream livestream = optLivestream.get();

        String serverIp = livestream.getServerLocation();
        if (serverIp == null || serverIp.isBlank()) {
            return Optional.empty();
        }

        String streamEndpoint =
                "http://" + serverIp + "/live/" + key.getStreamKey() + ".flv";

        return Optional.of(streamEndpoint);
    }

    public List<Livestream> getLivestreamsByUsername(String username, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "uploadedDate"));
        return livestreamRepository.findByUsernameAndDvrPathIsNotNullOrderByUploadedDateDesc(username, pageable);
    }

    public void deleteLivestream(String livestreamId, String username) {
        Optional<Livestream> optLivestream = livestreamRepository.findById(livestreamId);
        if (optLivestream.isEmpty()) {
            throw new ResourceNotFoundException("Livestream not found");
        }
        
        Livestream livestream = optLivestream.get();
        if (!livestream.getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to delete this livestream");
        }
        
        livestreamRepository.delete(livestream);
    }

    public Livestream getLivestreamById(String livestreamId) {
        return livestreamRepository.findById(livestreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Livestream not found"));
    }

}
