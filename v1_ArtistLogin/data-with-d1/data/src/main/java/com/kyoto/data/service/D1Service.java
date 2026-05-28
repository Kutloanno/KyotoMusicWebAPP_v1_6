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

    public D1Response executeQuery(String sql) {
        return executeSql(sql);
    }

    public D1Response executeQueryWithParams(String sql, List<Object> params) {
        return executeSqlWithParams(sql, params);
    }

    public D1Response executeUpdate(String sql) {
        return executeSql(sql);
    }

    public D1Response executeUpdateWithParams(String sql, List<Object> params) {
        return executeSqlWithParams(sql, params);
    }

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

    public List<Map<String, Object>> getResults(D1Response response) {
        if (response != null && response.isSuccess() && response.getResult() != null && !response.getResult().isEmpty()) {
            return response.getResult().get(0).getResults();
        }
        return List.of();
    }

    public D1Response executeBatch(List<String> sqlStatements) {
        String combinedSql = String.join("; ", sqlStatements);
        return executeSql(combinedSql);
    }
}
