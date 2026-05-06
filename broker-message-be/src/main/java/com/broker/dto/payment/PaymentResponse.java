package com.broker.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        String status,
        BigDecimal remainingBalance,
        boolean eventPublished
) {
}