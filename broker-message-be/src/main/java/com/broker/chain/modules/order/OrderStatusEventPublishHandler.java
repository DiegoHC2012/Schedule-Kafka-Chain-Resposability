package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.dto.event.OrderStatusChangedEvent;
import com.broker.service.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusEventPublishHandler extends AbstractEndpointHandler<OrderStatusUpdateContext> {

    private final DomainEventPublisher domainEventPublisher;

    @Override
    public void handle(OrderStatusUpdateContext context) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                context.getOrder().getId(),
                context.getOrder().getCustomerEmail(),
                context.getOrder().getStatus()
        );
        domainEventPublisher.publishOrderStatusChanged(event);
        context.setEventPublished(true);
        handleNext(context);
    }
}