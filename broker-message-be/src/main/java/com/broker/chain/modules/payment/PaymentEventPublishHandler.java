package com.broker.chain.modules.payment;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.dto.event.PaymentReceivedEvent;
import com.broker.service.events.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublishHandler extends AbstractEndpointHandler<PaymentCommandContext> {

    private final DomainEventPublisher domainEventPublisher;

    @Override
    public void handle(PaymentCommandContext context) {
        PaymentReceivedEvent event = new PaymentReceivedEvent(
                context.getPaymentRecord().getId(),
                context.getPaymentRecord().getOrderId(),
                context.getPaymentRecord().getCustomerEmail(),
                context.getPaymentRecord().getAmount(),
                context.getPaymentRecord().getRemainingBalance()
        );
        domainEventPublisher.publishPaymentReceived(event);
        context.setEventPublished(true);
        handleNext(context);
    }
}