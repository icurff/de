package com.example.demo.payload.request.server;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterServerRequest {
    @NotBlank
    private String name = "New SubServer";
    @NotBlank
    private String ip;

}
