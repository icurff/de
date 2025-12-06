package com.example.demo.service;

import com.example.demo.model.UploadSession;
import com.example.demo.payload.request.video.NewUploadSessionRequest;
import com.example.demo.repository.UploadSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UploadSessionService {
    @Autowired
    private UploadSessionRepository uploadSessionRepository;



    public String createUploadSession(NewUploadSessionRequest request, String userId, String subServerUrl) {
        UploadSession uploadSession = new UploadSession();
        uploadSession.setUser_id(userId);
        uploadSession.setTotalChunks(request.getTotalChunks());
        uploadSession.setFileName(request.getFileName());
        uploadSession.setFileSize(request.getFileSize());
        uploadSession.setSubServer(subServerUrl);
        uploadSession.setFileType(request.getFileType());
        uploadSessionRepository.save(uploadSession);
        return uploadSession.getId();
    }

    public UploadSession getUploadSession(String sessionId) {
        return uploadSessionRepository.findById(sessionId).orElse(null);
    }

}
