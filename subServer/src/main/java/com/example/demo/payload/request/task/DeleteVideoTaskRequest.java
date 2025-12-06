package com.example.demo.payload.request.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteVideoTaskRequest {

    @NotBlank
    private String videoId;

    @NotBlank
    private String username;
}


