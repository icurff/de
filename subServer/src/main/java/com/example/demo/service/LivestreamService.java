package com.example.demo.service;

import com.example.demo.model.EVideoPrivacy;
import com.example.demo.model.LivestreamKey;
import com.example.demo.model.Livestream;
import com.example.demo.repository.LivestreamKeyRepository;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivestreamService {
    private final LivestreamKeyRepository liveStreamKeyRepository;
    private final LivestreamRepository livestreamRepository;

    @Value("${icurff.app.location:localhost}")
    private String serverLocation;

    @Value("${icurff.app.storage:storage}")
    private String storageBaseDir;

    public boolean handlePublish(String streamKey) {
        return liveStreamKeyRepository.findByStreamKey(streamKey)
                .map(key -> {
                    Livestream livestream = livestreamRepository.save(
                            Livestream.builder()
                                    .username(key.getUsername())
                                    .title(Optional.ofNullable(key.getTitle()).orElse("Live Stream"))
                                    .description(Optional.ofNullable(key.getDescription()).orElse(""))
                                    .thumbnail("")
                                    .duration(0)
                                    .serverLocation(serverLocation)
                                    .privacy(EVideoPrivacy.PUBLIC)
                                    .dvrPath("")
                                    .build()
                    );
                    key.setCurrentLivestreamId(livestream.getId());
                    key.setLive(true);
                    liveStreamKeyRepository.save(key);
                    return true;
                })
                .orElse(false);
    }


    public void handleUnpublish(String streamKey) {
        liveStreamKeyRepository.findByStreamKey(streamKey)
                .ifPresent(key -> {
                    key.setLive(false);
                    key.setCurrentLivestreamId(null);
                    liveStreamKeyRepository.save(key);
                });
    }


    public void handleDvr(String streamKey, String dvrPath) {

        Optional<LivestreamKey> optKey = liveStreamKeyRepository.findByStreamKey(streamKey);
        if (optKey.isEmpty()) {
            log.warn("Stream key not found: {}", streamKey);
            return;
        }

        LivestreamKey key = optKey.get();
        String livestreamId = key.getCurrentLivestreamId();
        if (livestreamId == null || livestreamId.isBlank()) {
            log.warn("No active livestream for stream key: {}", streamKey);
            return;
        }
        Optional<Livestream> optLivestream = livestreamRepository.findById(livestreamId);
        if (optLivestream.isEmpty()) {
            log.warn("Livestream not found: {}", livestreamId);
            return;
        }
        Livestream livestream = optLivestream.get();
        String fileName = Path.of(dvrPath).getFileName().toString();

        Path outputPath = Path.of(
                storageBaseDir,
                "outputs",
                key.getUsername(),
                "livestreams",
                livestreamId,
                fileName
        );

        try {
            Files.createDirectories(outputPath.getParent());
            Path originalPath = resolveOriginalDvrPath(dvrPath);
            if (originalPath == null) {
                log.warn("DVR file not found: {}", dvrPath);
                return;
            }
            Files.move(originalPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
            int duration = 0;
            try {
                duration = FFmpegUtil.getVideoDuration(outputPath.toString());
            } catch (Exception e) {
                log.warn("Failed to get duration for {}", outputPath);
            }
            livestream.setDvrPath(outputPath.toString());
            livestream.setDuration(duration);
            livestreamRepository.save(livestream);
        } catch (IOException e) {
            log.error("Error handling DVR for stream key: {}", streamKey, e);
        }
    }

    private static final String SRS_LIVE_ROOT =
            "/usr/local/srs/objs/nginx/html/live/";

    private Path resolveOriginalDvrPath(String dvrPath) {

        if (dvrPath.startsWith(SRS_LIVE_ROOT)) {
            String relativePath = dvrPath.substring(SRS_LIVE_ROOT.length());
            Path path = Path.of(storageBaseDir, "srs", relativePath);
            return Files.exists(path) ? path : null;
        }

        Path path = Path.of(dvrPath);
        return Files.exists(path) ? path : null;
    }

}
