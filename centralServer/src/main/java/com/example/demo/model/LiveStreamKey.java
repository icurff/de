package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "livestream_keys")
public class LiveStreamKey {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    private String username;
    private String title;
    private String description;
    
    @Indexed(unique = true)
    private String streamKey;
    
    @JsonProperty("isLive")
    private boolean isLive;
}
