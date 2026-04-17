package com.broker.mongo;

import com.broker.model.PaymentRetryJob;
import com.broker.model.RetryJob;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Data
@Document(collection = "payment_retry_jobs")
public class PaymentRetryJobDocument {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    @Id
    private String id;

    private String retryJobId;
    private String topic;
    private String status;
    private Map<String, Object> payload;
    private String errorMessage;
    private Integer attemptCount;
    private LocalDateTime createdAt;

    public static PaymentRetryJobDocument from(PaymentRetryJob job) {
        PaymentRetryJobDocument doc = new PaymentRetryJobDocument();
        doc.setId(job.getId() != null ? job.getId().toString() : null);
        doc.setRetryJobId(job.getRetryJob() != null && job.getRetryJob().getId() != null
                ? job.getRetryJob().getId().toString() : null);
        doc.setTopic(job.getRetryJob() != null ? job.getRetryJob().getTopic() : null);
        doc.setStatus("FAILED");
        doc.setPayload(parseJson(job.getPayload()));
        doc.setErrorMessage(job.getErrorMessage());
        doc.setAttemptCount(job.getRetryJob() != null ? job.getRetryJob().getAttemptCount() : null);
        doc.setCreatedAt(job.getCreatedAt());
        return doc;
    }

    public static PaymentRetryJobDocument fromSuccess(RetryJob job) {
        PaymentRetryJobDocument doc = new PaymentRetryJobDocument();
        doc.setId(job.getId() != null ? job.getId().toString() : null);
        doc.setRetryJobId(job.getId() != null ? job.getId().toString() : null);
        doc.setTopic(job.getTopic());
        doc.setStatus("SUCCESS");
        doc.setPayload(parseJson(job.getPayload()));
        doc.setAttemptCount(job.getAttemptCount());
        doc.setCreatedAt(job.getCreatedAt());
        return doc;
    }

    private static Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Could not parse payload as JSON: {}", e.getMessage());
            return Map.of("raw", json);
        }
    }
}
