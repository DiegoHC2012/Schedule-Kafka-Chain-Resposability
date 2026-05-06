package com.broker.service.inventory;

import com.broker.dto.event.InventoryUpdateEvent;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.mongo.inventory.ProductInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryProjectionService {

    private final ProductInventoryRepository productInventoryRepository;

    public void apply(InventoryUpdateEvent event) {
        String eventKey = buildEventKey(event);

        event.changes().forEach(change -> {
            String productId = Objects.requireNonNull(change.productId(), "productId is required");
            ProductInventoryDocument product = productInventoryRepository.findById(productId)
                    .orElseGet(ProductInventoryDocument::new);

            Set<String> appliedEvents = product.getAppliedInventoryEvents() != null
                    ? new HashSet<>(product.getAppliedInventoryEvents())
                    : new HashSet<>();

            if (appliedEvents.contains(eventKey)) {
                return;
            }

            product.setId(productId);
            product.setName(change.productName());
            int current = product.getAvailableQuantity() != null ? product.getAvailableQuantity() : 0;
            product.setAvailableQuantity(current + change.quantityDelta());
            appliedEvents.add(eventKey);
            product.setAppliedInventoryEvents(appliedEvents);
            product.setUpdatedAt(LocalDateTime.now());
            productInventoryRepository.save(product);
        });
    }

    private String buildEventKey(InventoryUpdateEvent event) {
        String changeFingerprint = event.changes().stream()
                .sorted(Comparator.comparing(change -> Objects.toString(change.productId(), "")))
                .map(change -> Objects.toString(change.productId(), "") + ":" + change.quantityDelta())
                .collect(Collectors.joining(","));

        return Objects.toString(event.orderId(), "no-order")
                + "|" + Objects.toString(event.reason(), "no-reason")
                + "|" + changeFingerprint;
    }
}