package com.example.demo.repository;

import com.example.demo.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends MongoRepository<Comment, String> {
    Page<Comment> findByVideoIdAndParentCommentIdIsNull(String videoId, Pageable pageable);

    List<Comment> findByParentCommentId(String parentCommentId, Sort sort);

    Optional<Comment> findByIdAndVideoId(String id, String videoId);
}



