package com.broker.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreateRequest(
        @NotBlank @Email String customerEmail,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
        @NotEmpty List<@Valid OrderItemRequest> products
) {
}