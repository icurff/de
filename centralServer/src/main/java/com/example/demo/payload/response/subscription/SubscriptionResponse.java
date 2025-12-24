package com.example.demo.payload.response.subscription;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SubscriptionResponse {
    private String id;
    private String subscriberId;
    private String subscriberUsername;
    private String channelUsername;
    private Instant subscribedAt;
}

