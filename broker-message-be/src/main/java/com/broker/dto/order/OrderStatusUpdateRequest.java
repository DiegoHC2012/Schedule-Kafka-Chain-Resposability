package com.broker.dto.order;

import com.broker.model.common.OrderStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderStatusUpdateRequest(
        @NotNull UUID orderId,
        @NotNull OrderStatus status
) {
}