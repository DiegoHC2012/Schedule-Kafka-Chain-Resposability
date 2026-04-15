package com.broker.chain.product;

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
public class ProductCreationHandler implements RetryHandler {

    private RetryHandler next;

    private final RestTemplate restTemplate;

    @Value("${app.endpoints.product}")
    private String productEndpoint;

    public ProductCreationHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        log.debug("PASO A - ProductCreationHandler: sending product to {}", productEndpoint);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> request = new HttpEntity<>(context.getPayload().getData(), headers);
            restTemplate.postForObject(productEndpoint, request, String.class);
            context.getRetryJob().setStepAStatus("SUCCESS");
            log.info("PASO A - Product created successfully for retryJobId={}", context.getRetryJob().getId());
            if (next != null) {
                next.handle(context);
            }
        } catch (Exception e) {
            log.error("PASO A - Failed to create product: {}", e.getMessage());
            context.getRetryJob().setStepAStatus("FAILED");
            context.fail("PASO A failed: " + e.getMessage());
        }
    }
}
