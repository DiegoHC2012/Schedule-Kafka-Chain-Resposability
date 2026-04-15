package com.broker.chain.payment;

import com.broker.chain.RetryContext;
import com.broker.chain.RetryHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PaymentCreationHandler implements RetryHandler {

    private RetryHandler next;

    private final RestTemplate restTemplate;

    @Value("${app.endpoints.payment}")
    private String paymentEndpoint;

    public PaymentCreationHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        log.debug("PASO A - PaymentCreationHandler: sending payment to {}", paymentEndpoint);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> request = new HttpEntity<>(context.getPayload().getData(), headers);
            restTemplate.postForObject(paymentEndpoint, request, String.class);
            context.getRetryJob().setStepAStatus("SUCCESS");
            log.info("PASO A - Payment created successfully for retryJobId={}", context.getRetryJob().getId());
            if (next != null) {
                next.handle(context);
            }
        } catch (Exception e) {
            log.error("PASO A - Failed to create payment: {}", e.getMessage());
            context.getRetryJob().setStepAStatus("FAILED");
            context.fail("PASO A failed: " + e.getMessage());
        }
    }
}
