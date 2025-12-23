package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.LivestreamKey;
import com.example.demo.model.Livestream;
import com.example.demo.model.Server;
import com.example.demo.payload.response.livestream.LivestreamSetupResponse;
import com.example.demo.repository.LivestreamRepository;
import com.example.demo.service.LivestreamService;
import com.example.demo.service.LoadBalancingService;
import com.example.demo.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/livestream")
@RequiredArgsConstructor
public class LivestreamController {
    private final LivestreamService liveStreamService;
    private final LoadBalancingService loadBalancingService;
    private final LivestreamRepository livestreamRepository;

    @GetMapping
    public ResponseEntity<?> getLiveStream(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        LivestreamKey stream = liveStreamService.getLiveStream(userDetails.getId(), userDetails.getUsername());

        if (stream.isLive()) {
            Optional<String> streamEndpoint = liveStreamService.getLivestreamEndpoint(userDetails.getUsername());
            if (streamEndpoint.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", stream.getId());
                response.put("userId", stream.getUserId());
                response.put("username", stream.getUsername());
                response.put("title", stream.getTitle());
                response.put("description", stream.getDescription());
                response.put("streamKey", stream.getStreamKey());
                response.put("isLive", stream.isLive());
                response.put("streamEndpoint", streamEndpoint.get());
                return ResponseEntity.ok(response);
            }
        }
        
        return ResponseEntity.ok(stream); 
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getLiveStreamByUsername(@PathVariable String username) {
        Optional<LivestreamKey> optStream = liveStreamService.findByUsername(username);
        
        if (optStream.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "isLive", false,
                "username", username,
                "message", "User not found"
            ));
        }
        
        LivestreamKey stream = optStream.get();
        
        if (!stream.isLive()) {
            return ResponseEntity.ok(Map.of(
                "isLive", false,
                "username", username,
                "message", "User is not currently streaming"
            ));
        }

        Optional<String> streamEndpoint = liveStreamService.getLivestreamEndpoint(username);
        
        if (streamEndpoint.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "isLive", false,
                "username", username,
                "message", "Unable to get stream URL"
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", stream.getId());
        response.put("username", stream.getUsername());
        response.put("title", stream.getTitle() != null ? stream.getTitle() : "");
        response.put("description", stream.getDescription() != null ? stream.getDescription() : "");
        response.put("isLive", stream.isLive());
        response.put("streamEndpoint", streamEndpoint.get());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{username}")
    public ResponseEntity<Map<String, Object>> checkLiveStatus(@PathVariable String username) {
        boolean isLive = liveStreamService.isUserLive(username);
        return ResponseEntity.ok(Map.of(
            "username", username,
            "isLive", isLive
        ));
    }


    @GetMapping("/stream-info")
    public ResponseEntity<?> getStreamInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            LivestreamKey livestreamKey = liveStreamService.getLiveStream(
                    userDetails.getId(), 
                    userDetails.getUsername()
            );

            String serverIp = null;
            String livestreamId = livestreamKey.getCurrentLivestreamId();
            if (livestreamId != null && !livestreamId.isBlank()) {
                Optional<Livestream> optLivestream = livestreamRepository.findById(livestreamId);
                if (optLivestream.isPresent()) {
                    Livestream livestream = optLivestream.get();
                    String existingServerIp = livestream.getServerLocation();
                    if (existingServerIp != null && !existingServerIp.isBlank()) {
                        serverIp = existingServerIp;
                    }
                }
            }


            if (serverIp == null) {
                Server selectedServer = loadBalancingService.getBestAvailableServer();
                serverIp = selectedServer.getIp();
            }

            LivestreamSetupResponse response = new LivestreamSetupResponse(livestreamKey, serverIp);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No available servers to handle the livestream request. Please try again later.");
        }
    }


    @PostMapping("/setup")
    public ResponseEntity<?> updateLiveInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody Map<String, String> payload) {
        try {

            String title = payload.get("title");
            String description = payload.get("description");

            LivestreamKey livestreamKey = liveStreamService.updateLiveStream(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    title,
                    description
            );

            Map<String, Object> response = new HashMap<>();
            response.put("id", livestreamKey.getId());
            response.put("userId", livestreamKey.getUserId());
            response.put("username", livestreamKey.getUsername());
            response.put("title", livestreamKey.getTitle());
            response.put("description", livestreamKey.getDescription());
            response.put("streamKey", livestreamKey.getStreamKey());
            response.put("isLive", livestreamKey.isLive());
            response.put("currentLivestreamId", livestreamKey.getCurrentLivestreamId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating live stream info: " + e.getMessage());
        }
    }

    @PostMapping("/reset-key")
    public ResponseEntity<LivestreamKey> resetKey(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(liveStreamService.resetStreamKey(userDetails.getId(), userDetails.getUsername()));
    }

    @GetMapping("/user/{username}/recordings")
    public ResponseEntity<?> getLivestreamRecordingsByUsername(
            @PathVariable String username,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(liveStreamService.getLivestreamsByUsername(username, limit));
    }

    @GetMapping("/{livestreamId}")
    public ResponseEntity<?> getLivestreamById(@PathVariable String livestreamId) {
        Optional<Livestream> optLivestream = livestreamRepository.findById(livestreamId);
        if (optLivestream.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Livestream not found");
        }
        return ResponseEntity.ok(optLivestream.get());
    }

    @DeleteMapping("/{livestreamId}")
    public ResponseEntity<?> deleteLivestream(
            @PathVariable String livestreamId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        try {
            liveStreamService.deleteLivestream(livestreamId, user.getUsername());
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @PostMapping("/{livestreamId}/thumbnail")
    public ResponseEntity<?> uploadThumbnail(@PathVariable String livestreamId,
                                            @RequestParam("file") MultipartFile file,
                                            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is required"));
        }

        try {
            Livestream updated = liveStreamService.updateLivestreamThumbnail(livestreamId, user.getUsername(), file);
            return ResponseEntity.ok(updated);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload thumbnail: " + e.getMessage()));
        }
    }

    @PatchMapping("/{livestreamId}")
    public ResponseEntity<?> updateLivestreamMetadata(@PathVariable String livestreamId,
                                                      @RequestBody Map<String, String> payload,
                                                      @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            String title = payload.get("title");
            String description = payload.get("description");

            Livestream updated = liveStreamService.updateLivestreamMetadata(
                    livestreamId,
                    user.getUsername(),
                    title != null ? title : "",
                    description != null ? description : ""
            );
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update livestream: " + e.getMessage()));
        }
    }
}
