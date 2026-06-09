package com.broker.dto.order;

import com.broker.model.common.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderEditRequest(
        @NotBlank @Email String customerEmail,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
        @NotNull @DecimalMin("0.00") BigDecimal remainingBalance,
        @NotNull OrderStatus status
) {
}