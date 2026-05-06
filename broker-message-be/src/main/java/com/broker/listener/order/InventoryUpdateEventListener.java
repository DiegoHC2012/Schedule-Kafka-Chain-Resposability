package com.broker.listener.order;

import com.broker.config.KafkaTopics;
import com.broker.dto.event.InventoryUpdateEvent;
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
public class InventoryUpdateEventListener {

    private final ObjectMapper objectMapper;
    private final ModuleEventProcessor moduleEventProcessor;
    private final InternalRetryJobService internalRetryJobService;

    @KafkaListener(topics = KafkaTopics.INVENTORY_UPDATE_EVENTS, groupId = "broker-message-be-inventory-events")
    public void onInventoryUpdate(String message) {
        try {
            InventoryUpdateEvent event = objectMapper.readValue(message, InventoryUpdateEvent.class);
            moduleEventProcessor.processInventoryUpdate(event);
        } catch (Exception e) {
            log.error("Error processing inventory_update_events: {}", e.getMessage(), e);
            internalRetryJobService.recordEventFailure(
                    InternalRetryOperation.INVENTORY_UPDATE_EVENT,
                    KafkaTopics.INVENTORY_UPDATE_EVENTS,
                    message,
                    e
            );
        }
    }
}