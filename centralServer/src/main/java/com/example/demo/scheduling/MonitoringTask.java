package com.example.demo.scheduling;

import com.example.demo.model.EServerStatus;
import com.example.demo.model.Server;
import com.example.demo.model.ServerSpecification;
import com.example.demo.service.MonitorService;
import com.example.demo.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MonitoringTask {

    @Autowired
    private MonitorService monitorService;

    @Autowired
    private ServerService serverService;

    // Chạy sau 5s và lặp lại mỗi 60s
    @Scheduled(fixedDelay = 10 * 1000, initialDelay = 5 * 1000)
    public void monitorTask() {
        List<Server> servers = serverService.getAllServers();

        for (Server server : servers) {
            System.out.println("Monitoring server " + server.getIp());
            try {
                if (monitorService.isServerUp(server)) {
                    ServerSpecification serverSpec = monitorService.collectServerMetrics(server);

                    server.setSpecification(serverSpec);
                    server.setStatus(EServerStatus.UP);

                    serverService.updateServerMetrics(server);


                } else {
                    server.setStatus(EServerStatus.DOWN);
                    server.setCurrent_load(0);
                    serverService.updateServerMetrics(server);
                }

            } catch (Exception e) {
                server.setStatus(EServerStatus.DOWN);
                server.setCurrent_load(0);
                serverService.updateServerMetrics(server);
            }
        }
    }

}
