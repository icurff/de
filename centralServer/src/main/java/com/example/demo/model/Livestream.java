package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "livestreams")
public class Livestream {
    @Id
    private String id;
    private String username;
    private String title;
    private String description;
    private String thumbnail;
    private Integer duration;
    private String serverLocation;
    private EVideoPrivacy privacy;
    // DVR recording path
    private String dvrPath;

    @CreatedDate
    private Instant uploadedDate;

    @LastModifiedDate
    private Instant lastModifiedDate;
}

