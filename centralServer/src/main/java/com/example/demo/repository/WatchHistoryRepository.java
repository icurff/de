package com.example.demo.repository;

import com.example.demo.model.WatchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WatchHistoryRepository extends MongoRepository<WatchHistory, String> {
    List<WatchHistory> findByUsernameOrderByWatchedAtDesc(String username, Pageable pageable);
    Optional<WatchHistory> findByUsernameAndVideoId(String username, String videoId);
    void deleteByUsernameAndVideoId(String username, String videoId);
}

