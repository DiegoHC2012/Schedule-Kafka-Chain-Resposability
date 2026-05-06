package com.broker.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentReceivedEvent(
        UUID paymentId,
        UUID orderId,
        String customerEmail,
        BigDecimal amount,
        BigDecimal remainingBalance
) {
}