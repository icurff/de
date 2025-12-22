package com.example.demo.repository;

import com.example.demo.model.Livestream;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LivestreamRepository extends MongoRepository<Livestream, String> {
    Optional<Livestream> findById(String id);
}
