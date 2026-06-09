package com.broker.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        String productId,
        String name,
        String image,
        BigDecimal price,
        Integer availableQuantity,
        LocalDateTime updatedAt
) {
}