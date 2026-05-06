package com.broker.scheduler.shipment;

import com.broker.model.shipment.ShipmentRecord;
import com.broker.service.notifications.EmailNotificationPublisher;
import com.broker.service.shipment.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentNotificationScheduler {

    private final ShipmentService shipmentService;
    private final EmailNotificationPublisher emailNotificationPublisher;

    @Scheduled(fixedRate = 10000)
    public void processPaidShipments() {
        List<ShipmentRecord> shipments = shipmentService.findPaidPendingShipments();
        if (shipments.isEmpty()) {
            return;
        }

        log.info("ShipmentNotificationScheduler: processing {} paid shipments", shipments.size());
        for (ShipmentRecord shipment : shipments) {
            emailNotificationPublisher.publishShipmentConfirmation(shipment);
            shipmentService.markAsShipped(shipment);
        }
    }
}