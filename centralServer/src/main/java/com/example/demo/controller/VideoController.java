package com.example.demo.controller;

import com.example.demo.model.Video;
import com.example.demo.payload.request.video.UpdateVideoPrivacyRequest;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.service.VideoService;
import com.example.demo.service.WatchHistoryService;
import com.example.demo.service.LikedVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*", maxAge = 3600)
public class VideoController {
    @Autowired
    private VideoService videoService;

    @Autowired
    private WatchHistoryService watchHistoryService;

    @Autowired
    private LikedVideoService likedVideoService;

    @GetMapping("/{videoId}")
    public ResponseEntity<?> getVideo(@PathVariable String videoId,
                                      @AuthenticationPrincipal UserDetailsImpl user) {
        try {
            Video vid = videoService.getVideoForUser(videoId, user != null ? user.getUsername() : null);
            // Record watch history if user is authenticated
            if (user != null) {
                watchHistoryService.addToHistory(user.getUsername(), videoId);
            }
            return ResponseEntity.ok(vid);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @GetMapping("/{videoId}/stream")
    public ResponseEntity<?> streamRedirect(@PathVariable String videoId,
                                            @AuthenticationPrincipal UserDetailsImpl user) {
        try {
            Video vid = videoService.getVideoForUser(videoId, user != null ? user.getUsername() : null);
            String targetUrl = vid.getServer_locations().stream().findFirst()
                    .orElse(null);
            if (targetUrl == null || targetUrl.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No stream source available");
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", targetUrl)
                    .build();
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentVideos(
            @RequestParam(name = "limit", defaultValue = "12") int limit,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        return ResponseEntity.ok(videoService.getRecentVideosForUser(user.getUsername(), limit));
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable String videoId,
                                         @AuthenticationPrincipal UserDetailsImpl user) {
        try {
            videoService.deleteVideo(videoId, user.getUsername());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.getMessage());
        }
    }

    @PatchMapping("/{videoId}/privacy")
    public ResponseEntity<?> updatePrivacy(@PathVariable String videoId,
                                           @Valid @RequestBody UpdateVideoPrivacyRequest request,
                                           @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        try {
            Video updated = videoService.updateVideoPrivacy(videoId, user.getUsername(), request.getPrivacy());
            return ResponseEntity.ok(updated);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getPublicVideosByUsername(
            @PathVariable String username,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(videoService.getPublicVideosByUsername(username, limit));
    }

    @GetMapping("/public")
    public ResponseEntity<?> getAllPublicVideos(
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset
    ) {
        return ResponseEntity.ok(videoService.getAllPublicVideos(limit, offset));
    }

    @PostMapping("/{videoId}/thumbnail")
    public ResponseEntity<?> uploadThumbnail(@PathVariable String videoId,
                                            @RequestParam("file") MultipartFile file,
                                            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is required"));
        }

        try {
            Video updated = videoService.updateVideoThumbnail(videoId, user.getUsername(), file);
            return ResponseEntity.ok(updated);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload thumbnail: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        return ResponseEntity.ok(watchHistoryService.getHistoryForUser(user.getUsername(), limit));
    }

    @GetMapping("/liked")
    public ResponseEntity<?> getLikedVideos(
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        return ResponseEntity.ok(likedVideoService.getLikedVideosForUser(user.getUsername(), limit));
    }

    @PostMapping("/{videoId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable String videoId,
                                        @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        likedVideoService.toggleLike(user.getUsername(), videoId);
        return ResponseEntity.ok(Map.of("liked", likedVideoService.isLiked(user.getUsername(), videoId)));
    }

    @GetMapping("/{videoId}/like-info")
    public ResponseEntity<?> getLikeInfo(@PathVariable String videoId,
                                         @AuthenticationPrincipal UserDetailsImpl user) {
        String username = user != null ? user.getUsername() : null;
        return ResponseEntity.ok(likedVideoService.getLikeInfo(username, videoId));
    }
}
