package com.broker.dto.event;

import com.broker.model.common.OrderStatus;

import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        String customerEmail,
        OrderStatus status
) {
}