package com.broker.service;

import com.broker.chain.RetryContext;
import com.broker.chain.payment.PaymentCreationHandler;
import com.broker.chain.payment.PaymentEmailHandler;
import com.broker.chain.payment.PaymentUpdateStatusHandler;
import com.broker.config.KafkaTopics;
import com.broker.dto.RetryJobPayload;
import com.broker.model.PaymentRetryJob;
import com.broker.model.RetryJob;
import com.broker.repository.PaymentRetryJobRepository;
import com.broker.repository.RetryJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRetryService {

    private final RetryJobRepository retryJobRepository;
    private final PaymentRetryJobRepository paymentRetryJobRepository;
    private final PaymentCreationHandler paymentCreationHandler;
    private final PaymentEmailHandler paymentEmailHandler;
    private final PaymentUpdateStatusHandler paymentUpdateStatusHandler;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Value("${app.email.from}")
    private String from;

    @Value("${app.email.failure-recipient}")
    private String failureRecipient;

    @Value("${app.retry.max-attempts:3}")
    private int defaultMaxAttempts;

    @Value("${app.retry.base-delay-seconds:10}")
    private int baseDelaySeconds;

    @Value("${app.retry.max-delay-seconds:300}")
    private int maxDelaySeconds;

    public void processRetries() {
        List<RetryJob> pending = retryJobRepository.findPendingDueByTopic(
                "PENDING", KafkaTopics.PAYMENTS_RETRY_JOBS, LocalDateTime.now(), PageRequest.of(0, 100));

        log.info("PaymentRetryService: processing {} pending payment retry jobs", pending.size());

        for (RetryJob retryJob : pending) {
            try {
                prepareForAttempt(retryJob);
                RetryJobPayload payload = objectMapper.readValue(retryJob.getPayload(), RetryJobPayload.class);
                RetryContext context = new RetryContext(retryJob, payload);

                // Build chain: PASO A -> PASO B -> PASO C
                paymentCreationHandler.setNext(paymentEmailHandler);
                paymentEmailHandler.setNext(paymentUpdateStatusHandler);
                paymentUpdateStatusHandler.setNext(null);

                paymentCreationHandler.handle(context);
                applyPayloadStepState(context);
                retryJob.setPayload(writePayload(context.getPayload(), retryJob.getPayload()));

                if (!context.isSuccess()) {
                    handleFailure(retryJob, context.getErrorMessage());
                } else {
                    finalizeSuccess(retryJob);
                    retryJobRepository.save(retryJob);
                }
            } catch (Exception e) {
                log.error("Unexpected error processing payment retryJob {}: {}", retryJob.getId(), e.getMessage(), e);
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
                payload.getSendEmail().setMessage("Correo de pago enviado correctamente");
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
                payload.getUpdateRetryJobs().setMessage("RetryJob de pago actualizado correctamente");
            } else {
                payload.getUpdateRetryJobs().setMessage(context.getErrorMessage());
            }
        }
    }

    private String writePayload(RetryJobPayload payload, String fallbackPayload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Could not serialize payment payload with step statuses: {}", e.getMessage());
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

        log.warn("Payment retry attempt {} failed for retryJobId={}. Next attempt in {} seconds. Error: {}",
                retryJob.getAttemptCount(), retryJob.getId(), delay, errorMessage);
    }

    private void markAsFailed(RetryJob retryJob, String errorMessage) {
        log.warn("Payment retry exhausted attempts for retryJobId={}: {}", retryJob.getId(), errorMessage);

        retryJob.setStatus("FAILED");
        retryJob.setNextRetryAt(LocalDateTime.now());
        retryJobRepository.save(retryJob);

        // Save to payments_retry_jobs table
        PaymentRetryJob failedJob = new PaymentRetryJob();
        failedJob.setRetryJob(retryJob);
        failedJob.setPayload(retryJob.getPayload());
        failedJob.setErrorMessage(errorMessage);
        paymentRetryJobRepository.save(failedJob);

        // Send failure email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(failureRecipient);
            message.setSubject("Error en procesamiento de pago");
            message.setText("El pago con RetryJob ID " + retryJob.getId() + " falló.\n\nError: " + errorMessage + "\n\nPayload: " + retryJob.getPayload());
            mailSender.send(message);
        } catch (Exception mailEx) {
            log.error("Could not send failure email for payment retryJobId={}: {}", retryJob.getId(), mailEx.getMessage());
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
