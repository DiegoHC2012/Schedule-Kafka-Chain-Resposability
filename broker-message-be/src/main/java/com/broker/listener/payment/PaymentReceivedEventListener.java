package com.broker.listener.payment;

import com.broker.config.KafkaTopics;
import com.broker.dto.event.PaymentReceivedEvent;
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
public class PaymentReceivedEventListener {

    private final ObjectMapper objectMapper;
    private final ModuleEventProcessor moduleEventProcessor;
    private final InternalRetryJobService internalRetryJobService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_RECEIVED_EVENTS, groupId = "broker-message-be-payment-events")
    public void onPaymentReceived(String message) {
        try {
            PaymentReceivedEvent event = objectMapper.readValue(message, PaymentReceivedEvent.class);
            moduleEventProcessor.processPaymentReceived(event);
        } catch (Exception e) {
            log.error("Error processing payment_received_events: {}", e.getMessage(), e);
            internalRetryJobService.recordEventFailure(
                    InternalRetryOperation.PAYMENT_RECEIVED_EVENT,
                    KafkaTopics.PAYMENT_RECEIVED_EVENTS,
                    message,
                    e
            );
        }
    }
}