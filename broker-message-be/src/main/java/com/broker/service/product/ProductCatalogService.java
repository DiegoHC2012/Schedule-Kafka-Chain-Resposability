package com.broker.service.product;

import com.broker.dto.product.ProductCreateRequest;
import com.broker.dto.product.ProductResponse;
import com.broker.dto.product.ProductUpdateRequest;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.mongo.inventory.ProductInventoryRepository;
import com.broker.repository.order.OrderRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductCatalogService {

    private final ProductInventoryRepository productInventoryRepository;
    private final OrderRecordRepository orderRecordRepository;

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
        product.setPrice(request.price());
        product.setAvailableQuantity(request.availableQuantity());
        product.setUpdatedAt(LocalDateTime.now());

        ProductInventoryDocument saved = productInventoryRepository.save(product);
        return toResponse(saved);
    }

    public void updateProduct(String productId, ProductUpdateRequest request) {
        ProductInventoryDocument product = productInventoryRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        product.setName(request.name().trim());
        product.setImage(normalize(request.image()));
        product.setPrice(request.price());
        product.setAvailableQuantity(request.availableQuantity());
        product.setUpdatedAt(LocalDateTime.now());

        productInventoryRepository.save(product);
    }

    public void deleteProduct(String productId) {
        ProductInventoryDocument product = productInventoryRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        if (orderRecordRepository.existsByItemsProductId(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar el producto porque ya está asociado a una orden"
            );
        }

        productInventoryRepository.delete(product);
    }

    private ProductResponse toResponse(ProductInventoryDocument product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getImage(),
                product.getPrice(),
                product.getAvailableQuantity(),
                product.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}