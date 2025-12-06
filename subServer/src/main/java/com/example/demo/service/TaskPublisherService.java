package com.example.demo.service;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskPublisherService {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Value("${rabbitmq.exchange}")
    private String exchangeName;
    @Value("${rabbitmq.routingkey}")
    private String routingKey;

    public void publishTranscodeTask(String videoId, String videoPath, String outputDir, String resolution) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "TRANSCODE");
        payload.put("videoId", videoId);
        payload.put("videoPath", videoPath);
        payload.put("outputDir", outputDir);
        payload.put("resolution", resolution);

        sendTask(payload);
    }

    public void publishDeleteTask(String videoId, String username) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "DELETE");
        payload.put("videoId", videoId);
        payload.put("username", username);

        sendTask(payload);
    }

    private void sendTask(Map<String, Object> payload) {
        System.out.println("Preparing to send task to Exchange: '" + exchangeName + "', RoutingKey: '" + routingKey + "'");
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());

        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, payload, correlationData);
            System.out.println("Task sent to queue: " + payload + " with correlation id: " + correlationData.getId());
        } catch (Exception e) {
            System.err.println("FAILED to send task to RabbitMQ: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
