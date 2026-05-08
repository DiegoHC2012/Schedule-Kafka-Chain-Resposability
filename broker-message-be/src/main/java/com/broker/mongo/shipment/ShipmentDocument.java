package com.broker.mongo.shipment;

import com.broker.model.shipment.ShipmentRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "envios")
public class ShipmentDocument {

    @Id
    private String id;

    private String orderId;

    private String paymentId;

    private String customerEmail;

    private String status;

    private boolean notificationSent;

    private LocalDateTime shippedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static ShipmentDocument from(ShipmentRecord shipment) {
        ShipmentDocument doc = new ShipmentDocument();
        doc.setId(shipment.getId() != null ? shipment.getId().toString() : null);
        doc.setOrderId(shipment.getOrderId() != null ? shipment.getOrderId().toString() : null);
        doc.setPaymentId(shipment.getPaymentId() != null ? shipment.getPaymentId().toString() : null);
        doc.setCustomerEmail(shipment.getCustomerEmail());
        doc.setStatus(shipment.getStatus() != null ? shipment.getStatus().name() : null);
        doc.setNotificationSent(shipment.isNotificationSent());
        doc.setShippedAt(shipment.getShippedAt());
        doc.setCreatedAt(shipment.getCreatedAt());
        doc.setUpdatedAt(shipment.getUpdatedAt());
        return doc;
    }
}