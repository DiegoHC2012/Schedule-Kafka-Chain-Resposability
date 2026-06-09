package com.broker.controller.shipment;

import com.broker.dto.shipment.ShipmentUpdateRequest;
import com.broker.service.shipment.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/envios")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PutMapping("/{shipmentId}")
    public ResponseEntity<Void> updateShipment(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody ShipmentUpdateRequest request
    ) {
        shipmentService.updateShipment(shipmentId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{shipmentId}")
    public ResponseEntity<Void> deleteShipment(@PathVariable UUID shipmentId) {
        shipmentService.deleteShipment(shipmentId);
        return ResponseEntity.noContent().build();
    }
}