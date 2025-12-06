package com.example.demo.controller;

import com.example.demo.model.Server;
import com.example.demo.payload.request.server.RegisterServerRequest;
import com.example.demo.payload.request.server.UpdateServerRequest;
import com.example.demo.service.ServerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/servers")
public class ServerController {
    @Autowired
    private ServerService serverService;

    @GetMapping("/")
    public ResponseEntity<?> getRegisteredServer() {
        return ResponseEntity.ok(serverService.getAllServers());
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<?> getServerDetails(@PathVariable String serverId) {
        Server server = serverService.getServerById(serverId);
        return ResponseEntity.ok(server);
    }


    @PostMapping("/")
    public ResponseEntity<?> registerServer(@Valid @RequestBody RegisterServerRequest request) {
        return ResponseEntity.ok(serverService.registerServer(request));
    }

    @PutMapping("/{serverId}")
    public ResponseEntity<?> updateServer(
            @PathVariable String serverId,
            @Valid @RequestBody UpdateServerRequest request) {
        return ResponseEntity.ok(serverService.updateServerInfo(serverId, request));
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<?> deleteServer(@PathVariable String serverId) {
        return ResponseEntity.ok(serverService.deleteServer(serverId));
    }

//    @GetMapping("/suitable-server")
//    public ResponseEntity<?> getSuitableServer() {
//        return ResponseEntity.ok(serverService.getBestServer());
//    }
}

