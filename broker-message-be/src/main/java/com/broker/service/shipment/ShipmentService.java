package com.broker.service.shipment;

import com.broker.model.common.ShipmentStatus;
import com.broker.model.shipment.ShipmentRecord;
import com.broker.repository.shipment.ShipmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRecordRepository shipmentRecordRepository;

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

        return shipmentRecordRepository.save(shipmentRecord);
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
        return shipmentRecordRepository.save(shipmentRecord);
    }
}