package com.broker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetryJobPayload {

    private Map<String, Object> data;
    private StepStatus sendEmail;
    private StepStatus updateRetryJobs;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepStatus {
        private String status;
        private String message;
    }
}
