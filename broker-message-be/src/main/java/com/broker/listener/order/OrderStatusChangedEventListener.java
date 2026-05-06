package com.broker.listener.order;

import com.broker.config.KafkaTopics;
import com.broker.dto.event.OrderStatusChangedEvent;
import com.broker.retry.InternalRetryOperation;
import com.broker.service.internal.InternalRetryJobService;
import com.broker.service.internal.ModuleEventProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusChangedEventListener {

    private final ObjectMapper objectMapper;
    private final ModuleEventProcessor moduleEventProcessor;
    private final InternalRetryJobService internalRetryJobService;

    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_CHANGED_EVENTS, groupId = "broker-message-be-order-status-events")
    public void onOrderStatusChanged(String message) {
        try {
            OrderStatusChangedEvent event = objectMapper.readValue(message, OrderStatusChangedEvent.class);
            moduleEventProcessor.processOrderStatusChanged(event);
        } catch (Exception e) {
            log.error("Error processing order_status_changed_events: {}", e.getMessage(), e);
            internalRetryJobService.recordEventFailure(
                    InternalRetryOperation.ORDER_STATUS_CHANGED_EVENT,
                    KafkaTopics.ORDER_STATUS_CHANGED_EVENTS,
                    message,
                    e
            );
        }
    }
}