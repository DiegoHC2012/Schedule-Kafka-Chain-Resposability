package com.broker.chain;

import com.broker.dto.RetryJobPayload;
import com.broker.model.RetryJob;
import lombok.Data;

@Data
public class RetryContext {

    private RetryJob retryJob;
    private RetryJobPayload payload;
    private boolean success = true;
    private String errorMessage;

    public RetryContext(RetryJob retryJob, RetryJobPayload payload) {
        this.retryJob = retryJob;
        this.payload = payload;
    }

    public void fail(String message) {
        this.success = false;
        this.errorMessage = message;
    }
}
