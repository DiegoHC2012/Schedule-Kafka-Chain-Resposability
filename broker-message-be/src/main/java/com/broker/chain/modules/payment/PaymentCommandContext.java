package com.broker.chain.modules.payment;

import com.broker.dto.payment.PaymentRequest;
import com.broker.model.order.OrderRecord;
import com.broker.model.payment.PaymentRecord;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCommandContext {

    private final PaymentRequest request;
    private OrderRecord order;
    private PaymentRecord paymentRecord;
    private boolean eventPublished;

    public PaymentCommandContext(PaymentRequest request) {
        this.request = request;
    }
}