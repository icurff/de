package com.example.demo.controller;

import com.example.demo.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*", maxAge = 3600)
public class VideoController {

    @Autowired
    private VideoService videoService;

    @DeleteMapping("/{username}/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable String username,
                                         @PathVariable String videoId) {
        boolean deleted = videoService.deleteVideo(username, videoId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}

