package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "watch_history")
public class WatchHistory {
    @Id
    private String id;
    private String username;
    private String videoId;
    private Instant watchedAt;

    public WatchHistory() {
        this.watchedAt = Instant.now();
    }

    public WatchHistory(String username, String videoId) {
        this.username = username;
        this.videoId = videoId;
        this.watchedAt = Instant.now();
    }
}

