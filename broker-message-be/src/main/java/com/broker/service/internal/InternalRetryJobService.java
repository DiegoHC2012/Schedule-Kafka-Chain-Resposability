package com.broker.service.internal;

import com.broker.config.KafkaTopics;
import com.broker.dto.event.EmailNotificationEvent;
import com.broker.dto.event.InventoryUpdateEvent;
import com.broker.dto.event.OrderStatusChangedEvent;
import com.broker.dto.event.PaymentReceivedEvent;
import com.broker.dto.order.OrderCreateRequest;
import com.broker.dto.order.OrderProductsUpdateRequest;
import com.broker.dto.order.OrderStatusUpdateRequest;
import com.broker.dto.payment.PaymentRequest;
import com.broker.dto.product.ProductCreateRequest;
import com.broker.model.RetryJob;
import com.broker.mongo.MongoSyncService;
import com.broker.repository.RetryJobRepository;
import com.broker.retry.InternalRetryJobPayload;
import com.broker.retry.InternalRetryOperation;
import com.broker.service.EmailService;
import com.broker.service.order.OrderCommandService;
import com.broker.service.payment.PaymentCommandService;
import com.broker.service.product.ProductCatalogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalRetryJobService {

    private final RetryJobRepository retryJobRepository;
    private final MongoSyncService mongoSyncService;
    private final ObjectMapper objectMapper;
    private final OrderCommandService orderCommandService;
    private final PaymentCommandService paymentCommandService;
    private final ProductCatalogService productCatalogService;
    private final ModuleEventProcessor moduleEventProcessor;
    private final EmailService emailService;

    @Value("${app.retry.max-attempts:3}")
    private int defaultMaxAttempts;

    @Value("${app.retry.base-delay-seconds:10}")
    private int baseDelaySeconds;

    @Value("${app.retry.max-delay-seconds:300}")
    private int maxDelaySeconds;

    public void recordEndpointFailure(InternalRetryOperation operation, String endpoint, Object request, RuntimeException exception) {
        if (!shouldRecordEndpointFailure(exception)) {
            return;
        }

        recordFailure(operation, "endpoint", endpoint, serializeBody(request), exception.getMessage());
    }

    public void recordEventFailure(InternalRetryOperation operation, String topic, String message, Exception exception) {
        recordFailure(operation, "topic", topic, message, exception.getMessage());
    }

    public void processRetries() {
        List<RetryJob> pending = retryJobRepository.findPendingDueByTopic(
                "PENDING", KafkaTopics.INTERNAL_RETRY_JOBS, LocalDateTime.now(), PageRequest.of(0, 100));

        log.info("InternalRetryJobService: processing {} pending internal retry jobs", pending.size());

        for (RetryJob retryJob : pending) {
            try {
                prepareForAttempt(retryJob);
                InternalRetryJobPayload payload = objectMapper.readValue(retryJob.getPayload(), InternalRetryJobPayload.class);
                execute(payload);
                finalizeSuccess(retryJob);
                retryJobRepository.save(retryJob);
                mongoSyncService.sync(retryJob);
            } catch (Exception e) {
                log.error("Unexpected error processing internal retryJob {}: {}", retryJob.getId(), e.getMessage(), e);
                handleFailure(retryJob, e.getMessage());
            }
        }
    }

    private void execute(InternalRetryJobPayload payload) throws JsonProcessingException {
        InternalRetryOperation operation = InternalRetryOperation.valueOf(
                Objects.requireNonNull(payload.operation(), "operation is required")
        );
        String body = Objects.requireNonNull(payload.body(), "body is required");

        switch (operation) {
            case PRODUCT_CREATE -> productCatalogService.createProduct(objectMapper.readValue(body, ProductCreateRequest.class));
            case ORDER_CREATE -> orderCommandService.createOrder(objectMapper.readValue(body, OrderCreateRequest.class));
            case ORDER_UPDATE_PRODUCTS -> orderCommandService.updateOrderProducts(objectMapper.readValue(body, OrderProductsUpdateRequest.class));
            case ORDER_UPDATE_STATUS -> orderCommandService.updateStatus(objectMapper.readValue(body, OrderStatusUpdateRequest.class));
            case PAYMENT_CREATE -> paymentCommandService.createPayment(objectMapper.readValue(body, PaymentRequest.class));
            case PAYMENT_RECEIVED_EVENT -> moduleEventProcessor.processPaymentReceived(objectMapper.readValue(body, PaymentReceivedEvent.class));
            case INVENTORY_UPDATE_EVENT -> moduleEventProcessor.processInventoryUpdate(objectMapper.readValue(body, InventoryUpdateEvent.class));
            case ORDER_STATUS_CHANGED_EVENT -> moduleEventProcessor.processOrderStatusChanged(objectMapper.readValue(body, OrderStatusChangedEvent.class));
            case EMAIL_NOTIFICATION_EVENT -> emailService.sendNotificationEmail(objectMapper.readValue(body, EmailNotificationEvent.class));
        }
    }

    private void recordFailure(InternalRetryOperation operation, String sourceType, String sourceName, String body, String errorMessage) {
        try {
            RetryJob retryJob = new RetryJob();
            retryJob.setTopic(KafkaTopics.INTERNAL_RETRY_JOBS);
            retryJob.setPayload(objectMapper.writeValueAsString(new InternalRetryJobPayload(
                    operation.name(),
                    sourceType,
                    sourceName,
                    body
            )));
            retryJob.setStatus("PENDING");
            retryJob.setErrorMessage(errorMessage);
            retryJob.setAttemptCount(0);
            retryJob.setMaxAttempts(defaultMaxAttempts);
            retryJob.setNextRetryAt(LocalDateTime.now());
            retryJobRepository.save(retryJob);
            mongoSyncService.sync(retryJob);
            log.warn("Recorded internal retry job {} for {} {}", retryJob.getId(), sourceType, sourceName);
        } catch (Exception recorderException) {
            log.error("Could not persist internal retry job for {} {}: {}", sourceType, sourceName, recorderException.getMessage(), recorderException);
        }
    }

    private boolean shouldRecordEndpointFailure(RuntimeException exception) {
        return !(exception instanceof ResponseStatusException responseStatusException
                && responseStatusException.getStatusCode().is4xxClientError());
    }

    private String serializeBody(Object value) {
        try {
            if (value instanceof String text) {
                return text;
            }
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload para retry interno", e);
        }
    }

    private void prepareForAttempt(RetryJob retryJob) {
        if (retryJob.getAttemptCount() == null) {
            retryJob.setAttemptCount(0);
        }
        if (retryJob.getMaxAttempts() == null || retryJob.getMaxAttempts() <= 0) {
            retryJob.setMaxAttempts(defaultMaxAttempts);
        }

        retryJob.setAttemptCount(retryJob.getAttemptCount() + 1);
        retryJob.setErrorMessage(null);
        retryJob.setStepAStatus(null);
        retryJob.setStepBStatus(null);
        retryJob.setStepCStatus(null);
    }

    private void handleFailure(RetryJob retryJob, String errorMessage) {
        retryJob.setErrorMessage(errorMessage);

        if (shouldMarkAsFailed(retryJob)) {
            retryJob.setStatus("FAILED");
            retryJob.setNextRetryAt(LocalDateTime.now());
            retryJobRepository.save(retryJob);
            mongoSyncService.sync(retryJob);
            return;
        }

        long delay = calculateBackoffDelaySeconds(retryJob.getAttemptCount());
        retryJob.setStatus("PENDING");
        retryJob.setNextRetryAt(LocalDateTime.now().plusSeconds(delay));
        retryJobRepository.save(retryJob);
        mongoSyncService.sync(retryJob);

        log.warn("Internal retry attempt {} failed for retryJobId={}. Next attempt in {} seconds. Error: {}",
                retryJob.getAttemptCount(), retryJob.getId(), delay, errorMessage);
    }

    private void finalizeSuccess(RetryJob retryJob) {
        retryJob.setStatus("SUCCESS");
        retryJob.setErrorMessage(null);
        retryJob.setNextRetryAt(LocalDateTime.now());
    }

    private boolean shouldMarkAsFailed(RetryJob retryJob) {
        return retryJob.getAttemptCount() >= retryJob.getMaxAttempts();
    }

    private long calculateBackoffDelaySeconds(int attemptCount) {
        long exponential = Math.round(baseDelaySeconds * Math.pow(2, Math.max(attemptCount - 1, 0)));
        return Math.min(exponential, maxDelaySeconds);
    }
}