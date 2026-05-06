package com.broker.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrderProductsUpdateRequest(
        @NotNull UUID orderId,
        @NotEmpty List<@Valid OrderItemRequest> products
) {
}