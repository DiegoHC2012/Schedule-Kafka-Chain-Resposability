package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.dto.event.InventoryProductChangeEvent;
import com.broker.dto.event.InventoryUpdateEvent;
import com.broker.dto.order.OrderItemRequest;
import com.broker.service.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OrderInventoryUpdatePublishHandler extends AbstractEndpointHandler<OrderProductsUpdateContext> {

    private final DomainEventPublisher domainEventPublisher;

    @Override
    public void handle(OrderProductsUpdateContext context) {
        if (context.isProductsChanged()) {
            Map<String, Integer> currentQuantities = new HashMap<>();
            Map<String, String> currentNames = new HashMap<>();
            for (OrderItemRequest item : context.getRequest().products()) {
                currentQuantities.put(item.productId(), item.quantity());
                currentNames.put(item.productId(), item.productName());
            }

            Set<String> allProductIds = new HashSet<>();
            allProductIds.addAll(context.getPreviousQuantities().keySet());
            allProductIds.addAll(currentQuantities.keySet());

            List<InventoryProductChangeEvent> changes = new ArrayList<>();
            for (String productId : allProductIds) {
                int previous = context.getPreviousQuantities().getOrDefault(productId, 0);
                int current = currentQuantities.getOrDefault(productId, 0);
                int delta = previous - current;
                if (delta != 0) {
                    changes.add(new InventoryProductChangeEvent(
                            productId,
                            currentNames.getOrDefault(productId, context.getPreviousNames().getOrDefault(productId, productId)),
                            delta
                    ));
                }
            }

            InventoryUpdateEvent event = new InventoryUpdateEvent(
                    context.getOrder().getId(),
                    context.getOrder().getCustomerEmail(),
                    "ORDER_PRODUCTS_UPDATED",
                    changes
            );

            domainEventPublisher.publishInventoryUpdate(event);
            context.setInventoryUpdateEvent(event);
            context.setInventoryEventPublished(true);
        }

        handleNext(context);
    }
}