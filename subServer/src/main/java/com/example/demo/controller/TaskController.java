package com.example.demo.controller;

import com.example.demo.payload.request.task.DeleteVideoTaskRequest;
import com.example.demo.service.TaskPublisherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskPublisherService taskPublisherService;

    @PostMapping("/delete")
    public ResponseEntity<String> enqueueDeleteTask(@Valid @RequestBody DeleteVideoTaskRequest request) {
        taskPublisherService.publishDeleteTask(request.getVideoId(), request.getUsername());
        return ResponseEntity.accepted().body("Delete task published");
    }
}


