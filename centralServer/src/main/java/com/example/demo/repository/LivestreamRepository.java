package com.example.demo.repository;

import com.example.demo.model.Livestream;
import com.example.demo.model.EVideoPrivacy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface LivestreamRepository extends MongoRepository<Livestream, String> {
    Optional<Livestream> findById(String id);
    List<Livestream> findByUsernameAndDvrPathIsNotNullOrderByUploadedDateDesc(String username, Pageable pageable);
    List<Livestream> findByPrivacyAndDvrPathIsNotNullOrderByUploadedDateDesc(EVideoPrivacy privacy, Pageable pageable);
}
