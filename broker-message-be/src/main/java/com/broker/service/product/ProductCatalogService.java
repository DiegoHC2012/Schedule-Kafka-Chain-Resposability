package com.broker.service.product;

import com.broker.dto.product.ProductCreateRequest;
import com.broker.dto.product.ProductResponse;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.mongo.inventory.ProductInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductCatalogService {

    private final ProductInventoryRepository productInventoryRepository;

    public List<ProductResponse> listProducts() {
        return productInventoryRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        String productId = Objects.requireNonNull(request.productId(), "productId is required");
        ProductInventoryDocument product = productInventoryRepository.findById(productId)
                .orElseGet(ProductInventoryDocument::new);

        product.setId(productId);
        product.setName(request.name());
        product.setImage(normalize(request.image()));
        product.setAvailableQuantity(request.availableQuantity());
        product.setUpdatedAt(LocalDateTime.now());

        ProductInventoryDocument saved = productInventoryRepository.save(product);
        return toResponse(saved);
    }

    private ProductResponse toResponse(ProductInventoryDocument product) {
        return new ProductResponse(product.getId(), product.getName(), product.getImage(), product.getAvailableQuantity(), product.getUpdatedAt());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}