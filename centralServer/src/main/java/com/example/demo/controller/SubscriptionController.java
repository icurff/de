package com.example.demo.controller;

import com.example.demo.payload.response.subscription.SubscriptionResponse;
import com.example.demo.payload.response.subscription.SubscriptionStatsResponse;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping("/channels/{channelUsername}")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @PathVariable String channelUsername,
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(subscriptionService.subscribe(channelUsername, user));
    }

    @DeleteMapping("/channels/{channelUsername}")
    public ResponseEntity<?> unsubscribe(
            @PathVariable String channelUsername,
            @AuthenticationPrincipal UserDetailsImpl user) {
        subscriptionService.unsubscribe(channelUsername, user);
        return ResponseEntity.ok(Map.of("message", "Đã hủy đăng ký thành công"));
    }

    @GetMapping("/channels/{channelUsername}/stats")
    public ResponseEntity<SubscriptionStatsResponse> getSubscriptionStats(
            @PathVariable String channelUsername,
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionStats(channelUsername, user));
    }

    @GetMapping("/channels/{channelUsername}/subscribers")
    public ResponseEntity<?> getSubscribers(
            @PathVariable String channelUsername,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SubscriptionResponse> subscribers = subscriptionService.getSubscribers(channelUsername, page, size);
        return ResponseEntity.ok(Map.of(
                "content", subscribers.getContent(),
                "page", subscribers.getNumber(),
                "size", subscribers.getSize(),
                "totalElements", subscribers.getTotalElements(),
                "totalPages", subscribers.getTotalPages()
        ));
    }

    @GetMapping("/my-subscriptions")
    public ResponseEntity<?> getMySubscriptions(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        Page<SubscriptionResponse> subscriptions = subscriptionService.getSubscriptions(user.getId(), page, size);
        return ResponseEntity.ok(Map.of(
                "content", subscriptions.getContent(),
                "page", subscriptions.getNumber(),
                "size", subscriptions.getSize(),
                "totalElements", subscriptions.getTotalElements(),
                "totalPages", subscriptions.getTotalPages()
        ));
    }
}

