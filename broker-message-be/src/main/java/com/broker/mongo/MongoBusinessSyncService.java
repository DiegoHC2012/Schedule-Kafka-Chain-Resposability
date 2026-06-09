package com.broker.mongo;

import com.broker.mongo.order.OrderDocument;
import com.broker.mongo.order.OrderMongoRepository;
import com.broker.mongo.payment.PaymentDocument;
import com.broker.mongo.payment.PaymentMongoRepository;
import com.broker.mongo.shipment.ShipmentDocument;
import com.broker.mongo.shipment.ShipmentMongoRepository;
import com.broker.model.order.OrderRecord;
import com.broker.model.payment.PaymentRecord;
import com.broker.model.shipment.ShipmentRecord;
import com.broker.repository.order.OrderRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MongoBusinessSyncService {

    private final OrderRecordRepository orderRecordRepository;
    private final OrderMongoRepository orderMongoRepository;
    private final PaymentMongoRepository paymentMongoRepository;
    private final ShipmentMongoRepository shipmentMongoRepository;

    public void syncOrder(OrderRecord order) {
        try {
            OrderRecord source = order;
            if (order.getId() != null) {
                source = orderRecordRepository.findWithItemsById(order.getId()).orElse(order);
            }
            orderMongoRepository.save(OrderDocument.from(source));
            log.debug("MongoDB synced ordenes id={} status={}", source.getId(), source.getStatus());
        } catch (Exception e) {
            log.error("MongoDB sync failed for ordenes id={}: {}", order.getId(), e.getMessage(), e);
            throw new IllegalStateException("No se pudo sincronizar la orden en MongoDB", e);
        }
    }

    public void syncPayment(PaymentRecord payment) {
        try {
            paymentMongoRepository.save(PaymentDocument.from(payment));
            log.debug("MongoDB synced pagos id={} orderId={}", payment.getId(), payment.getOrderId());
        } catch (Exception e) {
            log.error("MongoDB sync failed for pagos id={}: {}", payment.getId(), e.getMessage(), e);
            throw new IllegalStateException("No se pudo sincronizar el pago en MongoDB", e);
        }
    }

    public void syncShipment(ShipmentRecord shipment) {
        try {
            shipmentMongoRepository.save(ShipmentDocument.from(shipment));
            log.debug("MongoDB synced envios id={} orderId={} status={}", shipment.getId(), shipment.getOrderId(), shipment.getStatus());
        } catch (Exception e) {
            log.error("MongoDB sync failed for envios id={}: {}", shipment.getId(), e.getMessage(), e);
            throw new IllegalStateException("No se pudo sincronizar el envio en MongoDB", e);
        }
    }

    public void deleteOrder(UUID orderId) {
        try {
            orderMongoRepository.deleteById(orderId.toString());
            log.debug("MongoDB deleted ordenes id={}", orderId);
        } catch (Exception e) {
            log.error("MongoDB delete failed for ordenes id={}: {}", orderId, e.getMessage(), e);
            throw new IllegalStateException("No se pudo eliminar la orden en MongoDB", e);
        }
    }

    public void deletePayments(List<UUID> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            return;
        }

        try {
            paymentIds.stream()
                    .map(UUID::toString)
                    .forEach(paymentMongoRepository::deleteById);
            log.debug("MongoDB deleted pagos ids={}", paymentIds);
        } catch (Exception e) {
            log.error("MongoDB delete failed for pagos ids={}: {}", paymentIds, e.getMessage(), e);
            throw new IllegalStateException("No se pudieron eliminar los pagos en MongoDB", e);
        }
    }

    public void deleteShipment(UUID shipmentId) {
        try {
            shipmentMongoRepository.deleteById(shipmentId.toString());
            log.debug("MongoDB deleted envios id={}", shipmentId);
        } catch (Exception e) {
            log.error("MongoDB delete failed for envios id={}: {}", shipmentId, e.getMessage(), e);
            throw new IllegalStateException("No se pudo eliminar el envio en MongoDB", e);
        }
    }
}