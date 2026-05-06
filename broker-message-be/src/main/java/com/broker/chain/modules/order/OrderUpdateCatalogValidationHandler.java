package com.broker.chain.modules.order;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.dto.order.OrderItemRequest;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.mongo.inventory.ProductInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OrderUpdateCatalogValidationHandler extends AbstractEndpointHandler<OrderProductsUpdateContext> {

    private final ProductInventoryRepository productInventoryRepository;

    @Override
    public void handle(OrderProductsUpdateContext context) {
        Set<String> productIds = new HashSet<>();
        for (OrderItemRequest item : context.getRequest().products()) {
            String productId = Objects.requireNonNull(item.productId(), "productId is required");
            if (!productIds.add(productId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permiten productos repetidos en la actualización");
            }

            ProductInventoryDocument catalogProduct = productInventoryRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "El producto " + productId + " no existe en el catalogo"
                    ));
            context.getCatalogProducts().put(productId, catalogProduct);
        }
        handleNext(context);
    }
}