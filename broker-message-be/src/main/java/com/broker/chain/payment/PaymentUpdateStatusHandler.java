package com.broker.chain.payment;

import com.broker.chain.RetryContext;
import com.broker.chain.RetryHandler;
import com.broker.repository.RetryJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentUpdateStatusHandler implements RetryHandler {

    private RetryHandler next;

    private final RetryJobRepository retryJobRepository;

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        log.debug("PASO C - PaymentUpdateStatusHandler: updating retryJob status to SUCCESS");
        try {
            context.getRetryJob().setStatus("SUCCESS");
            context.getRetryJob().setStepCStatus("SUCCESS");
            retryJobRepository.save(context.getRetryJob());
            log.info("PASO C - RetryJob {} updated to SUCCESS", context.getRetryJob().getId());
            if (next != null) {
                next.handle(context);
            }
        } catch (Exception e) {
            log.error("PASO C - Failed to update retryJob status: {}", e.getMessage());
            context.getRetryJob().setStepCStatus("FAILED");
            context.fail("PASO C failed: " + e.getMessage());
        }
    }
}
