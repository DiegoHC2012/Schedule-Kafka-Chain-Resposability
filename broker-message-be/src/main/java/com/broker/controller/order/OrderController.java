package com.broker.controller.order;

import com.broker.dto.order.OrderCreateRequest;
import com.broker.dto.order.OrderProductsUpdateRequest;
import com.broker.dto.order.OrderResponse;
import com.broker.dto.order.OrderStatusUpdateRequest;
import com.broker.dto.order.OrderStatusUpdateResponse;
import com.broker.retry.InternalRetryOperation;
import com.broker.service.internal.InternalRetryJobService;
import com.broker.service.order.OrderCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ordenes")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService orderCommandService;
    private final InternalRetryJobService internalRetryJobService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderCommandService.createOrder(request));
        } catch (RuntimeException e) {
            internalRetryJobService.recordEndpointFailure(InternalRetryOperation.ORDER_CREATE, "/ordenes", request, e);
            throw e;
        }
    }

    @PutMapping
    public ResponseEntity<OrderResponse> updateOrderProducts(@Valid @RequestBody OrderProductsUpdateRequest request) {
        try {
            return ResponseEntity.ok(orderCommandService.updateOrderProducts(request));
        } catch (RuntimeException e) {
            internalRetryJobService.recordEndpointFailure(InternalRetryOperation.ORDER_UPDATE_PRODUCTS, "/ordenes", request, e);
            throw e;
        }
    }

    @PutMapping("/estatus")
    public ResponseEntity<OrderStatusUpdateResponse> updateOrderStatus(@Valid @RequestBody OrderStatusUpdateRequest request) {
        try {
            return ResponseEntity.ok(orderCommandService.updateStatus(request));
        } catch (RuntimeException e) {
            internalRetryJobService.recordEndpointFailure(InternalRetryOperation.ORDER_UPDATE_STATUS, "/ordenes/estatus", request, e);
            throw e;
        }
    }
}