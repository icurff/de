package com.example.demo.repository;

import com.example.demo.model.LiveStream;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface LiveStreamRepository extends MongoRepository<LiveStream, String> {
    Optional<LiveStream> findByUserId(String userId);
    Optional<LiveStream> findByStreamKey(String streamKey);
}
