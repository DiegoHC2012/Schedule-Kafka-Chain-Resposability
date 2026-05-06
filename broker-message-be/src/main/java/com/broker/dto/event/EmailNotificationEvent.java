package com.broker.dto.event;

import java.util.Map;

public record EmailNotificationEvent(
        String template,
        String recipient,
        String subject,
        Map<String, Object> payload
) {
}