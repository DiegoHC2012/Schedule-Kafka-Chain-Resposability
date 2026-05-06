package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.dto.order.OrderItemRequest;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.model.order.OrderItemRecord;
import com.broker.repository.order.OrderRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OrderUpdatePersistenceHandler extends AbstractEndpointHandler<OrderProductsUpdateContext> {

    private final OrderRecordRepository orderRecordRepository;

    @Override
    public void handle(OrderProductsUpdateContext context) {
        if (context.isProductsChanged()) {
            List<OrderItemRecord> updatedItems = new ArrayList<>();
            for (OrderItemRequest item : context.getRequest().products()) {
                ProductInventoryDocument catalogProduct = context.getCatalogProducts().get(item.productId());
                OrderItemRecord record = new OrderItemRecord();
                record.setProductId(item.productId());
                record.setProductName(catalogProduct != null ? catalogProduct.getName() : item.productName());
                record.setQuantity(item.quantity());
                updatedItems.add(record);
            }
            context.getOrder().replaceItems(updatedItems);
            context.setOrder(orderRecordRepository.save(Objects.requireNonNull(context.getOrder(), "order is required")));
        }
        handleNext(context);
    }
}