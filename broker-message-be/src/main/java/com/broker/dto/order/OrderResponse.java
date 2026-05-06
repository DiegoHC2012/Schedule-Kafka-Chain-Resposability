package com.broker.dto.order;

import com.broker.model.common.OrderStatus;

import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderStatus status,
        boolean inventoryEventPublished
) {
}