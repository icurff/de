package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Comment;
import com.example.demo.payload.request.comment.CreateCommentRequest;
import com.example.demo.payload.response.comment.CommentPageResponse;
import com.example.demo.payload.response.comment.CommentResponse;
import com.example.demo.repository.CommentRepository;
import com.example.demo.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoService videoService;

    private static final Sort TOP_LEVEL_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Sort REPLY_SORT = Sort.by(Sort.Direction.ASC, "createdAt");

    @Autowired
    public CommentService(CommentRepository commentRepository, VideoService videoService) {
        this.commentRepository = commentRepository;
        this.videoService = videoService;
    }

    public CommentPageResponse getCommentsForVideo(String videoId, UserDetailsImpl currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), TOP_LEVEL_SORT);
        Page<Comment> commentPage = commentRepository.findByVideoIdAndParentCommentIdIsNull(videoId, pageable);

        List<CommentResponse> responses = commentPage.getContent().stream()
                .map(comment -> toResponse(comment, currentUser, fetchReplies(comment.getId(), currentUser)))
                .collect(Collectors.toList());

        return CommentPageResponse.builder()
                .comments(responses)
                .page(commentPage.getNumber())
                .size(commentPage.getSize())
                .hasMore(commentPage.hasNext())
                .build();
    }

    public CommentResponse createComment(String videoId, CreateCommentRequest request, UserDetailsImpl user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để bình luận");
        }

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung bình luận không được để trống");
        }

        // Ensure video exists
        videoService.getVideoById(videoId);

        Comment parentComment = null;
        if (StringUtils.hasText(request.getParentCommentId())) {
            parentComment = commentRepository.findByIdAndVideoId(request.getParentCommentId(), videoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bình luận gốc không tồn tại"));

            if (StringUtils.hasText(parentComment.getParentCommentId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ trả lời một cấp độ");
            }
        }

        Comment comment = Comment.builder()
                .videoId(videoId)
                .parentCommentId(parentComment != null ? parentComment.getId() : null)
                .userId(user.getId())
                .username(user.getUsername())
                .content(content)
                .build();

        Comment saved = commentRepository.save(comment);

        return toResponse(saved, user, parentComment == null ? Collections.emptyList() : null);
    }

    public CommentResponse toggleLike(String videoId, String commentId, UserDetailsImpl user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để tương tác");
        }

        Comment comment = commentRepository.findByIdAndVideoId(commentId, videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        boolean added = comment.getLikedUserIds().add(user.getId());
        if (!added) {
            comment.getLikedUserIds().remove(user.getId());
        }

        Comment saved = commentRepository.save(comment);

        List<CommentResponse> replies = null;
        if (saved.getParentCommentId() == null) {
            replies = fetchReplies(saved.getId(), user);
        }

        return toResponse(saved, user, replies);
    }

    private List<CommentResponse> fetchReplies(String parentId, UserDetailsImpl currentUser) {
        List<Comment> replies = commentRepository.findByParentCommentId(parentId, REPLY_SORT);
        return replies.stream()
                .map(reply -> toResponse(reply, currentUser, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    private CommentResponse toResponse(Comment comment, UserDetailsImpl currentUser, List<CommentResponse> replies) {
        boolean likedByCurrentUser = currentUser != null && comment.getLikedUserIds().contains(currentUser.getId());
        List<CommentResponse> replyList = replies;
        if (replyList == null) {
            replyList = comment.getParentCommentId() == null ? fetchReplies(comment.getId(), currentUser) : Collections.emptyList();
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .videoId(comment.getVideoId())
                .parentCommentId(comment.getParentCommentId())
                .userId(comment.getUserId())
                .username(comment.getUsername())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .likesCount(comment.getLikedUserIds() == null ? 0 : comment.getLikedUserIds().size())
                .likedByCurrentUser(likedByCurrentUser)
                .replies(replyList)
                .build();
    }
}



