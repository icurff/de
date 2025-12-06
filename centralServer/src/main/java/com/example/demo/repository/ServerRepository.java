package com.example.demo.repository;

import com.example.demo.model.Server;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ServerRepository extends MongoRepository<Server, String> {
    Boolean existsByIp(String ip);
}
