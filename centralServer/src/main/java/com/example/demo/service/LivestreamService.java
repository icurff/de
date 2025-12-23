package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.LivestreamKey;
import com.example.demo.model.Livestream;
import com.example.demo.repository.LivestreamKeyRepository;
import com.example.demo.repository.LivestreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LivestreamService {
    private final LivestreamKeyRepository liveStreamKeyRepository;
    private final LivestreamRepository livestreamRepository;

    @Autowired
    private SupabaseImageService supabaseImageService;

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

    public Livestream updateLivestreamMetadata(String livestreamId, String username, String title, String description) {
        Livestream livestream = getLivestreamById(livestreamId);
        if (!livestream.getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to update this livestream");
        }

        livestream.setTitle(title);
        livestream.setDescription(description);
        return livestreamRepository.save(livestream);
    }

    public Livestream updateLivestreamThumbnail(String livestreamId, String username, MultipartFile thumbnailFile) throws IOException {
        Livestream livestream = getLivestreamById(livestreamId);
        if (!livestream.getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to update this livestream");
        }

        // Delete old thumbnail from Supabase if exists
        if (livestream.getThumbnail() != null && !livestream.getThumbnail().isEmpty()) {
            try {
                String oldThumbnailPath = extractFilePathFromUrl(livestream.getThumbnail());
                if (oldThumbnailPath != null) {
                    supabaseImageService.deleteImage(oldThumbnailPath);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete old thumbnail: " + e.getMessage());
            }
        }

        // Upload new thumbnail
        String fileName = livestreamId + "_thumbnail";
        String folder = "thumbnails/livestreams/" + username;
        String thumbnailUrl = supabaseImageService.uploadImage(thumbnailFile, folder, fileName);
        
        livestream.setThumbnail(thumbnailUrl);
        return livestreamRepository.save(livestream);
    }

    private String extractFilePathFromUrl(String url) {
        if (url == null || !url.contains("/storage/v1/object/public/")) {
            return null;
        }
        String[] parts = url.split("/storage/v1/object/public/");
        if (parts.length < 2) {
            return null;
        }
        String[] pathParts = parts[1].split("/", 2);
        if (pathParts.length < 2) {
            return null;
        }
        return pathParts[1];
    }

}
