package com.broker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.web.client.RestTemplate;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public NewTopic paymentsRetryJobsTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENTS_RETRY_JOBS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderRetryJobsTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_RETRY_JOBS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productRetryJobsTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCT_RETRY_JOBS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentReceivedEventsTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_RECEIVED_EVENTS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryUpdateEventsTopic() {
        return TopicBuilder.name(KafkaTopics.INVENTORY_UPDATE_EVENTS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderStatusChangedEventsTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_STATUS_CHANGED_EVENTS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic emailNotificationEventsTopic() {
        return TopicBuilder.name(KafkaTopics.EMAIL_NOTIFICATION_EVENTS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
