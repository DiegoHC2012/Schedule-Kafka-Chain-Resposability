package com.broker.listener.notification;

import com.broker.config.KafkaTopics;
import com.broker.dto.event.EmailNotificationEvent;
import com.broker.retry.InternalRetryOperation;
import com.broker.service.EmailService;
import com.broker.service.internal.InternalRetryJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationEventListener {

    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final InternalRetryJobService internalRetryJobService;

    @KafkaListener(topics = KafkaTopics.EMAIL_NOTIFICATION_EVENTS, groupId = "broker-message-be-email-events")
    public void onEmailNotification(String message) {
        try {
            EmailNotificationEvent event = objectMapper.readValue(message, EmailNotificationEvent.class);
            emailService.sendNotificationEmail(event);
        } catch (Exception e) {
            log.error("Error processing email_notification_events: {}", e.getMessage(), e);
            internalRetryJobService.recordEventFailure(
                    InternalRetryOperation.EMAIL_NOTIFICATION_EVENT,
                    KafkaTopics.EMAIL_NOTIFICATION_EVENTS,
                    message,
                    e
            );
        }
    }
}