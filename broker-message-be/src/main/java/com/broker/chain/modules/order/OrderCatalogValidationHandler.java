package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.dto.order.OrderItemRequest;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.mongo.inventory.ProductInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OrderCatalogValidationHandler extends AbstractEndpointHandler<OrderCreateContext> {

    private final ProductInventoryRepository productInventoryRepository;

    @Override
    public void handle(OrderCreateContext context) {
        Map<String, Integer> requestedQuantities = new LinkedHashMap<>();

        for (OrderItemRequest product : context.getRequest().products()) {
            String productId = Objects.requireNonNull(product.productId(), "productId is required");
            int requestedQuantity = product.quantity() != null ? product.quantity() : 0;
            ProductInventoryDocument catalogProduct = productInventoryRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "El producto " + productId + " no existe en el catalogo"
                    ));
            context.getCatalogProducts().put(productId, catalogProduct);
                int accumulatedQuantity = requestedQuantities.getOrDefault(productId, 0);
                requestedQuantities.put(productId, accumulatedQuantity + requestedQuantity);
        }

        requestedQuantities.forEach((productId, requestedQuantity) -> {
            ProductInventoryDocument catalogProduct = context.getCatalogProducts().get(productId);
            int availableQuantity = catalogProduct.getAvailableQuantity() != null ? catalogProduct.getAvailableQuantity() : 0;

            if (requestedQuantity > availableQuantity) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El producto " + productId + " solo tiene " + availableQuantity + " unidades disponibles"
                );
            }
        });

        handleNext(context);
    }
}