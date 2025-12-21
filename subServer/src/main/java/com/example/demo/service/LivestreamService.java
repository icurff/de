package com.example.demo.service;

import com.example.demo.model.EVideoPrivacy;
import com.example.demo.model.LiveStreamKey;
import com.example.demo.model.Livestream;
import com.example.demo.repository.LiveStreamKeyRepository;
import com.example.demo.repository.LivestreamRepository;
import com.example.demo.util.FFmpegUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivestreamService {
    private final LiveStreamKeyRepository liveStreamKeyRepository;
    private final LivestreamRepository livestreamRepository;
    

    private final Map<String, Instant> streamStartTimes = new ConcurrentHashMap<>();
    
    @Value("${icurff.app.location:localhost}")
    private String serverLocation;
    
    @Value("${icurff.app.storage:storage}")
    private String storageBaseDir;

 
    public LiveStreamKey getLiveStreamKey(String userId, String username) {
        return liveStreamKeyRepository.findByUserId(userId)
                .orElseGet(() -> createLiveStreamKey(userId, username));
    }

    private LiveStreamKey createLiveStreamKey(String userId, String username) {
        LiveStreamKey liveStreamKey = LiveStreamKey.builder()
                .userId(userId)
                .username(username)
                .streamKey(UUID.randomUUID().toString())
                .isLive(false)
                .build();
        return liveStreamKeyRepository.save(liveStreamKey);
    }

    /**
     * Update stream info (title, description)
     */
    public LiveStreamKey updateLiveStreamInfo(String userId, String username, String title, String description) {
        LiveStreamKey liveStreamKey = getLiveStreamKey(userId, username);
        liveStreamKey.setTitle(title);
        liveStreamKey.setDescription(description);
        return liveStreamKeyRepository.save(liveStreamKey);
    }

 
    public LiveStreamKey resetStreamKey(String userId, String username) {
        LiveStreamKey liveStreamKey = getLiveStreamKey(userId, username);
        liveStreamKey.setStreamKey(UUID.randomUUID().toString());
        return liveStreamKeyRepository.save(liveStreamKey);
    }

    public Optional<LiveStreamKey> validateStreamKey(String streamKey) {
        return liveStreamKeyRepository.findByStreamKey(streamKey);
    }


    public boolean handlePublish(String streamKey) {
        Optional<LiveStreamKey> optKey = liveStreamKeyRepository.findByStreamKey(streamKey);
        if (optKey.isEmpty()) {
            log.warn("Invalid stream key attempted: {}", streamKey);
            return false;
        }

        LiveStreamKey key = optKey.get();
        

        key.setLive(true);
        liveStreamKeyRepository.save(key);
        

        streamStartTimes.put(streamKey, Instant.now());
        
        log.info("Publish started for user: {} with stream key: {} at {}", key.getUsername(), streamKey, streamStartTimes.get(streamKey));
        return true;
    }

Remove start time from map (cleanup)
     */
    public void handleUnpublish(String streamKey) {
        Optional<LiveStreamKey> optKey = liveStreamKeyRepository.findByStreamKey(streamKey);
        if (optKey.isEmpty()) {
            log.warn("Unpublish for unknown stream key: {}", streamKey);
            return;
        }

        LiveStreamKey key = optKey.get();
        key.setLive(false);
        liveStreamKeyRepository.save(key);
        
        // Clean up start time (stream is finished)
        streamStartTimes.remove(streamKey);
        
        log.info("Unpublish completed for user: {}", key.getUsername());
    }


    public void handleDvr(String streamKey, String dvrPath) {
        Optional<LiveStreamKey> optKey = liveStreamKeyRepository.findByStreamKey(streamKey);
        if (optKey.isEmpty()) {
            log.warn("DVR for unknown stream key: {}", streamKey);
            return;
        }

        LiveStreamKey key = optKey.get();

        Instant startTime = streamStartTimes.getOrDefault(streamKey, Instant.now());
        

        Livestream livestream = Livestream.builder()
                .username(key.getUsername())
                .title(key.getTitle() != null ? key.getTitle() : "Live Stream")
                .description(key.getDescription() != null ? key.getDescription() : "")
                .thumbnail("")
                .duration(0) // Will be calculated after file is moved
                .serverLocation(serverLocation)
                .privacy(EVideoPrivacy.PUBLIC)
                .dvrPath(dvrPath) // Temporary path, will be updated after moving file
                .uploadedDate(startTime) // Time when live stream started, not when saved to DB
                .lastModifiedDate(Instant.now())
                .build();
        
        livestreamRepository.save(livestream);
        String livestreamId = livestream.getId();
    
        String fileName = Path.of(dvrPath).getFileName().toString();
        
        // Build new path: storageBaseDir/outputs/username/livestream/livestreamId/fileName
        Path newDvrPath = Path.of(storageBaseDir, "outputs", key.getUsername(), "livestreams", livestreamId, fileName);
        
        try {
            // Create parent directories
            Files.createDirectories(newDvrPath.getParent());
            
      
            // SRS path: /usr/local/srs/objs/nginx/html/live/[app]/[stream].[timestamp].mp4
            // On host: ./storage/srs/[app]/[stream].[timestamp].mp4
            // From backend: storageBaseDir/srs/[app]/[stream].[timestamp].mp4
            Path originalPath;
            if (dvrPath.startsWith("/usr/local/srs/objs/nginx/html/live/")) {
                // Extract relative path from SRS container path
                String relativePath = dvrPath.substring("/usr/local/srs/objs/nginx/html/live/".length());
                originalPath = Path.of(storageBaseDir, "srs", relativePath);
            } else {
                // If path is already relative or in different format, try to use as-is
                originalPath = Path.of(dvrPath);
            }
            
            // Move file to new location
            if (Files.exists(originalPath)) {
                Files.move(originalPath, newDvrPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("DVR file moved from {} to {}", originalPath, newDvrPath);
            } else {
                log.warn("DVR file not found at original path: {}, trying absolute path", originalPath);
                // Try absolute path as fallback
                Path absolutePath = Path.of(dvrPath);
                if (Files.exists(absolutePath)) {
                    Files.move(absolutePath, newDvrPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("DVR file moved from {} to {}", absolutePath, newDvrPath);
                } else {
                    log.error("DVR file not found at either {} or {}", originalPath, absolutePath);
                    // Keep original path in database if file move fails
                    return;
                }
            }
            
            // Calculate duration from the moved file
            Integer duration = 0;
            try {
                duration = FFmpegUtil.getVideoDuration(newDvrPath.toString());
                log.info("Calculated duration for livestream {}: {} seconds", livestreamId, duration);
            } catch (Exception e) {
                log.warn("Failed to calculate duration for livestream {}: {}", livestreamId, e.getMessage());
                // Keep duration as 0 if calculation fails
            }
            

            livestream.setDvrPath(newDvrPath.toString());
            livestream.setDuration(duration);
            livestreamRepository.save(livestream);
    
            streamStartTimes.remove(streamKey);
            
            log.info("DVR record created for user: {} with livestreamId: {}, duration: {}s, and new path: {}", 
                    key.getUsername(), livestreamId, duration, newDvrPath);
        } catch (IOException e) {
            log.error("Failed to move DVR file from {} to {}: {}", dvrPath, newDvrPath, e.getMessage(), e);

            streamStartTimes.remove(streamKey);
        }
    }


    public Optional<LiveStreamKey> getLiveStreamKeyByUsername(String username) {
        return liveStreamKeyRepository.findByUsername(username);
    }


    public boolean isUserLive(String username) {
        return liveStreamKeyRepository.findByUsername(username)
                .map(LiveStreamKey::isLive)
                .orElse(false);
    }
}
