package com.example.demo.service;

import com.example.demo.model.EUploadStatus;
import com.example.demo.model.EVideoResolution;
import com.example.demo.model.EVideoPrivacy;
import com.example.demo.model.UploadSession;
import com.example.demo.model.Video;
import com.example.demo.repository.UploadSessionRepository;
import com.example.demo.repository.VideoRepository;
import com.example.demo.util.FFmpegUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;

@Service
public class VideoService {

    @Value("${icurff.app.storage}")
    private String storageBaseDir;

    @Value("${icurff.app.location}")
    private String serverLocation;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Autowired
    private TaskPublisherService taskPublisherService;


    public String addNewVideo(String username, String title, Integer duration) {
        Video vid = new Video();
        vid.setUsername(username);
        vid.setTitle(title);
        vid.setDuration(duration);
        Set<String> initialLocations = new java.util.HashSet<>();
        initialLocations.add(serverLocation);
        vid.setServer_locations(initialLocations);
        vid.setPrivacy(EVideoPrivacy.PUBLIC);
        videoRepository.save(vid);
        return vid.getId();
    }


    public void saveChunk(String username, String sessionId, int chunkIndex, MultipartFile chunkFile) throws IOException {
        Path chunkDir = Path.of(storageBaseDir, "uploads", username, sessionId);
        Files.createDirectories(chunkDir);
        Path chunkPath = chunkDir.resolve(String.format("chunk_%06d", chunkIndex));
        InputStream in = chunkFile.getInputStream();
        Files.copy(in, chunkPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public void mergeChunks(String username, String sessionId, String fileName, String fileType, Long fileSize, Integer fileDuration) throws IOException {
        // Add new video
        String vidId =  addNewVideo(username, fileName, fileDuration);

        Path chunkDir = Path.of(storageBaseDir, "uploads", username, sessionId);
        Path outputVideoPath = Path.of(
                storageBaseDir, "outputs", username,"videos", vidId,"raw", fileName);
        Files.createDirectories(outputVideoPath.getParent());

        try (var out = Files.newOutputStream(outputVideoPath)) {
            Files.list(chunkDir)
                    .filter(p -> p.getFileName().toString().startsWith("chunk_"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(p -> {
                        try (var in = Files.newInputStream(p)) {
                            in.transferTo(out);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        UploadSession uploadSession = uploadSessionRepository.findById(sessionId).orElse(null);
        ;
        assert uploadSession != null;
        uploadSession.setFileName(fileName);
        uploadSession.setFileType(fileType);
        uploadSession.setFileSize(fileSize);
        uploadSession.setDuration(fileDuration);
        uploadSession.setStatus(EUploadStatus.COMPLETED);
        uploadSessionRepository.save(uploadSession);


        try {
            Files.walk(chunkDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // ignore cleanup errors
                        }
                    });
        } catch (IOException ignored) {

        }
        System.out.println(outputVideoPath.toAbsolutePath());
        int[] resolution = FFmpegUtil.getVideoResolution(outputVideoPath.toAbsolutePath().toString());

        // Generate thumbnail
        Path thumbnailPath = Path.of(storageBaseDir, "outputs", username,"videos", vidId, "thumbnail.jpg");
        Files.createDirectories(thumbnailPath.getParent());

        double thumbnailSecond = 1.0;
        if (fileDuration != null && fileDuration > 0) {
            // Pick a frame near the middle while staying within the video duration bounds
            double midPoint = fileDuration / 2.0;
            double maxAllowed = Math.max(fileDuration - 1, 0.5);
            thumbnailSecond = Math.max(0.5, Math.min(midPoint, maxAllowed));
        }

        try {
            FFmpegUtil.generateThumbnail(outputVideoPath.toAbsolutePath().toString(), thumbnailPath.toAbsolutePath().toString(), thumbnailSecond);

            videoRepository.findById(vidId).ifPresent(video -> {
                String normalizedServer = serverLocation;
                if (!normalizedServer.startsWith("http://") && !normalizedServer.startsWith("https://")) {
                    normalizedServer = "http://" + normalizedServer;
                }

                String thumbnailUrl = normalizedServer + "/videos/" + username + "/" + vidId + "/thumbnail.jpg";
                video.setThumbnail(thumbnailUrl);
                videoRepository.save(video);
            });
        } catch (Exception e) {
            System.err.println("Failed to generate thumbnail for video " + vidId + ": " + e.getMessage());
        }

        int width = resolution[0];
        int height = resolution[1];
        System.out.println(height);
        // Define standard resolutions
        int[] resolutionList = {1080, 720, 480, 360, 240};
        
        System.out.println("Input video height: " + height);
        
        for (int res : resolutionList) {
            System.out.println("Checking resolution: " + res + " against height: " + height);
            // Only generate resolutions that are less than or equal to the input video's height
            if (res <= height) {
                System.out.println("Queueing transcoding task for resolution: " + res);
                Path processedVideoPath = Path.of(
                        storageBaseDir, "outputs", username,"videos", vidId, String.valueOf(res));

                taskPublisherService.publishTranscodeTask(vidId, outputVideoPath.toString(), processedVideoPath.toString(), String.valueOf(res));
            } else {
                System.out.println("Skipping resolution " + res + " because it is higher than input height " + height);
            }
        }

    }

    public void updateVideoResolutionAndServer(String videoId, int resolution, String serverLocation) {
        System.out.println("=== updateVideoResolutionAndServer called ===");
        System.out.println("Video ID: " + videoId);
        System.out.println("Resolution: " + resolution);
        System.out.println("Server Location: " + serverLocation);

        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            System.err.println("ERROR: Video not found with id: " + videoId);
            return;
        }

        System.out.println("Video found! Current resolutions: " + video.getResolutions());
        System.out.println("Video found! Current server_locations: " + video.getServer_locations());

        // Add the resolution to the set
        EVideoResolution videoResolution = getResolutionEnum(resolution);
        if (videoResolution != null) {
            video.getResolutions().add(videoResolution);
            System.out.println("Added resolution: " + videoResolution);
        } else {
            System.err.println("ERROR: Could not convert resolution to enum: " + resolution);
        }

        // Add the server location to the set
        video.getServer_locations().add(serverLocation);
        System.out.println("Added server location: " + serverLocation);

        System.out.println("Before save - resolutions: " + video.getResolutions());
        System.out.println("Before save - server_locations: " + video.getServer_locations());

        Video savedVideo = videoRepository.save(video);

        System.out.println("After save - resolutions: " + savedVideo.getResolutions());
        System.out.println("After save - server_locations: " + savedVideo.getServer_locations());
        System.out.println("=== updateVideoResolutionAndServer completed ===");
    }

    private EVideoResolution getResolutionEnum(int resolution) {
        return switch (resolution) {
            case 240 -> EVideoResolution.P240;
            case 360 -> EVideoResolution.P360;
            case 480 -> EVideoResolution.P480;
            case 720 -> EVideoResolution.P720;
            case 1080 -> EVideoResolution.P1080;
            default -> null;
        };
    }

    public boolean deleteVideo(String username, String videoId) {
        Video video = videoRepository.findById(videoId).orElse(null);

        if (video != null && !video.getUsername().equals(username)) {
            return false;
        }

        Path videoDirectory = Path.of(storageBaseDir, "outputs", username,"videos", videoId);
        try {
            if (Files.exists(videoDirectory)) {
                Files.walk(videoDirectory)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to delete file: " + path, e);
                            }
                        });

                Path userDirectory = videoDirectory.getParent();
                if (userDirectory != null && Files.exists(userDirectory)) {
                    try (var entries = Files.list(userDirectory)) {
                        if (!entries.findAny().isPresent()) {
                            Files.deleteIfExists(userDirectory);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete video files", e);
        }

        if (video != null) {
            videoRepository.delete(video);
        }

        return true;
    }
}
