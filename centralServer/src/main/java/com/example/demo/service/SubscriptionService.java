package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ResourceTakenException;
import com.example.demo.model.Subscription;
import com.example.demo.model.User;
import com.example.demo.payload.response.subscription.SubscriptionResponse;
import com.example.demo.payload.response.subscription.SubscriptionStatsResponse;
import com.example.demo.repository.SubscriptionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    public SubscriptionResponse subscribe(String channelUsername, UserDetailsImpl subscriber) {
        if (subscriber == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để đăng ký kênh");
        }

        // Check if user is trying to subscribe to themselves
        if (subscriber.getUsername().equals(channelUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn không thể đăng ký kênh của chính mình");
        }

        // Check if channel exists
        User channelOwner = userRepository.findByUsername(channelUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Kênh không tồn tại"));

        // Check if already subscribed
        if (subscriptionRepository.existsBySubscriberIdAndChannelUsername(subscriber.getId(), channelUsername)) {
            throw new ResourceTakenException("Bạn đã đăng ký kênh này rồi");
        }

        Subscription subscription = Subscription.builder()
                .subscriberId(subscriber.getId())
                .subscriberUsername(subscriber.getUsername())
                .channelUsername(channelUsername)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        return toResponse(saved);
    }

    public void unsubscribe(String channelUsername, UserDetailsImpl subscriber) {
        if (subscriber == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để hủy đăng ký");
        }

        Subscription subscription = subscriptionRepository
                .findBySubscriberIdAndChannelUsername(subscriber.getId(), channelUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa đăng ký kênh này"));

        subscriptionRepository.delete(subscription);
    }

    public boolean isSubscribed(String channelUsername, UserDetailsImpl subscriber) {
        if (subscriber == null) {
            return false;
        }
        return subscriptionRepository.existsBySubscriberIdAndChannelUsername(subscriber.getId(), channelUsername);
    }

    public SubscriptionStatsResponse getSubscriptionStats(String channelUsername) {
        Long subscriberCount = subscriptionRepository.countByChannelUsername(channelUsername);
        return SubscriptionStatsResponse.builder()
                .channelUsername(channelUsername)
                .subscriberCount(subscriberCount)
                .build();
    }

    public SubscriptionStatsResponse getSubscriptionStats(String channelUsername, UserDetailsImpl currentUser) {
        Long subscriberCount = subscriptionRepository.countByChannelUsername(channelUsername);
        boolean isSubscribed = isSubscribed(channelUsername, currentUser);
        
        return SubscriptionStatsResponse.builder()
                .channelUsername(channelUsername)
                .subscriberCount(subscriberCount)
                .isSubscribed(isSubscribed)
                .build();
    }

    public Page<SubscriptionResponse> getSubscribers(String channelUsername, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), 
                Sort.by(Sort.Direction.DESC, "subscribedAt"));
        Page<Subscription> subscriptionPage = subscriptionRepository.findByChannelUsername(channelUsername, pageable);
        
        return subscriptionPage.map(this::toResponse);
    }

    public Page<SubscriptionResponse> getSubscriptions(String subscriberId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), 
                Sort.by(Sort.Direction.DESC, "subscribedAt"));
        Page<Subscription> subscriptionPage = subscriptionRepository.findBySubscriberId(subscriberId, pageable);
        
        return subscriptionPage.map(this::toResponse);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .subscriberId(subscription.getSubscriberId())
                .subscriberUsername(subscription.getSubscriberUsername())
                .channelUsername(subscription.getChannelUsername())
                .subscribedAt(subscription.getSubscribedAt())
                .build();
    }
}

