package com.example.demo.repository;

import com.example.demo.model.LivestreamKey;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface LivestreamKeyRepository extends MongoRepository<LivestreamKey, String> {
    Optional<LivestreamKey> findByUserId(String userId);
    Optional<LivestreamKey> findByUsername(String username);
    Optional<LivestreamKey> findByStreamKey(String streamKey);
    long countByIsLive(boolean isLive);
}
