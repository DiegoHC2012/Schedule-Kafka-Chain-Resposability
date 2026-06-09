package com.broker.service.shipment;

import com.broker.dto.shipment.ShipmentUpdateRequest;
import com.broker.mongo.MongoBusinessSyncService;
import com.broker.model.common.ShipmentStatus;
import com.broker.model.shipment.ShipmentRecord;
import com.broker.repository.payment.PaymentRecordRepository;
import com.broker.repository.shipment.ShipmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRecordRepository shipmentRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final MongoBusinessSyncService mongoBusinessSyncService;

    @Transactional
    public ShipmentRecord createPaidShipmentIfAbsent(UUID orderId, UUID paymentId, String customerEmail) {
        ShipmentRecord shipmentRecord = shipmentRecordRepository.findByOrderId(orderId)
                .orElseGet(ShipmentRecord::new);

        shipmentRecord.setOrderId(orderId);
        shipmentRecord.setCustomerEmail(customerEmail);

        if (shipmentRecord.getStatus() == null) {
            shipmentRecord.setStatus(ShipmentStatus.PAGADO);
        }

        if (shipmentRecord.getStatus() != ShipmentStatus.ENVIADO) {
            shipmentRecord.setStatus(ShipmentStatus.PAGADO);
        }

        if (paymentId != null) {
            shipmentRecord.setPaymentId(paymentId);
        }

        ShipmentRecord savedShipment = shipmentRecordRepository.save(shipmentRecord);
        mongoBusinessSyncService.syncShipment(savedShipment);
        return savedShipment;
    }

    @Transactional(readOnly = true)
    public List<ShipmentRecord> findPaidPendingShipments() {
        return shipmentRecordRepository.findTop100ByStatusAndNotificationSentFalseOrderByCreatedAtAsc(ShipmentStatus.PAGADO);
    }

    @Transactional
    public ShipmentRecord markAsShipped(ShipmentRecord shipmentRecord) {
        shipmentRecord.setStatus(ShipmentStatus.ENVIADO);
        shipmentRecord.setNotificationSent(true);
        shipmentRecord.setShippedAt(LocalDateTime.now());
        ShipmentRecord savedShipment = shipmentRecordRepository.save(shipmentRecord);
        mongoBusinessSyncService.syncShipment(savedShipment);
        return savedShipment;
    }

    @Transactional
    public void updateShipment(UUID shipmentId, ShipmentUpdateRequest request) {
        ShipmentRecord shipmentRecord = shipmentRecordRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Envio no encontrado"));

        if (request.paymentId() != null && !paymentRecordRepository.existsById(request.paymentId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado");
        }

        shipmentRecord.setCustomerEmail(request.customerEmail().trim());
        shipmentRecord.setPaymentId(request.paymentId());
        shipmentRecord.setStatus(request.status());

        if (request.status() == ShipmentStatus.ENVIADO) {
            shipmentRecord.setNotificationSent(true);
            if (shipmentRecord.getShippedAt() == null) {
                shipmentRecord.setShippedAt(LocalDateTime.now());
            }
        } else {
            shipmentRecord.setNotificationSent(false);
            shipmentRecord.setShippedAt(null);
        }

        ShipmentRecord savedShipment = shipmentRecordRepository.save(shipmentRecord);
        mongoBusinessSyncService.syncShipment(savedShipment);
    }

    @Transactional
    public void deleteShipment(UUID shipmentId) {
        ShipmentRecord shipmentRecord = shipmentRecordRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Envio no encontrado"));

        shipmentRecordRepository.delete(shipmentRecord);
        mongoBusinessSyncService.deleteShipment(shipmentId);
    }
}