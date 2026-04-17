package com.broker.chain.order;

import com.broker.chain.RetryContext;
import com.broker.chain.RetryHandler;
import com.broker.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEmailHandler implements RetryHandler {

    private RetryHandler next;

    private final EmailService emailService;

    public OrderEmailHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        log.debug("PASO B - OrderEmailHandler: sending success email for retryJobId={}", context.getRetryJob().getId());
        try {
            emailService.sendSuccessEmail(
                context.getRetryJob(),
                context.getPayload().getData()
            );
            context.getRetryJob().setStepBStatus("SUCCESS");
            log.info("PASO B - Success email sent for retryJobId={}", context.getRetryJob().getId());
            if (next != null) {
                next.handle(context);
            }
        } catch (Exception e) {
            log.error("PASO B - Failed to send order success email: {}", e.getMessage());
            context.getRetryJob().setStepBStatus("FAILED");
            context.fail("PASO B failed: " + e.getMessage());
        }
    }
}
