package com.broker.dto.event;

public record InventoryProductChangeEvent(
        String productId,
        String productName,
        int quantityDelta
) {
}