package com.kyoto.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class D1Response {
    
    private boolean success;
    private List<D1Result> result;
    private List<D1Error> errors;
    private List<String> messages;

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<D1Result> getResult() {
        return result;
    }

    public void setResult(List<D1Result> result) {
        this.result = result;
    }

    public List<D1Error> getErrors() {
        return errors;
    }

    public void setErrors(List<D1Error> errors) {
        this.errors = errors;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    // Inner classes
    public static class D1Result {
        private List<Map<String, Object>> results;
        private boolean success;
        private D1Meta meta;

        @JsonProperty("served_by")
        private String servedBy;

        public List<Map<String, Object>> getResults() {
            return results;
        }

        public void setResults(List<Map<String, Object>> results) {
            this.results = results;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public D1Meta getMeta() {
            return meta;
        }

        public void setMeta(D1Meta meta) {
            this.meta = meta;
        }

        public String getServedBy() {
            return servedBy;
        }

        public void setServedBy(String servedBy) {
            this.servedBy = servedBy;
        }
    }

    public static class D1Meta {
        private Double duration;
        
        @JsonProperty("changes")
        private Integer changes;
        
        @JsonProperty("last_row_id")
        private Integer lastRowId;
        
        @JsonProperty("rows_read")
        private Integer rowsRead;
        
        @JsonProperty("rows_written")
        private Integer rowsWritten;

        public Double getDuration() {
            return duration;
        }

        public void setDuration(Double duration) {
            this.duration = duration;
        }

        public Integer getChanges() {
            return changes;
        }

        public void setChanges(Integer changes) {
            this.changes = changes;
        }

        public Integer getLastRowId() {
            return lastRowId;
        }

        public void setLastRowId(Integer lastRowId) {
            this.lastRowId = lastRowId;
        }

        public Integer getRowsRead() {
            return rowsRead;
        }

        public void setRowsRead(Integer rowsRead) {
            this.rowsRead = rowsRead;
        }

        public Integer getRowsWritten() {
            return rowsWritten;
        }

        public void setRowsWritten(Integer rowsWritten) {
            this.rowsWritten = rowsWritten;
        }
    }

    public static class D1Error {
        private String code;
        private String message;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
