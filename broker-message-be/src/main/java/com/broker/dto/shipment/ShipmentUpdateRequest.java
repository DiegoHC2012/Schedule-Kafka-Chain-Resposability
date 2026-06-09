package com.broker.dto.shipment;

import com.broker.model.common.ShipmentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ShipmentUpdateRequest(
        @NotBlank @Email String customerEmail,
        UUID paymentId,
        @NotNull ShipmentStatus status
) {
}