package com.example.demo.controller;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatController extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToStream = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionToAuthenticated = new ConcurrentHashMap<>();
    
    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        String streamUsername = sessionToStream.get(sessionId);
        sessions.remove(sessionId);
        sessionToStream.remove(sessionId);
        sessionToUsername.remove(sessionId);
        sessionToAuthenticated.remove(sessionId);
        if (streamUsername != null) {
            broadcastViewerCount(streamUsername);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ObjectMapper mapper = getObjectMapper();
            String payload = message.getPayload();
            Map<String, Object> data = mapper.readValue(payload, Map.class);

            String type = (String) data.get("type");
            String token = (String) data.get("token");
            String streamUsername = (String) data.get("streamUsername");

            if (streamUsername == null) {
                sendError(session, "streamUsername is required");
                return;
            }
            sessionToStream.put(session.getId(), streamUsername);

            String username = null;
            boolean isAuthenticated = false;
            
            if (token != null && jwtUtil.validateJwtToken(token)) {
                username = jwtUtil.getUserNameFromJwtToken(token);
                sessionToUsername.put(session.getId(), username);
                sessionToAuthenticated.put(session.getId(), true);
                isAuthenticated = true;
            } else {
                username = sessionToUsername.get(session.getId());
                isAuthenticated = sessionToAuthenticated.getOrDefault(session.getId(), false);
            }

            if ("join".equals(type) || "viewerCount".equals(type)) {
                broadcastViewerCount(streamUsername);
                return;
            }

      
            if ("message".equals(type) && !isAuthenticated) {
                sendError(session, "Please login to send messages");
                return;
            }

            if ("message".equals(type) && username == null) {
                sendError(session, "Unauthorized");
                return;
            }


            Optional<User> userOpt = userRepository.findByUsername(username);
            String avatar = userOpt.map(User::getAvatar).orElse(null);

            if ("message".equals(type)) {
                String messageText = (String) data.get("message");
                if (messageText == null || messageText.trim().isEmpty()) {
                    return;
                }

                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setUsername(username);
                chatMessage.setMessage(messageText);
                chatMessage.setAvatar(avatar);
                chatMessage.setStreamUsername(streamUsername);
                chatMessage.setTimestamp(LocalDateTime.now());

                broadcastToStream(chatMessage, streamUsername);

                broadcastViewerCount(streamUsername);
            }
        } catch (Exception e) {
            sendError(session, "Invalid message format: " + e.getMessage());
        }
    }

    private void broadcastToStream(ChatMessage message, String streamUsername) {
        String messageJson;
        try {
            ObjectMapper mapper = getObjectMapper();
            messageJson = mapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }

        sessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                String sessionStream = sessionToStream.get(sessionId);
                if (streamUsername.equals(sessionStream)) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        });
    }

    private void broadcastViewerCount(String streamUsername) {

        int count = 0;
        for (Map.Entry<String, String> entry : sessionToStream.entrySet()) {
            if (streamUsername.equals(entry.getValue())) {
                WebSocketSession session = sessions.get(entry.getKey());
                if (session != null && session.isOpen()) {
                    count++;
                }
            }
        }

 
        ObjectMapper mapper = getObjectMapper();
        try {
            Map<String, Object> viewerCountMsg = Map.of(
                "type", "viewerCount",
                "streamUsername", streamUsername,
                "count", count
            );
            String json = mapper.writeValueAsString(viewerCountMsg);

            sessions.forEach((sessionId, session) -> {
                if (session.isOpen()) {
                    String sessionStream = sessionToStream.get(sessionId);
                    if (streamUsername.equals(sessionStream)) {
                        try {
                            session.sendMessage(new TextMessage(json));
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            });
        } catch (Exception e) {
            // Ignore
        }
    }

    private void sendError(WebSocketSession session, String error) {
        try {
            ObjectMapper mapper = getObjectMapper();
            Map<String, Object> errorMsg = Map.of(
                "error", error,
                "timestamp", LocalDateTime.now().toString()
            );
            String json = mapper.writeValueAsString(errorMsg);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            // Ignore
        }
    }
}

