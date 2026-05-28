package com.kyoto.data.controller;

import com.kyoto.data.model.D1Response;
import com.kyoto.data.service.D1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/d1")
public class D1Controller {

    @Autowired
    private D1Service d1Service;

    @GetMapping("/query")
    public ResponseEntity<?> queryTable(@RequestParam String table) {
        try {
            String sql = "SELECT * FROM " + table;
            D1Response response = d1Service.executeQuery(sql);
            
            if (response.isSuccess()) {
                List<Map<String, Object>> results = d1Service.getResults(response);
                return ResponseEntity.ok(results);
            } else {
                return ResponseEntity.badRequest().body(response.getErrors());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestBody Map<String, String> request) {
        try {
            String sql = request.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("SQL query is required");
            }

            D1Response response = d1Service.executeQuery(sql);
            
            if (response.isSuccess()) {
                List<Map<String, Object>> results = d1Service.getResults(response);
                return ResponseEntity.ok(results);
            } else {
                return ResponseEntity.badRequest().body(response.getErrors());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insertRecord(@RequestBody Map<String, Object> request) {
        try {
            String table = (String) request.get("table");
            request.remove("table");

            StringBuilder columns = new StringBuilder();
            StringBuilder values = new StringBuilder();
            List<Object> params = new java.util.ArrayList<>();

            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (columns.length() > 0) {
                    columns.append(", ");
                    values.append(", ");
                }
                columns.append(entry.getKey());
                values.append("?");
                params.add(entry.getValue());
            }

            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table, columns, values);
            D1Response response = d1Service.executeUpdateWithParams(sql, params);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "message", "Record inserted successfully",
                    "meta", response.getResult().get(0).getMeta()
                ));
            } else {
                return ResponseEntity.badRequest().body(response.getErrors());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateRecord(@RequestBody Map<String, Object> request) {
        try {
            String table = (String) request.get("table");
            Object id = request.get("id");
            request.remove("table");
            request.remove("id");

            StringBuilder setClause = new StringBuilder();
            List<Object> params = new java.util.ArrayList<>();

            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (setClause.length() > 0) {
                    setClause.append(", ");
                }
                setClause.append(entry.getKey()).append(" = ?");
                params.add(entry.getValue());
            }
            params.add(id);

            String sql = String.format("UPDATE %s SET %s WHERE id = ?", table, setClause);
            D1Response response = d1Service.executeUpdateWithParams(sql, params);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "message", "Record updated successfully",
                    "meta", response.getResult().get(0).getMeta()
                ));
            } else {
                return ResponseEntity.badRequest().body(response.getErrors());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteRecord(@RequestParam String table, @RequestParam Long id) {
        try {
            String sql = "DELETE FROM " + table + " WHERE id = ?";
            D1Response response = d1Service.executeUpdateWithParams(sql, List.of(id));
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "message", "Record deleted successfully",
                    "meta", response.getResult().get(0).getMeta()
                ));
            } else {
                return ResponseEntity.badRequest().body(response.getErrors());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        try {
            String sql = "SELECT 1 as test";
            D1Response response = d1Service.executeQuery(sql);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "status", "Connected to Cloudflare D1 successfully!",
                    "result", d1Service.getResults(response)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "Failed to connect",
                    "errors", response.getErrors()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "Error connecting to D1",
                "error", e.getMessage()
            ));
        }
    }
}
