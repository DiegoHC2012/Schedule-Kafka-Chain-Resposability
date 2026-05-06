package com.broker.service;

import com.broker.chain.RetryContext;
import com.broker.chain.product.ProductCreationHandler;
import com.broker.chain.product.ProductEmailHandler;
import com.broker.chain.product.ProductUpdateStatusHandler;
import com.broker.config.KafkaTopics;
import com.broker.dto.RetryJobPayload;
import com.broker.model.ProductRetryJob;
import com.broker.model.RetryJob;
import com.broker.mongo.MongoSyncService;
import com.broker.repository.ProductRetryJobRepository;
import com.broker.repository.RetryJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRetryService {

    private final RetryJobRepository retryJobRepository;
    private final ProductRetryJobRepository productRetryJobRepository;
    private final ProductCreationHandler productCreationHandler;
    private final ProductEmailHandler productEmailHandler;
    private final ProductUpdateStatusHandler productUpdateStatusHandler;
    private final EmailService emailService;
    private final MongoSyncService mongoSyncService;
    private final ObjectMapper objectMapper;

    @Value("${app.retry.max-attempts:3}")
    private int defaultMaxAttempts;

    @Value("${app.retry.base-delay-seconds:10}")
    private int baseDelaySeconds;

    @Value("${app.retry.max-delay-seconds:300}")
    private int maxDelaySeconds;

    public void processRetries() {
        List<RetryJob> pending = retryJobRepository.findPendingDueByTopic(
                "PENDING", KafkaTopics.PRODUCT_RETRY_JOBS, LocalDateTime.now(), PageRequest.of(0, 100));

        log.info("ProductRetryService: processing {} pending product retry jobs", pending.size());

        for (RetryJob retryJob : pending) {
            try {
                prepareForAttempt(retryJob);
                RetryJobPayload payload = objectMapper.readValue(retryJob.getPayload(), RetryJobPayload.class);
                RetryContext context = new RetryContext(retryJob, payload);

                // Build chain: PASO A -> PASO B -> PASO C
                productCreationHandler.setNext(productEmailHandler);
                productEmailHandler.setNext(productUpdateStatusHandler);
                productUpdateStatusHandler.setNext(null);

                productCreationHandler.handle(context);
                applyPayloadStepState(context);
                retryJob.setPayload(writePayload(context.getPayload(), retryJob.getPayload()));

                if (!context.isSuccess()) {
                    handleFailure(retryJob, context.getErrorMessage());
                } else {
                    finalizeSuccess(retryJob);
                    retryJobRepository.save(retryJob);
                    mongoSyncService.sync(retryJob);
                    mongoSyncService.syncProductSuccess(retryJob);
                }
            } catch (Exception e) {
                log.error("Unexpected error processing product retryJob {}: {}", retryJob.getId(), e.getMessage(), e);
                handleFailure(retryJob, e.getMessage());
            }
        }
    }

    private void applyPayloadStepState(RetryContext context) {
        RetryJob retryJob = context.getRetryJob();
        RetryJobPayload payload = context.getPayload();

        if (payload.getData() != null && retryJob.getStepAStatus() != null) {
            payload.getData().put("status", retryJob.getStepAStatus());
        }

        if (retryJob.getStepBStatus() != null) {
            if (payload.getSendEmail() == null) {
                payload.setSendEmail(new RetryJobPayload.StepStatus());
            }
            payload.getSendEmail().setStatus(retryJob.getStepBStatus());
            if ("SUCCESS".equals(retryJob.getStepBStatus())) {
                payload.getSendEmail().setMessage("Correo de producto enviado correctamente");
            } else {
                payload.getSendEmail().setMessage(context.getErrorMessage());
            }
        }

        if (retryJob.getStepCStatus() != null) {
            if (payload.getUpdateRetryJobs() == null) {
                payload.setUpdateRetryJobs(new RetryJobPayload.StepStatus());
            }
            payload.getUpdateRetryJobs().setStatus(retryJob.getStepCStatus());
            if ("SUCCESS".equals(retryJob.getStepCStatus())) {
                payload.getUpdateRetryJobs().setMessage("RetryJob de producto actualizado correctamente");
            } else {
                payload.getUpdateRetryJobs().setMessage(context.getErrorMessage());
            }
        }
    }

    private String writePayload(RetryJobPayload payload, String fallbackPayload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Could not serialize product payload with step statuses: {}", e.getMessage());
            return fallbackPayload;
        }
    }

    private void handleFailure(RetryJob retryJob, String errorMessage) {
        retryJob.setErrorMessage(errorMessage);

        if (shouldMarkAsFailed(retryJob)) {
            markAsFailed(retryJob, errorMessage);
            return;
        }

        long delay = calculateBackoffDelaySeconds(retryJob.getAttemptCount());
        retryJob.setStatus("PENDING");
        retryJob.setNextRetryAt(LocalDateTime.now().plusSeconds(delay));
        retryJobRepository.save(retryJob);
        mongoSyncService.sync(retryJob);

        log.warn("Product retry attempt {} failed for retryJobId={}. Next attempt in {} seconds. Error: {}",
                retryJob.getAttemptCount(), retryJob.getId(), delay, errorMessage);
    }

    private void markAsFailed(RetryJob retryJob, String errorMessage) {
        log.warn("Product retry exhausted attempts for retryJobId={}: {}", retryJob.getId(), errorMessage);

        retryJob.setStatus("FAILED");
        retryJob.setNextRetryAt(LocalDateTime.now());
        retryJobRepository.save(retryJob);
        mongoSyncService.sync(retryJob);

        // Save to product_retry_jobs table
        ProductRetryJob failedJob = new ProductRetryJob();
        failedJob.setRetryJob(retryJob);
        failedJob.setPayload(retryJob.getPayload());
        failedJob.setErrorMessage(errorMessage);
        productRetryJobRepository.save(failedJob);
        mongoSyncService.syncProductFailed(failedJob);

        // Send failure email and record result in payload
        try {
            emailService.sendFailureEmail(retryJob, errorMessage);
            updatePayloadFailureNotification(retryJob, "SUCCESS", "Correo de fallo enviado correctamente");
        } catch (Exception mailEx) {
            log.error("Could not send failure email for product retryJobId={}: {}", retryJob.getId(), mailEx.getMessage());
            updatePayloadFailureNotification(retryJob, "FAILED", mailEx.getMessage());
        }
        retryJobRepository.save(retryJob);
        mongoSyncService.sync(retryJob);
    }

    private void updatePayloadFailureNotification(RetryJob retryJob, String status, String message) {
        try {
            RetryJobPayload payload = objectMapper.readValue(retryJob.getPayload(), RetryJobPayload.class);
            RetryJobPayload.StepStatus notif = new RetryJobPayload.StepStatus();
            notif.setStatus(status);
            notif.setMessage(message);
            payload.setFailureNotification(notif);
            retryJob.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("Could not update failure notification in payload: {}", e.getMessage());
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
