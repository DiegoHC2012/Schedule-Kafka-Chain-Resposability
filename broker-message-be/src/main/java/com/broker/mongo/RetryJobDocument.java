package com.broker.mongo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Data
@Document(collection = "retry_jobs")
public class RetryJobDocument {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    @Id
    private String id;

    private String topic;
    private String status;
    private Map<String, Object> payload;

    private String stepAStatus;
    private String stepBStatus;
    private String stepCStatus;

    private String errorMessage;

    private Integer attemptCount;
    private Integer maxAttempts;

    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static RetryJobDocument from(com.broker.model.RetryJob job) {
        RetryJobDocument doc = new RetryJobDocument();
        doc.setId(job.getId() != null ? job.getId().toString() : UUID.randomUUID().toString());
        doc.setTopic(job.getTopic());
        doc.setStatus(job.getStatus());
        doc.setPayload(parseJson(job.getPayload()));
        doc.setStepAStatus(job.getStepAStatus());
        doc.setStepBStatus(job.getStepBStatus());
        doc.setStepCStatus(job.getStepCStatus());
        doc.setErrorMessage(job.getErrorMessage());
        doc.setAttemptCount(job.getAttemptCount());
        doc.setMaxAttempts(job.getMaxAttempts());
        doc.setNextRetryAt(job.getNextRetryAt());
        doc.setCreatedAt(job.getCreatedAt());
        doc.setUpdatedAt(LocalDateTime.now());
        return doc;
    }

    private static Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Could not parse payload as JSON, storing as raw string wrapper: {}", e.getMessage());
            return Map.of("raw", json);
        }
    }
}
