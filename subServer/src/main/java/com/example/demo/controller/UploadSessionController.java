package com.example.demo.controller;


import com.example.demo.security.UserDetailsImpl;
import com.example.demo.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/uploads")
public class UploadSessionController {
    @Autowired
    private VideoService videoService;

    @PostMapping("/{sessionId}")
    public ResponseEntity<?> uploadChunk(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String sessionId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("file") MultipartFile chunk,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileType") String fileType,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam("fileDuration") Integer fileDuration
    ) {
        try {
            String username = userDetails.getUsername();
            videoService.saveChunk(username, sessionId, chunkIndex, chunk);
            if (chunkIndex == totalChunks - 1) {
                videoService.mergeChunks(username, sessionId, fileName, fileType, fileSize, fileDuration);
                return ResponseEntity.ok("ok");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.ok(Map.of("status", "ok", "chunkIndex", chunkIndex, "totalChunks", totalChunks));
    }


}

