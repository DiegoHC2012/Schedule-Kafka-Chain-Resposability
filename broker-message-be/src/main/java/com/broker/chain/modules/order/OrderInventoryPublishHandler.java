package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.dto.event.InventoryProductChangeEvent;
import com.broker.dto.event.InventoryUpdateEvent;
import com.broker.dto.order.OrderItemRequest;
import com.broker.service.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderInventoryPublishHandler extends AbstractEndpointHandler<OrderCreateContext> {

    private final DomainEventPublisher domainEventPublisher;

    @Override
    public void handle(OrderCreateContext context) {
        List<InventoryProductChangeEvent> changes = context.getRequest().products().stream()
                .map(this::toInventoryDecrease)
                .toList();

        InventoryUpdateEvent event = new InventoryUpdateEvent(
                context.getOrder().getId(),
                context.getOrder().getCustomerEmail(),
                "ORDER_CREATED",
                changes
        );

        domainEventPublisher.publishInventoryUpdate(event);
        context.setInventoryUpdateEvent(event);
        context.setInventoryEventPublished(true);
        handleNext(context);
    }

    private InventoryProductChangeEvent toInventoryDecrease(OrderItemRequest item) {
        return new InventoryProductChangeEvent(item.productId(), item.productName(), item.quantity() * -1);
    }
}