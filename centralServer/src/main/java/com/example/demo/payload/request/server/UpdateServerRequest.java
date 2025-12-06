package com.example.demo.payload.request.server;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateServerRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String ip;
}
