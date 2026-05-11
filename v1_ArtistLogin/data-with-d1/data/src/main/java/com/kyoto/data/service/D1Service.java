package com.kyoto.data.service;

import com.kyoto.data.config.D1Config;
import com.kyoto.data.model.D1Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class D1Service {

    @Autowired
    private D1Config d1Config;

    @Autowired
    private RestTemplate restTemplate;

    private static final String D1_API_URL = "https://api.cloudflare.com/client/v4/accounts/%s/d1/database/%s/query";

    /**
     * Execute a SELECT query and return results
     */
    public D1Response executeQuery(String sql) {
        return executeSql(sql);
    }

    /**
     * Execute a query with parameters (for security/SQL injection prevention)
     */
    public D1Response executeQueryWithParams(String sql, List<Object> params) {
        return executeSqlWithParams(sql, params);
    }

    /**
     * Execute an INSERT, UPDATE, or DELETE query
     */
    public D1Response executeUpdate(String sql) {
        return executeSql(sql);
    }

    /**
     * Execute an UPDATE with parameters
     */
    public D1Response executeUpdateWithParams(String sql, List<Object> params) {
        return executeSqlWithParams(sql, params);
    }

    /**
     * Execute raw SQL query
     */
    private D1Response executeSql(String sql) {
        String url = String.format(D1_API_URL, d1Config.getAccountId(), d1Config.getDatabaseId());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + d1Config.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("sql", sql);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<D1Response> response = restTemplate.postForEntity(url, request, D1Response.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error executing D1 query: " + e.getMessage(), e);
        }
    }

    /**
     * Execute SQL with parameters (prevents SQL injection)
     */
    private D1Response executeSqlWithParams(String sql, List<Object> params) {
        String url = String.format(D1_API_URL, d1Config.getAccountId(), d1Config.getDatabaseId());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + d1Config.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("sql", sql);
        body.put("params", params);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<D1Response> response = restTemplate.postForEntity(url, request, D1Response.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error executing D1 query with params: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to get results as List of Maps
     */
    public List<Map<String, Object>> getResults(D1Response response) {
        if (response != null && response.isSuccess() && response.getResult() != null && !response.getResult().isEmpty()) {
            return response.getResult().get(0).getResults();
        }
        return List.of();
    }

    /**
     * Execute multiple SQL statements in a batch
     */
    public D1Response executeBatch(List<String> sqlStatements) {
        // D1 supports batch operations - you can send multiple SQL statements
        String combinedSql = String.join("; ", sqlStatements);
        return executeSql(combinedSql);
    }
}
