package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "servers")
public class Server {
    @Id
    private String id;
    private String name;
    private String ip;
    private ServerSpecification specification = new ServerSpecification();
    private Integer current_load = 0;
    private EServerStatus status = EServerStatus.DOWN;
    private List<String> videos = new ArrayList<>();
    @CreatedDate
    private Instant createdDate;
    @LastModifiedDate
    private Instant lastModifiedDate;

    public Server(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }
}


