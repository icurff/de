package com.example.demo.service;

import com.example.demo.util.FFmpegUtil;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
public class TaskConsumerService {

    @Autowired
    private VideoService videoService;

    @Value("${icurff.app.server.location:localhost:8081}")
    private String serverLocation;

    @RabbitListener(queues = "${rabbitmq.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void consumeTask(Map<String, Object> task, Channel channel, Message message) throws IOException {
//        Long tag = message.getMessageProperties().getDeliveryTag();
        try {
            System.out.println("=== RECEIVED TASK ===");
            System.out.println("Task content: " + task);

            String action = task.getOrDefault("action", "TRANSCODE").toString();
            String normalizedAction = action.toUpperCase(Locale.ROOT);

            if (normalizedAction.equals("DELETE") || normalizedAction.equals("DELETE_VIDEO")) {
                handleDeleteTask(task);
                return;
            }

            handleTranscodeTask(task);

//            channel.basicAck(tag, false);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
//            channel.basicNack(tag, false, false);
        }


    }

    private void handleTranscodeTask(Map<String, Object> task) throws IOException {
        String videoId = getAsString(task, "videoId");
        String videoPath = getAsString(task, "videoPath");
        String outputDir = getAsString(task, "outputDir");
        String resolution = getAsString(task, "resolution");

        System.out.println("Extracted videoId: " + videoId);
        System.out.println("Extracted videoPath: " + videoPath);
        System.out.println("Extracted outputDir: " + outputDir);
        System.out.println("Extracted resolution: " + resolution);

        if (videoId == null || videoId.isEmpty()) {
            System.err.println("ERROR: videoId is null or empty! Cannot update database. Task: " + task);
            System.err.println("This means the application is using old compiled code. Please rebuild and restart!");
        }

        try {
            FFmpegUtil.transcodeVideo(videoPath, outputDir, resolution, "qmh");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Transcoding interrupted", e);
        }

        if (videoId != null && !videoId.isEmpty()) {
            System.out.println("Calling updateVideoResolutionAndServer...");
            videoService.updateVideoResolutionAndServer(videoId, Integer.parseInt(resolution), serverLocation);
        } else {
            System.err.println("SKIPPING database update due to missing videoId");
        }
    }

    private void handleDeleteTask(Map<String, Object> task) {
        String videoId = getAsString(task, "videoId");
        String username = getAsString(task, "username");

        if (videoId == null || videoId.isBlank() || username == null || username.isBlank()) {
            System.err.println("Delete task missing videoId or username: " + task);
            return;
        }

        try {
            boolean deleted = videoService.deleteVideo(username, videoId);
            if (deleted) {
                System.out.println("Successfully deleted video " + videoId + " for user " + username);
            } else {
                System.err.println("Failed to delete video " + videoId + " for user " + username + ". Video not found or permission denied.");
            }
        } catch (Exception ex) {
            System.err.println("Error deleting video " + videoId + " for user " + username + ": " + ex.getMessage());
        }
    }

    private String getAsString(Map<String, Object> task, String key) {
        Object value = task.get(key);
        return value != null ? value.toString() : null;
    }
}
