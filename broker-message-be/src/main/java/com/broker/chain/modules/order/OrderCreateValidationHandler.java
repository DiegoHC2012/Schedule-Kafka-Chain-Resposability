package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.dto.order.OrderItemRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Component
public class OrderCreateValidationHandler extends AbstractEndpointHandler<OrderCreateContext> {

    @Override
    public void handle(OrderCreateContext context) {
        Set<String> productIds = new HashSet<>();
        for (OrderItemRequest product : context.getRequest().products()) {
            if (!productIds.add(product.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permiten productos repetidos en la orden");
            }
        }
        handleNext(context);
    }
}