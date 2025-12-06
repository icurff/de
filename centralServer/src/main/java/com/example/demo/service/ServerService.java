package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ResourceTakenException;
import com.example.demo.payload.request.server.RegisterServerRequest;
import com.example.demo.payload.request.server.UpdateServerRequest;
import com.example.demo.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.model.Server;

import java.util.List;

@Service
public class ServerService {

    @Autowired
    private ServerRepository serverRepository;

    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }

    public Server getServerById(String serverId) {
        return serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server not found"));
    }


    public String registerServer(RegisterServerRequest request) {
        if (serverRepository.existsByIp(request.getIp())) {
            throw new ResourceTakenException("Server Ip already exists");
        }
        Server server = new Server(request.getName(), request.getIp());
        serverRepository.save(server);
        return "Server registered successfully!";
    }

    public String updateServerInfo(String serverId, UpdateServerRequest request) {

        Server server = getServerById(serverId);
        server.setIp(request.getIp());
        server.setName(request.getName());
        serverRepository.save(server);
        return "Server updated successfully!";
    }

    public void updateServerMetrics(Server server) {
        serverRepository.save(server);
    }

    public String deleteServer(String serverId) {
        Server server = getServerById(serverId);
        serverRepository.delete(server);
        return "Server deleted successfully!";
    }

    public List<Server> getAvailableServers() {
        return serverRepository.findAll().stream()
                .filter(server -> server.getStatus() == com.example.demo.model.EServerStatus.UP)
                .collect(java.util.stream.Collectors.toList());
    }

}
