package com.example.demo.repository;

import com.example.demo.model.LiveStreamKey;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LiveStreamKeyRepository extends MongoRepository<LiveStreamKey, String> {
    Optional<LiveStreamKey> findByUserId(String userId);
    Optional<LiveStreamKey> findByUsername(String username);
    Optional<LiveStreamKey> findByStreamKey(String streamKey);
    boolean existsByStreamKey(String streamKey);
}
