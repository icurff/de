package com.example.demo.repository;

import com.example.demo.model.EUploadStatus;
import com.example.demo.model.UploadSession;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UploadSessionRepository extends MongoRepository<UploadSession, String> {
}
