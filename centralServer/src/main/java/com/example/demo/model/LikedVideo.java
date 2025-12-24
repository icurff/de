package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "liked_videos")
public class LikedVideo {
    @Id
    private String id;
    private String username;
    private String videoId;
    private Instant likedAt;

    public LikedVideo() {
        this.likedAt = Instant.now();
    }

    public LikedVideo(String username, String videoId) {
        this.username = username;
        this.videoId = videoId;
        this.likedAt = Instant.now();
    }
}

