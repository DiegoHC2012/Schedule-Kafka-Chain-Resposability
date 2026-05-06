package com.broker.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        @NotNull UUID orderId,
        @NotBlank @Email String customerEmail,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull @DecimalMin("0.00") BigDecimal remainingBalance
) {
}