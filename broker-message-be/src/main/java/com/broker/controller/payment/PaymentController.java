package com.broker.controller.payment;

import com.broker.dto.payment.PaymentRequest;
import com.broker.dto.payment.PaymentResponse;
import com.broker.retry.InternalRetryOperation;
import com.broker.service.internal.InternalRetryJobService;
import com.broker.service.payment.PaymentCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentCommandService paymentCommandService;
    private final InternalRetryJobService internalRetryJobService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(paymentCommandService.createPayment(request));
        } catch (RuntimeException e) {
            internalRetryJobService.recordEndpointFailure(InternalRetryOperation.PAYMENT_CREATE, "/pagos", request, e);
            throw e;
        }
    }
}