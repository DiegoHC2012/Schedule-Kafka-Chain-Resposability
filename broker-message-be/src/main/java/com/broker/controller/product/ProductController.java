package com.broker.controller.product;

import com.broker.dto.product.ProductCreateRequest;
import com.broker.dto.product.ProductResponse;
import com.broker.dto.product.ProductUpdateRequest;
import com.broker.retry.InternalRetryOperation;
import com.broker.service.product.ProductCatalogService;
import com.broker.service.internal.InternalRetryJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCatalogService productCatalogService;
    private final InternalRetryJobService internalRetryJobService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts() {
        return ResponseEntity.ok(productCatalogService.listProducts());
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(productCatalogService.createProduct(request));
        } catch (RuntimeException e) {
            internalRetryJobService.recordEndpointFailure(InternalRetryOperation.PRODUCT_CREATE, "/productos", request, e);
            throw e;
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        productCatalogService.updateProduct(productId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId) {
        productCatalogService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}