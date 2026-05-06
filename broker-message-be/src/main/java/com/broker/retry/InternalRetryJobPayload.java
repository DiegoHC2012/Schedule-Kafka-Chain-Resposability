package com.broker.retry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalRetryJobPayload(
        String operation,
        String sourceType,
        String sourceName,
        String body
) {
}