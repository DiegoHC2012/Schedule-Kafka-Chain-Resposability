package com.broker.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductCreateRequest(
        @NotBlank String productId,
        @NotBlank String name,
        String image,
        @NotNull @Min(0) Integer availableQuantity
) {
}