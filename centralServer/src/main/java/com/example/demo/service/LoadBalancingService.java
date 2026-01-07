package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.EServerStatus;
import com.example.demo.model.Server;
import com.example.demo.model.ServerSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoadBalancingService {

    @Autowired
    private ServerService serverService;

    // Weight distribution for resource-based load balancing
    private static final double CPU_WEIGHT = 0.40;   // 40%
    private static final double RAM_WEIGHT = 0.40;   // 40%
    private static final double DISK_WEIGHT = 0.20;  // 20%


    public Server getBestAvailableServer() {
        List<Server> availableServers = getAvailableServers();

        if (availableServers.isEmpty()) {
            throw new ResourceNotFoundException("No available servers to handle the request");
        }

        Server bestServer = null;
        double lowestScore = Double.MAX_VALUE;

        for (Server server : availableServers) {
            double score = calculateServerScore(server);
            
            System.out.println(String.format(
                "Server %s (IP: %s) - Score: %.2f (CPU: %.2f%%, RAM: %.2f%%, Disk: %.2f%%)",
                server.getName(),
                server.getIp(),
                score,
                server.getSpecification().getCpu_usage(),
                server.getSpecification().getRam_usage(),
                server.getSpecification().getDisk_usage()
            ));

            if (score < lowestScore) {
                lowestScore = score;
                bestServer = server;
            }
        }

        System.out.println(String.format(
            "Selected server: %s (IP: %s) with score: %.2f",
            bestServer.getName(),
            bestServer.getIp(),
            lowestScore
        ));

        return bestServer;
    }


    public double calculateServerScore(Server server) {
        ServerSpecification spec = server.getSpecification();

        // Get usage percentages (already 0-100 from Prometheus)
        double cpuUsage = spec.getCpu_usage() != null ? spec.getCpu_usage() : 0.0;
        double ramUsage = spec.getRam_usage() != null ? spec.getRam_usage() : 0.0;
        double diskUsage = spec.getDisk_usage() != null ? spec.getDisk_usage() : 0.0;

        // Calculate weighted score
        double score = (cpuUsage * CPU_WEIGHT) +
                       (ramUsage * RAM_WEIGHT) +
                       (diskUsage * DISK_WEIGHT);

        return score;
    }


    public List<Server> getAvailableServers() {
        return serverService.getAllServers().stream()
                .filter(server -> server.getStatus() == EServerStatus.UP)
                .collect(Collectors.toList());
    }
}
