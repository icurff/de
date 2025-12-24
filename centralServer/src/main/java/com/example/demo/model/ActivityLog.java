package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "activity_logs")
public class ActivityLog {
    @Id
    private String id;
    
    private String action; // UPLOAD_REQUEST, LIVESTREAM_START, LIVESTREAM_END, VIDEO_DELETE, etc.
    private String serverName;
    private String serverIp;
    private String username;
    private String details; // Additional details like video title, etc.
    
    @CreatedDate
    private Instant createdAt;
}
