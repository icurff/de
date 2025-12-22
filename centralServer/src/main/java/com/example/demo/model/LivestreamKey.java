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
public class LivestreamKey {
    @Id
    private String id;
    @Indexed(unique = true)
    private String userId;
    private String username;
    private String title = "Livestream";
    private String description= "Wellcome to my livestream";
    @Indexed(unique = true)
    private String streamKey;
    @JsonProperty("isLive")
    private boolean isLive;
    private String currentLivestreamId;
}
