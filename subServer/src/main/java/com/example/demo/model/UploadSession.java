package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "upload_sessions")
public class UploadSession {
    @Id
    private String id;
    private String user_id;
    private String subServer;
    private EUploadStatus status = EUploadStatus.UPLOADING;
    private Integer totalChunks = 0;
    private String fileType;
    private String fileName;
    private Long fileSize;
    private Integer duration = 0;
    @CreatedDate
    private Instant createdDate;
}
