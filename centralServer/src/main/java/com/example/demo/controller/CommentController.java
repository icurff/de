package com.example.demo.controller;

import com.example.demo.payload.request.comment.CreateCommentRequest;
import com.example.demo.payload.response.comment.CommentPageResponse;
import com.example.demo.payload.response.comment.CommentResponse;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos/{videoId}/comments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<CommentPageResponse> getComments(@PathVariable String videoId,
                                                           @AuthenticationPrincipal UserDetailsImpl user,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(commentService.getCommentsForVideo(videoId, user, page, size));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@PathVariable String videoId,
                                                         @Valid @RequestBody CreateCommentRequest request,
                                                         @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(commentService.createComment(videoId, request, user));
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommentResponse> toggleLike(@PathVariable String videoId,
                                                      @PathVariable String commentId,
                                                      @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(commentService.toggleLike(videoId, commentId, user));
    }
}

