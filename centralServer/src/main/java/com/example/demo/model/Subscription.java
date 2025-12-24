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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "subscriptions")
@CompoundIndexes({
        @CompoundIndex(name = "subscriber_channel_idx", def = "{'subscriberId': 1, 'channelUsername': 1}", unique = true)
})
public class Subscription {
    @Id
    private String id;

    @Indexed
    private String subscriberId;

    private String subscriberUsername;

    @Indexed
    private String channelUsername;

    @CreatedDate
    private Instant subscribedAt;

    @LastModifiedDate
    private Instant lastModifiedDate;
}

