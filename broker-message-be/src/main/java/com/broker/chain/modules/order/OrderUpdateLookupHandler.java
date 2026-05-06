package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.model.order.OrderItemRecord;
import com.broker.model.order.OrderRecord;
import com.broker.repository.order.OrderRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class OrderUpdateLookupHandler extends AbstractEndpointHandler<OrderProductsUpdateContext> {

    private final OrderRecordRepository orderRecordRepository;

    @Override
    public void handle(OrderProductsUpdateContext context) {
        OrderRecord orderRecord = orderRecordRepository.findWithItemsById(context.getRequest().orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));

        context.setOrder(orderRecord);
        for (OrderItemRecord item : orderRecord.getItems()) {
            context.getPreviousQuantities().put(item.getProductId(), item.getQuantity());
            context.getPreviousNames().put(item.getProductId(), item.getProductName());
        }
        handleNext(context);
    }
}