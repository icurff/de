package com.example.demo.controller;

import com.example.demo.model.Video;
import com.example.demo.payload.request.video.UpdateVideoPrivacyRequest;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*", maxAge = 3600)
public class VideoController {
    @Autowired
    private VideoService videoService;

    @GetMapping("/{videoId}")
    public ResponseEntity<?> getVideo(@PathVariable String videoId,
                                      @AuthenticationPrincipal UserDetailsImpl user) {
        try {
            Video vid = videoService.getVideoForUser(videoId, user != null ? user.getUsername() : null);
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
}
