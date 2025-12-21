package com.example.demo.controller;

import com.example.demo.model.LiveStreamKey;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.service.LivestreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/livestream")
@RequiredArgsConstructor
public class LivestreamController {
    private final LivestreamService livestreamService;

    /**
     * Get current user's stream key and info
     */
    @GetMapping
    public ResponseEntity<LiveStreamKey> getLiveStream(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(livestreamService.getLiveStreamKey(userDetails.getId(), userDetails.getUsername()));
    }

    /**
     * Get live stream info by username for public viewing
     * Returns stream info with FLV URL if user is live
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getLiveStreamByUsername(@PathVariable String username) {
        Optional<LiveStreamKey> optKey = livestreamService.getLiveStreamKeyByUsername(username);
        
        if (optKey.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "isLive", false,
                "username", username,
                "message", "User not found"
            ));
        }
        
        LiveStreamKey streamKey = optKey.get();
        
        if (!streamKey.isLive()) {
            return ResponseEntity.ok(Map.of(
                "isLive", false,
                "username", username,
                "message", "User is not currently streaming"
            ));
        }
        
        // Build FLV URL for live playback
        String flvUrl = "http://localhost:18088/live/" + streamKey.getStreamKey() + ".flv";

        Map<String, Object> response = new HashMap<>();
        response.put("id", streamKey.getId());
        response.put("username", streamKey.getUsername());
        response.put("title", streamKey.getTitle() != null ? streamKey.getTitle() : "");
        response.put("description", streamKey.getDescription() != null ? streamKey.getDescription() : "");
        response.put("isLive", streamKey.isLive());
        response.put("hlsUrl", flvUrl);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a user is currently live
     */
    @GetMapping("/status/{username}")
    public ResponseEntity<Map<String, Object>> checkLiveStatus(@PathVariable String username) {
        boolean isLive = livestreamService.isUserLive(username);
        return ResponseEntity.ok(Map.of(
            "username", username,
            "isLive", isLive
        ));
    }

    /**
     * Setup/update stream info (title, description)
     */
    @PostMapping("/setup")
    public ResponseEntity<LiveStreamKey> setupStream(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(livestreamService.updateLiveStreamInfo(
                userDetails.getId(),
                userDetails.getUsername(),
                payload.get("title"),
                payload.get("description")
        ));
    }

    /**
     * Reset stream key
     */
    @PostMapping("/reset-key")
    public ResponseEntity<LiveStreamKey> resetKey(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(livestreamService.resetStreamKey(userDetails.getId(), userDetails.getUsername()));
    }
}
