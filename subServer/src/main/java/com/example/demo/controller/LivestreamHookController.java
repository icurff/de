package com.example.demo.controller;

import com.example.demo.service.LivestreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller to handle SRS HTTP hooks
 */
@Slf4j
@RestController
@RequestMapping("/api/livestreams/hooks")
@RequiredArgsConstructor
public class LivestreamHookController {
    private final LivestreamService livestreamService;


    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> onPublish(@RequestBody Map<String, Object> payload) {
        log.info("on_publish hook received: {}", payload);
        
        String streamKey = (String) payload.get("stream");
        if (streamKey == null || streamKey.isEmpty()) {
            log.warn("No stream key in publish request");
            return ResponseEntity.ok(Map.of("code", 1, "msg", "No stream key provided"));
        }

        boolean success = livestreamService.handlePublish(streamKey);
        if (!success) {
            log.warn("Invalid stream key: {}", streamKey);
            return ResponseEntity.ok(Map.of("code", 1, "msg", "Invalid stream key"));
        }

        log.info("Publish authorized for stream key: {}", streamKey);
        return ResponseEntity.ok(Map.of("code", 0, "msg", "ok"));
    }

    @PostMapping("/unpublish")
    public ResponseEntity<Map<String, Object>> onUnpublish(@RequestBody Map<String, Object> payload) {
        log.info("on_unpublish hook received: {}", payload);
        
        String streamKey = (String) payload.get("stream");
        if (streamKey != null && !streamKey.isEmpty()) {
            livestreamService.handleUnpublish(streamKey);
        }

        return ResponseEntity.ok(Map.of("code", 0, "msg", "ok"));
    }

    @PostMapping("/dvr")
    public ResponseEntity<Map<String, Object>> onDvr(@RequestBody Map<String, Object> payload) {
        log.info("on_dvr hook received: {}", payload);
        
        String streamKey = (String) payload.get("stream");
        String dvrPath = (String) payload.get("file");
        
        if (streamKey != null && dvrPath != null) {
            livestreamService.handleDvr(streamKey, dvrPath);
        }

        return ResponseEntity.ok(Map.of("code", 0, "msg", "ok"));
    }
}
