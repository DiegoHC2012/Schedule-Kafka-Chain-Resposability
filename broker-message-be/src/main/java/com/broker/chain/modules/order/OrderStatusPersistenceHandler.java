package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.model.common.OrderStatus;
import com.broker.repository.order.OrderRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OrderStatusPersistenceHandler extends AbstractEndpointHandler<OrderStatusUpdateContext> {

    private final OrderRecordRepository orderRecordRepository;

    @Override
    public void handle(OrderStatusUpdateContext context) {
        context.getOrder().setStatus(context.getRequest().status());
        if (context.getRequest().status() == OrderStatus.PAGADO) {
            context.getOrder().setRemainingBalance(BigDecimal.ZERO);
        }
        context.setOrder(orderRecordRepository.save(Objects.requireNonNull(context.getOrder(), "order is required")));
        handleNext(context);
    }
}