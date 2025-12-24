package com.example.demo.repository;

import com.example.demo.model.LikedVideo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LikedVideoRepository extends MongoRepository<LikedVideo, String> {
    List<LikedVideo> findByUsernameOrderByLikedAtDesc(String username, Pageable pageable);
    Optional<LikedVideo> findByUsernameAndVideoId(String username, String videoId);
    void deleteByUsernameAndVideoId(String username, String videoId);
    boolean existsByUsernameAndVideoId(String username, String videoId);
    long countByVideoId(String videoId);
}

