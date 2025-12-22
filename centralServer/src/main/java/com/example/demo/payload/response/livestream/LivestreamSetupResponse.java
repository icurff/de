package com.example.demo.payload.response.livestream;

import com.example.demo.model.LivestreamKey;
import lombok.Data;

@Data
public class LivestreamSetupResponse {
    private String id;
    private String userId;
    private String username;
    private String title;
    private String description;
    private String streamKey;
    private boolean isLive;
    private String currentLivestreamId;
    private String rtmpUrl;
    
    public LivestreamSetupResponse(LivestreamKey livestreamKey, String serverIp) {
        this.id = livestreamKey.getId();
        this.userId = livestreamKey.getUserId();
        this.username = livestreamKey.getUsername();
        this.title = livestreamKey.getTitle();
        this.description = livestreamKey.getDescription();
        this.streamKey = livestreamKey.getStreamKey();
        this.isLive = livestreamKey.isLive();
        this.currentLivestreamId = livestreamKey.getCurrentLivestreamId();
        this.rtmpUrl = "rtmp://" + serverIp + ":11935/live";
    }

}

