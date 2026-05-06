package com.broker.dto.product;

import java.time.LocalDateTime;

public record ProductResponse(
        String productId,
        String name,
        String image,
        Integer availableQuantity,
        LocalDateTime updatedAt
) {
}