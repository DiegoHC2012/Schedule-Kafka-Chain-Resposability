package com.broker.chain.order;

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
public class OrderCreationHandler implements RetryHandler {

    private RetryHandler next;

    private final RestTemplate restTemplate;

    @Value("${app.endpoints.order}")
    private String orderEndpoint;

    public OrderCreationHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        log.debug("PASO A - OrderCreationHandler: sending order to {}", orderEndpoint);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> request = new HttpEntity<>(context.getPayload().getData(), headers);
            restTemplate.postForObject(orderEndpoint, request, String.class);
            context.getRetryJob().setStepAStatus("SUCCESS");
            log.info("PASO A - Order created successfully for retryJobId={}", context.getRetryJob().getId());
            if (next != null) {
                next.handle(context);
            }
        } catch (Exception e) {
            log.error("PASO A - Failed to create order: {}", e.getMessage());
            context.getRetryJob().setStepAStatus("FAILED");
            context.fail("PASO A failed: " + e.getMessage());
        }
    }
}
