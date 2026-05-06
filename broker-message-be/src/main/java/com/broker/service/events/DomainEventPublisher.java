package com.broker.service.events;

import com.broker.config.KafkaTopics;
import com.broker.dto.event.InventoryUpdateEvent;
import com.broker.dto.event.OrderStatusChangedEvent;
import com.broker.dto.event.PaymentReceivedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishPaymentReceived(PaymentReceivedEvent event) {
        send(KafkaTopics.PAYMENT_RECEIVED_EVENTS, event);
    }

    public void publishInventoryUpdate(InventoryUpdateEvent event) {
        send(KafkaTopics.INVENTORY_UPDATE_EVENTS, event);
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        send(KafkaTopics.ORDER_STATUS_CHANGED_EVENTS, event);
    }

    private void send(String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(
                    Objects.requireNonNull(topic, "topic is required"),
                    Objects.requireNonNull(payload, "payload is required")
            );
            log.info("Published domain event to topic {}", topic);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento para Kafka", e);
        }
    }
}