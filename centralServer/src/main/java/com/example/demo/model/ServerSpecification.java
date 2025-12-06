package com.example.demo.model;

import lombok.Data;

@Data
public class ServerSpecification {
    private Double ram = 0.0;
    private Double ram_usage = 0.0;
    private Double cpu = 0.0;
    private Double cpu_usage = 0.0;
    private Double disk = 0.0;
    private Double disk_usage = 0.0;

}
