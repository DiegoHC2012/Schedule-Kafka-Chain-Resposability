package com.broker.chain.modules.order;

import com.broker.dto.event.InventoryUpdateEvent;
import com.broker.dto.order.OrderProductsUpdateRequest;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.model.order.OrderRecord;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class OrderProductsUpdateContext {

    private final OrderProductsUpdateRequest request;
    private OrderRecord order;
    private Map<String, Integer> previousQuantities = new HashMap<>();
    private Map<String, String> previousNames = new HashMap<>();
    private Map<String, ProductInventoryDocument> catalogProducts = new HashMap<>();
    private boolean productsChanged;
    private InventoryUpdateEvent inventoryUpdateEvent;
    private boolean inventoryEventPublished;

    public OrderProductsUpdateContext(OrderProductsUpdateRequest request) {
        this.request = request;
    }
}