package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderProductChangeDetectionHandler extends AbstractEndpointHandler<OrderProductsUpdateContext> {

    @Override
    public void handle(OrderProductsUpdateContext context) {
        var incomingQuantities = new java.util.HashMap<String, Integer>();
        var incomingNames = new java.util.HashMap<String, String>();

        context.getRequest().products().forEach(item -> {
            incomingQuantities.put(item.productId(), item.quantity());
            String catalogName = context.getCatalogProducts().containsKey(item.productId())
                    ? context.getCatalogProducts().get(item.productId()).getName()
                    : item.productName();
            incomingNames.put(item.productId(), catalogName);
        });

        boolean quantitiesChanged = !context.getPreviousQuantities().equals(incomingQuantities);
        boolean namesChanged = !context.getPreviousNames().equals(incomingNames);

        context.setProductsChanged(quantitiesChanged || namesChanged);
        handleNext(context);
    }
}