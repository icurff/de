package com.example.demo.payload.response.subscription;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionStatsResponse {
    private String channelUsername;
    private Long subscriberCount;
    private Boolean isSubscribed;
}

