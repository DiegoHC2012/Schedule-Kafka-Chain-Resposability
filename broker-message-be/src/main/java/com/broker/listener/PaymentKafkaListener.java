package com.broker.listener;

import com.broker.config.KafkaTopics;
import com.broker.model.RetryJob;
import com.broker.repository.RetryJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaListener {

    private final RetryJobRepository retryJobRepository;

    @KafkaListener(topics = KafkaTopics.PAYMENTS_RETRY_JOBS, groupId = "broker-message-be")
    public void listen(String message) {
        log.info("Received payment message from topic {}: {}", KafkaTopics.PAYMENTS_RETRY_JOBS, message);
        try {
            RetryJob retryJob = new RetryJob();
            retryJob.setTopic(KafkaTopics.PAYMENTS_RETRY_JOBS);
            retryJob.setPayload(message);
            retryJob.setStatus("PENDING");
            retryJobRepository.save(retryJob);
            log.info("Saved PENDING RetryJob for payment: {}", retryJob.getId());
        } catch (Exception e) {
            log.error("Error saving payment retry job: {}", e.getMessage(), e);
        }
    }
}
