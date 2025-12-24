package com.example.demo.service;


import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.EVideoPrivacy;
import com.example.demo.model.Video;
import com.example.demo.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class VideoService {
    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SupabaseImageService supabaseImageService;

    public Video getVideoById(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
    }

    public Video getVideoForUser(String videoId, String requesterUsername) {
        Video video = getVideoById(videoId);
        if (isPrivate(video) && !isOwner(video, requesterUsername)) {
            throw new AccessDeniedException("You do not have permission to view this video");
        }
        return video;
    }

    public void deleteVideo(String videoId, String username) {
        Video video = getVideoById(videoId);

        if (!video.getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to delete this video");
        }

        Set<String> serverLocations = new HashSet<>(video.getServer_locations());

        videoRepository.delete(video);
        if (serverLocations.isEmpty()) {
            return;
        }

        List<String> failedServers = new ArrayList<>();

        for (String serverLocation : serverLocations) {
            String baseUrl = normalizeServerLocation(serverLocation);
            if (baseUrl == null) {
                continue;
            }

            String taskUrl = baseUrl + "/api/tasks/delete";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("videoId", videoId);
            requestBody.put("username", username);

            try {
                restTemplate.postForEntity(taskUrl, requestBody, Void.class);
            } catch (RestClientException e) {
                failedServers.add(baseUrl);
            }
        }

        if (!failedServers.isEmpty()) {
            throw new IllegalStateException("Failed to dispatch delete task to: " + String.join(", ", failedServers));
        }
    }

    public List<Video> getRecentVideosForUser(String username, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "uploadedDate"));
        return videoRepository.findByUsernameOrderByUploadedDateDesc(username, pageable);
    }

    public List<Video> getPublicVideosByUsername(String username, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "uploadedDate"));
        return videoRepository.findByUsernameAndPrivacyOrderByUploadedDateDesc(username, EVideoPrivacy.PUBLIC, pageable);
    }

    public List<Video> getAllPublicVideos(int limit, int offset) {
        int page = Math.max(0, offset) / Math.max(1, limit);
        Pageable pageable = PageRequest.of(page, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "uploadedDate"));
        return videoRepository.findByPrivacyOrderByUploadedDateDesc(EVideoPrivacy.PUBLIC, pageable);
    }

    public Video updateVideoPrivacy(String videoId, String username, EVideoPrivacy privacy) {
        Video video = getVideoById(videoId);
        if (!isOwner(video, username)) {
            throw new AccessDeniedException("You do not have permission to update this video");
        }

        video.setPrivacy(privacy);
        return videoRepository.save(video);
    }

    private String normalizeServerLocation(String serverLocation) {
        if (serverLocation == null) {
            return null;
        }

        String normalized = serverLocation.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://" + normalized;
        }

        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private boolean isPrivate(Video video) {
        return video.getPrivacy() == EVideoPrivacy.PRIVATE;
    }

    private boolean isOwner(Video video, String requesterUsername) {
        return requesterUsername != null && requesterUsername.equals(video.getUsername());
    }

    public Video updateVideoThumbnail(String videoId, String username, MultipartFile thumbnailFile) throws IOException {
        Video video = getVideoById(videoId);
        if (!isOwner(video, username)) {
            throw new AccessDeniedException("You do not have permission to update this video");
        }

        // Delete old thumbnail from Supabase if exists
        if (video.getThumbnail() != null && !video.getThumbnail().isEmpty()) {
            try {
                String oldThumbnailPath = extractFilePathFromUrl(video.getThumbnail());
                if (oldThumbnailPath != null) {
                    supabaseImageService.deleteImage(oldThumbnailPath);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete old thumbnail: " + e.getMessage());
            }
        }

        // Upload new thumbnail
        String fileName = videoId + "_thumbnail";
        String folder = "thumbnails/videos/" + username;
        String thumbnailUrl = supabaseImageService.uploadImage(thumbnailFile, folder, fileName);
        
        video.setThumbnail(thumbnailUrl);
        return videoRepository.save(video);
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
