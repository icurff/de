package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Server;
import com.example.demo.model.UploadSession;
import com.example.demo.payload.request.video.NewUploadSessionRequest;
import com.example.demo.payload.response.video.UploadSessionResponse;
import com.example.demo.repository.UploadSessionRepository;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.service.LoadBalancingService;
import com.example.demo.service.UploadSessionService;
import com.example.demo.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/uploads")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UploadSessionController {
    @Autowired
    private UploadSessionService uploadSessionService;

    @Autowired
    private LoadBalancingService loadBalancingService;

    @Autowired
    private com.example.demo.service.ActivityLogService activityLogService;


    @PostMapping("/sessions")
    public ResponseEntity<?> createUploadSession(@Valid @RequestBody NewUploadSessionRequest request,
                                                 @AuthenticationPrincipal UserDetailsImpl user) {
        try {
            // Use load balancing to select the best available server
            Server selectedServer = loadBalancingService.getBestAvailableServer();
            String subServerUrl = "http://" + selectedServer.getIp();
            
            String sessionId = uploadSessionService.createUploadSession(request, user.getId(), subServerUrl);
            UploadSessionResponse response = new UploadSessionResponse(sessionId, subServerUrl);
            
            // Log the activity
            activityLogService.logActivity(
                "UPLOAD_REQUEST",
                selectedServer.getName(),
                selectedServer.getIp(),
                user.getUsername(),
                "Upload request for file: " + request.getFileName()
            );
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            // No servers available
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No available servers to handle the upload request. Please try again later.");
        }
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getUploadSession(@PathVariable String sessionId,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UploadSession session = uploadSessionService.getUploadSession(sessionId);
        if (session == null || !session.getUser_id().equals(userDetails.getId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

//    @PostMapping("/{sessionId}/complete")
//    public ResponseEntity<?> completeUpload(
//            @AuthenticationPrincipal UserDetailsImpl userDetails,
//            @PathVariable String sessionId,
//            @Valid @RequestBody NewVideoRequest request
//    ) {
//        UploadSession uploadSession = uploadSessionService.getUploadSession(sessionId);
//        if (uploadSession == null || !uploadSession.getUser_id().equals(userDetails.getId())) {
//            return ResponseEntity.notFound().build();
//        }
//
//        uploadSession.setStatus(EUploadStatus.COMPLETED);
//        uploadSessionRepository.save(uploadSession);
//
//        String result = videoService.addNewVideo(request, userDetails.getId());
//
//        String url = uploadSession.getSubServer() + "/api/videos/" + sessionId + "/merge";
//
//        Map<String, String> body = Map.of("username", userDetails.getUsername());
//        restTemplate.postForObject(url, body, String.class);
//
//        return ResponseEntity.ok(result);
//    }

}
