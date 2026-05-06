package com.broker.chain.modules.order;

import com.broker.dto.order.OrderStatusUpdateRequest;
import com.broker.model.order.OrderRecord;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateContext {

    private final OrderStatusUpdateRequest request;
    private OrderRecord order;
    private boolean eventPublished;

    public OrderStatusUpdateContext(OrderStatusUpdateRequest request) {
        this.request = request;
    }
}