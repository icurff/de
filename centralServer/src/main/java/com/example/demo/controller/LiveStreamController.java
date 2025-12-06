package com.example.demo.controller;

import com.example.demo.model.LiveStream;
import com.example.demo.service.LiveStreamService;
import com.example.demo.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/livestream")
@RequiredArgsConstructor
public class LiveStreamController {
    private final LiveStreamService liveStreamService;

    @GetMapping
    public ResponseEntity<LiveStream> getLiveStream(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(liveStreamService.getLiveStream(userDetails.getId())); 
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<LiveStream> getLiveStreamByUserId(@PathVariable String userId) {
        LiveStream stream = liveStreamService.getLiveStream(userId);
        // Construct HLS URL (assuming default SRS config)
        // In a real app, this should be dynamic or stored
        String hlsUrl = "http://localhost:8088/live/" + stream.getStreamKey() + "_ff.m3u8";
        stream.setHlsUrl(hlsUrl);
        // We still mask the key itself so the API doesn't explicitly say "here is the secret key"
        // even though it's in the URL. It's a slight obfuscation.
        stream.setStreamKey(null);
        return ResponseEntity.ok(stream);
    }

    @PostMapping("/setup")
    public ResponseEntity<LiveStream> setupStream(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(liveStreamService.updateLiveStream(
                userDetails.getId(),
                payload.get("title"),
                payload.get("description")
        ));
    }

    @PostMapping("/reset-key")
    public ResponseEntity<LiveStream> resetKey(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(liveStreamService.resetStreamKey(userDetails.getId()));
    }
}
