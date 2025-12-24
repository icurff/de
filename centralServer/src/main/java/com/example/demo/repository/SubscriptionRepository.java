package com.example.demo.repository;

import com.example.demo.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {
    Optional<Subscription> findBySubscriberIdAndChannelUsername(String subscriberId, String channelUsername);
    
    Boolean existsBySubscriberIdAndChannelUsername(String subscriberId, String channelUsername);
    
    List<Subscription> findBySubscriberId(String subscriberId);
    
    List<Subscription> findByChannelUsername(String channelUsername);
    
    Page<Subscription> findByChannelUsername(String channelUsername, Pageable pageable);
    
    Page<Subscription> findBySubscriberId(String subscriberId, Pageable pageable);
    
    Long countByChannelUsername(String channelUsername);
    
    Long countBySubscriberId(String subscriberId);
}

