package com.broker.chain.modules.order;

import com.broker.dto.event.InventoryUpdateEvent;
import com.broker.dto.order.OrderCreateRequest;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.model.order.OrderRecord;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class OrderCreateContext {

    private final OrderCreateRequest request;
    private OrderRecord order;
    private InventoryUpdateEvent inventoryUpdateEvent;
    private boolean inventoryEventPublished;
    private final Map<String, ProductInventoryDocument> catalogProducts = new HashMap<>();

    public OrderCreateContext(OrderCreateRequest request) {
        this.request = request;
    }
}