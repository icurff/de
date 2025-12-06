package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Document(collection = "videos")
public class Video {
    @Id
    private String id;
    private String username;

    private String title;
    private String description="";
    private String thumbnail="";
    // second

    private Set<String> server_locations = new HashSet<>();
    private Set<EVideoResolution> resolutions = new HashSet<>();
    private EVideoPrivacy privacy = EVideoPrivacy.PUBLIC;
    @CreatedDate
    private Instant uploadedDate;
    @LastModifiedDate
    private Instant lastModifiedDate;

}
