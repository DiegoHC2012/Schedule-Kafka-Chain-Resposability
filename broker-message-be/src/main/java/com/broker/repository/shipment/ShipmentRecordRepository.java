package com.broker.repository.shipment;

import com.broker.model.common.ShipmentStatus;
import com.broker.model.shipment.ShipmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRecordRepository extends JpaRepository<ShipmentRecord, UUID> {

    long countByStatus(ShipmentStatus status);

    Optional<ShipmentRecord> findByOrderId(UUID orderId);

    List<ShipmentRecord> findTop100ByStatusAndNotificationSentFalseOrderByCreatedAtAsc(ShipmentStatus status);
}