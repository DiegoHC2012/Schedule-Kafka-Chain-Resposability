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

    public void processRetries() {
        List<RetryJob> pending = retryJobRepository.findByStatusAndTopic(
                "PENDING", KafkaTopics.PAYMENTS_RETRY_JOBS, PageRequest.of(0, 100));

        log.info("PaymentRetryService: processing {} pending payment retry jobs", pending.size());

        for (RetryJob retryJob : pending) {
            try {
                RetryJobPayload payload = objectMapper.readValue(retryJob.getPayload(), RetryJobPayload.class);
                RetryContext context = new RetryContext(retryJob, payload);

                // Build chain: PASO A -> PASO B -> PASO C
                paymentCreationHandler.setNext(paymentEmailHandler);
                paymentEmailHandler.setNext(paymentUpdateStatusHandler);
                paymentUpdateStatusHandler.setNext(null);

                paymentCreationHandler.handle(context);

                if (!context.isSuccess()) {
                    handleFailure(retryJob, context.getErrorMessage());
                }
            } catch (Exception e) {
                log.error("Unexpected error processing payment retryJob {}: {}", retryJob.getId(), e.getMessage(), e);
                handleFailure(retryJob, e.getMessage());
            }
        }
    }

    private void handleFailure(RetryJob retryJob, String errorMessage) {
        log.warn("Payment retry failed for retryJobId={}: {}", retryJob.getId(), errorMessage);

        // Save to payments_retry_jobs table
        PaymentRetryJob failedJob = new PaymentRetryJob();
        failedJob.setRetryJob(retryJob);
        failedJob.setPayload(retryJob.getPayload());
        failedJob.setErrorMessage(errorMessage);
        paymentRetryJobRepository.save(failedJob);

        // Update retry_job status to FAILED
        retryJob.setStatus("FAILED");
        retryJob.setErrorMessage(errorMessage);
        retryJobRepository.save(retryJob);

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
}
