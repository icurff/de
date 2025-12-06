package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "comments")
@CompoundIndexes({
        @CompoundIndex(name = "video_parent_idx", def = "{'videoId': 1, 'parentCommentId': 1}"),
        @CompoundIndex(name = "parent_created_idx", def = "{'parentCommentId': 1, 'createdAt': 1}"),
})
public class Comment {
    @Id
    private String id;

    @Indexed
    private String videoId;

    private String parentCommentId;

    @Indexed
    private String userId;

    private String username;

    private String content;

    @Builder.Default
    private Set<String> likedUserIds = new HashSet<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}



