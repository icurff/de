package com.example.demo.service;

import com.example.demo.model.Server;
import com.example.demo.model.ServerSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class MonitorService {
    @Autowired
    private RestTemplate restTemplate;
    @Value("${icurff.app.prometheusEndpoint}")
    private String prometheusEndpoint;

    public Double queryValue(String promql) {

        try {
            String encodedQuery = URLEncoder.encode(promql, StandardCharsets.UTF_8);
            String url = prometheusEndpoint + "?query=" + encodedQuery;

            URI uri = new URI(url);
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            if (response == null || !response.containsKey("data")) {
                System.err.println("Prometheus: Empty or invalid response");
                return null;
            }

            Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
            List<?> resultList = (List<?>) dataMap.get("result");

            if (resultList == null || resultList.isEmpty()) {
                System.err.println("Prometheus: No result for query: " + promql);
                return null;
            }

            Map<String, Object> firstResult = (Map<String, Object>) resultList.get(0);
            List<?> valueList = (List<?>) firstResult.get("value");

            String valueStr = (String) valueList.get(1);
            return Math.round(Double.parseDouble(valueStr) * 100.0) / 100.0;

        } catch (Exception e) {
            System.err.println("Error querying Prometheus: " + e.getMessage());
            System.err.println("Failed query: " + promql);
            e.printStackTrace();
            return null;
        }
    }

    public ServerSpecification collectServerMetrics(Server server) {
        try {
            String serverIp = server.getIp();

            String ramTotalQuery = "node_memory_MemTotal_bytes{instance=\"" + serverIp + "\"} / 1073741824";
            String ramUsageQuery = "(1 - (node_memory_MemAvailable_bytes{instance=\"" + serverIp + "\"} / node_memory_MemTotal_bytes{instance=\"" + serverIp + "\"})) * 100";
            String cpuCoreQuery = "count(node_cpu_seconds_total{instance=\"" + serverIp + "\",mode=\"idle\"})";
            String cpuUsageQuery = "100 - (avg by (instance)(irate(node_cpu_seconds_total{instance=\"" + serverIp + "\",mode=\"idle\"}[5m])) * 100)";
            String diskTotalQuery = "sum(node_filesystem_size_bytes{instance=\"" + serverIp + "\",fstype!~\"tmpfs|overlay\"}) / 1073741824";
            String diskUsageQuery = "((sum(node_filesystem_size_bytes{instance=\"" + serverIp + "\",fstype!~\"tmpfs|overlay\"}) - sum(node_filesystem_avail_bytes{instance=\"" + serverIp + "\",fstype!~\"tmpfs|overlay\"})) / sum(node_filesystem_size_bytes{instance=\"" + serverIp + "\",fstype!~\"tmpfs|overlay\"})) * 100";


            Double ramTotal = queryValue(ramTotalQuery);
            Double ramUsage = queryValue(ramUsageQuery);
            Double cpuCores = queryValue(cpuCoreQuery);
            Double cpuUsage = queryValue(cpuUsageQuery);
            Double diskTotal = queryValue(diskTotalQuery);
            Double diskUsage = queryValue(diskUsageQuery);

            // Create and populate ServerSpecification
            ServerSpecification spec = new ServerSpecification();
            spec.setRam(ramTotal != null ? ramTotal : 0.0);
            spec.setRam_usage(ramUsage != null ? ramUsage : 0.0);
            spec.setCpu(cpuCores != null ? cpuCores : 0.0);
            spec.setCpu_usage(cpuUsage != null ? cpuUsage : 0.0);
            spec.setDisk(diskTotal != null ? diskTotal : 0.0);
            spec.setDisk_usage(diskUsage != null ? diskUsage : 0.0);

            return spec;

        } catch (Exception e) {
            System.err.println("Error collecting metrics for server " + server.getIp() + ": " + e.getMessage());
            return null;
        }
    }

    public boolean isServerUp(Server server) {
        try {
            String query = "up{instance=\"" + server.getIp() + "\"}";
            Double result = queryValue(query);
            return result != null && result > 0;
        } catch (Exception e) {
            System.err.println("Server " + server.getIp() + " is down: " + e.getMessage());
            return false;
        }
    }


}
