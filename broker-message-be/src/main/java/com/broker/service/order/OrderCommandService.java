package com.broker.service.order;

import com.broker.chain.modules.order.OrderCreateContext;
import com.broker.chain.modules.order.OrderCatalogValidationHandler;
import com.broker.chain.modules.order.OrderCreatePersistenceHandler;
import com.broker.chain.modules.order.OrderCreateValidationHandler;
import com.broker.chain.modules.order.OrderInventoryPublishHandler;
import com.broker.chain.modules.order.OrderInventoryUpdatePublishHandler;
import com.broker.chain.modules.order.OrderProductChangeDetectionHandler;
import com.broker.chain.modules.order.OrderProductsUpdateContext;
import com.broker.chain.modules.order.OrderStatusEventPublishHandler;
import com.broker.chain.modules.order.OrderStatusPersistenceHandler;
import com.broker.chain.modules.order.OrderStatusUpdateContext;
import com.broker.chain.modules.order.OrderStatusValidationHandler;
import com.broker.chain.modules.order.OrderUpdateCatalogValidationHandler;
import com.broker.chain.modules.order.OrderUpdateLookupHandler;
import com.broker.chain.modules.order.OrderUpdatePersistenceHandler;
import com.broker.dto.order.OrderCreateRequest;
import com.broker.dto.order.OrderProductsUpdateRequest;
import com.broker.dto.order.OrderResponse;
import com.broker.dto.order.OrderStatusUpdateRequest;
import com.broker.dto.order.OrderStatusUpdateResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderCreateValidationHandler orderCreateValidationHandler;
    private final OrderCatalogValidationHandler orderCatalogValidationHandler;
    private final OrderCreatePersistenceHandler orderCreatePersistenceHandler;
    private final OrderInventoryPublishHandler orderInventoryPublishHandler;
    private final OrderUpdateLookupHandler orderUpdateLookupHandler;
    private final OrderUpdateCatalogValidationHandler orderUpdateCatalogValidationHandler;
    private final OrderProductChangeDetectionHandler orderProductChangeDetectionHandler;
    private final OrderUpdatePersistenceHandler orderUpdatePersistenceHandler;
    private final OrderInventoryUpdatePublishHandler orderInventoryUpdatePublishHandler;
    private final OrderStatusValidationHandler orderStatusValidationHandler;
    private final OrderStatusPersistenceHandler orderStatusPersistenceHandler;
    private final OrderStatusEventPublishHandler orderStatusEventPublishHandler;

    @PostConstruct
    public void initChains() {
        orderCreateValidationHandler.setNext(orderCatalogValidationHandler);
        orderCatalogValidationHandler.setNext(orderCreatePersistenceHandler);
        orderCreatePersistenceHandler.setNext(orderInventoryPublishHandler);

        orderUpdateLookupHandler.setNext(orderUpdateCatalogValidationHandler);
        orderUpdateCatalogValidationHandler.setNext(orderProductChangeDetectionHandler);
        orderProductChangeDetectionHandler.setNext(orderUpdatePersistenceHandler);
        orderUpdatePersistenceHandler.setNext(orderInventoryUpdatePublishHandler);

        orderStatusValidationHandler.setNext(orderStatusPersistenceHandler);
        orderStatusPersistenceHandler.setNext(orderStatusEventPublishHandler);
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        OrderCreateContext context = new OrderCreateContext(request);
        orderCreateValidationHandler.handle(context);
        return new OrderResponse(context.getOrder().getId(), context.getOrder().getStatus(), context.isInventoryEventPublished());
    }

    @Transactional
    public OrderResponse updateOrderProducts(OrderProductsUpdateRequest request) {
        OrderProductsUpdateContext context = new OrderProductsUpdateContext(request);
        orderUpdateLookupHandler.handle(context);
        return new OrderResponse(context.getOrder().getId(), context.getOrder().getStatus(), context.isInventoryEventPublished());
    }

    @Transactional
    public OrderStatusUpdateResponse updateStatus(OrderStatusUpdateRequest request) {
        OrderStatusUpdateContext context = new OrderStatusUpdateContext(request);
        orderStatusValidationHandler.handle(context);
        return new OrderStatusUpdateResponse(context.getOrder().getId(), context.getOrder().getStatus(), context.isEventPublished());
    }
}