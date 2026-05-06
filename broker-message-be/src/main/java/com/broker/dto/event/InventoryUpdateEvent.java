package com.broker.dto.event;

import java.util.List;
import java.util.UUID;

public record InventoryUpdateEvent(
        UUID orderId,
        String customerEmail,
        String reason,
        List<InventoryProductChangeEvent> changes
) {
}