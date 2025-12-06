package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "livestreams")
public class LiveStream {
    @Id
    private String id;
    
    private String userId;
    private String title;
    private String description;
    private String streamKey;
    private boolean isLive;
    private String hlsUrl;
}
