package com.broker.listener;

import com.broker.config.KafkaTopics;
import com.broker.model.RetryJob;
import com.broker.mongo.MongoSyncService;
import com.broker.repository.RetryJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductKafkaListener {

    private final RetryJobRepository retryJobRepository;
    private final MongoSyncService mongoSyncService;

    @KafkaListener(topics = KafkaTopics.PRODUCT_RETRY_JOBS, groupId = "broker-message-be")
    public void listen(String message) {
        log.info("Received product message from topic {}: {}", KafkaTopics.PRODUCT_RETRY_JOBS, message);
        try {
            RetryJob retryJob = new RetryJob();
            retryJob.setTopic(KafkaTopics.PRODUCT_RETRY_JOBS);
            retryJob.setPayload(message);
            retryJob.setStatus("PENDING");
            retryJobRepository.save(retryJob);
            mongoSyncService.sync(retryJob);
            log.info("Saved PENDING RetryJob for product: {}", retryJob.getId());
        } catch (Exception e) {
            log.error("Error saving product retry job: {}", e.getMessage(), e);
        }
    }
}
