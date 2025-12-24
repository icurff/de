package com.example.demo.service;

import com.example.demo.model.LikedVideo;
import com.example.demo.model.Video;
import com.example.demo.repository.LikedVideoRepository;
import com.example.demo.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LikedVideoService {
    @Autowired
    private LikedVideoRepository likedVideoRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private com.example.demo.repository.LivestreamRepository livestreamRepository;

    public void toggleLike(String username, String contentId) {
        // Check if content exists (either video or livestream)
        boolean exists = videoRepository.existsById(contentId) || livestreamRepository.existsById(contentId);
        if (!exists) {
            return;
        }

        likedVideoRepository.findByUsernameAndVideoId(username, contentId)
                .ifPresentOrElse(
                        existing -> {
                            // Unlike - remove from liked content
                            likedVideoRepository.delete(existing);
                        },
                        () -> {
                            // Like - add to liked content
                            LikedVideo likedVideo = new LikedVideo(username, contentId);
                            likedVideoRepository.save(likedVideo);
                        }
                );
    }

    public boolean isLiked(String username, String videoId) {
        return likedVideoRepository.existsByUsernameAndVideoId(username, videoId);
    }

    public List<Map<String, Object>> getLikedVideosForUser(String username, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "likedAt"));
        List<LikedVideo> likedList = likedVideoRepository.findByUsernameOrderByLikedAtDesc(username, pageable);

        List<String> contentIds = likedList.stream()
                .map(LikedVideo::getVideoId)
                .collect(Collectors.toList());

        if (contentIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch videos
        List<Video> videos = videoRepository.findAllById(contentIds);
        Map<String, Video> videoMap = videos.stream()
                .collect(Collectors.toMap(Video::getId, v -> v));

        // Fetch livestreams
        List<com.example.demo.model.Livestream> livestreams = livestreamRepository.findAllById(contentIds);
        Map<String, com.example.demo.model.Livestream> livestreamMap = livestreams.stream()
                .collect(Collectors.toMap(com.example.demo.model.Livestream::getId, l -> l));

        // Combine liked content with data
        return likedList.stream()
                .map(l -> {
                    Map<String, Object> result = new java.util.HashMap<>();
                    String contentId = l.getVideoId();
                    
                    if (videoMap.containsKey(contentId)) {
                        Video video = videoMap.get(contentId);
                        result.put("id", video.getId());
                        result.put("title", video.getTitle());
                        result.put("description", video.getDescription());
                        result.put("thumbnail", video.getThumbnail());
                        result.put("uploadedDate", video.getUploadedDate());
                        result.put("privacy", video.getPrivacy());
                        result.put("username", video.getUsername());
                        result.put("likedAt", l.getLikedAt());
                        result.put("duration", video.getDuration());
                        result.put("type", "video");
                        result.put("server_locations", video.getServer_locations());
                    } else if (livestreamMap.containsKey(contentId)) {
                        com.example.demo.model.Livestream livestream = livestreamMap.get(contentId);
                        result.put("id", livestream.getId());
                        result.put("title", livestream.getTitle());
                        result.put("description", livestream.getDescription());
                        result.put("thumbnail", livestream.getThumbnail());
                        result.put("uploadedDate", livestream.getUploadedDate());
                        result.put("privacy", livestream.getPrivacy());
                        result.put("username", livestream.getUsername());
                        result.put("likedAt", l.getLikedAt());
                        result.put("duration", livestream.getDuration());
                        result.put("type", "livestream");
                        result.put("serverLocation", livestream.getServerLocation());
                        result.put("dvrPath", livestream.getDvrPath());
                    } else {
                        return null; // Content not found
                    }
                    return result;
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    public long getLikeCount(String videoId) {
        return likedVideoRepository.countByVideoId(videoId);
    }

    public Map<String, Object> getLikeInfo(String username, String videoId) {
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("likeCount", getLikeCount(videoId));
        info.put("isLiked", username != null && isLiked(username, videoId));
        return info;
    }
}

