package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.service.notifications.EmailNotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreateEmailNotificationHandler extends AbstractEndpointHandler<OrderCreateContext> {

    private final EmailNotificationPublisher emailNotificationPublisher;

    @Override
    public void handle(OrderCreateContext context) {
        emailNotificationPublisher.publishOrderCreated(
                context.getOrder().getCustomerEmail(),
                context.getOrder().getId()
        );
        handleNext(context);
    }
}