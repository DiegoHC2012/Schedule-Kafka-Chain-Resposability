package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.repository.order.OrderRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OrderStatusValidationHandler extends AbstractEndpointHandler<OrderStatusUpdateContext> {

    private final OrderRecordRepository orderRecordRepository;

    @Override
    public void handle(OrderStatusUpdateContext context) {
        context.setOrder(orderRecordRepository.findById(Objects.requireNonNull(context.getRequest().orderId(), "orderId is required"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada")));
        handleNext(context);
    }
}