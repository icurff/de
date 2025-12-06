package com.example.demo.repository;

import com.example.demo.model.UploadSession;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UploadSessionRepository extends MongoRepository<UploadSession, String> {
}
