package com.example.demo.repository;

import com.example.demo.model.Video;
import com.example.demo.model.EVideoPrivacy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VideoRepository extends MongoRepository<Video,String> {
    List<Video> findByUsernameOrderByUploadedDateDesc(String userId, Pageable pageable);
    List<Video> findByUsernameAndPrivacyOrderByUploadedDateDesc(String username, EVideoPrivacy privacy, Pageable pageable);
    List<Video> findByPrivacyOrderByUploadedDateDesc(EVideoPrivacy privacy, Pageable pageable);
}

