package com.broker.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotBlank String productId,
        String productName,
        @NotNull @Min(1) Integer quantity
) {
}