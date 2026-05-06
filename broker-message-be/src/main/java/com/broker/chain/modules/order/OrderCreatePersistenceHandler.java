package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.dto.order.OrderItemRequest;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.model.common.OrderStatus;
import com.broker.model.order.OrderItemRecord;
import com.broker.model.order.OrderRecord;
import com.broker.repository.order.OrderRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatePersistenceHandler extends AbstractEndpointHandler<OrderCreateContext> {

    private final OrderRecordRepository orderRecordRepository;

    @Override
    public void handle(OrderCreateContext context) {
        OrderRecord orderRecord = new OrderRecord();
        orderRecord.setCustomerEmail(context.getRequest().customerEmail());
        orderRecord.setTotalAmount(context.getRequest().totalAmount());
        orderRecord.setRemainingBalance(context.getRequest().totalAmount());
        orderRecord.setStatus(OrderStatus.PENDIENTE_PAGO);

        for (OrderItemRequest product : context.getRequest().products()) {
            ProductInventoryDocument catalogProduct = context.getCatalogProducts().get(product.productId());
            OrderItemRecord itemRecord = new OrderItemRecord();
            itemRecord.setProductId(product.productId());
            itemRecord.setProductName(catalogProduct != null ? catalogProduct.getName() : product.productName());
            itemRecord.setQuantity(product.quantity());
            orderRecord.addItem(itemRecord);
        }

        context.setOrder(orderRecordRepository.save(orderRecord));
        handleNext(context);
    }
}