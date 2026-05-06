package com.broker.service.notifications;

import com.broker.config.KafkaTopics;
import com.broker.dto.event.EmailNotificationEvent;
import com.broker.model.common.OrderStatus;
import com.broker.model.common.ShipmentStatus;
import com.broker.model.order.OrderItemRecord;
import com.broker.model.shipment.ShipmentRecord;
import com.broker.mongo.inventory.ProductInventoryDocument;
import com.broker.mongo.inventory.ProductInventoryRepository;
import com.broker.repository.order.OrderRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OrderRecordRepository orderRecordRepository;
    private final ProductInventoryRepository productInventoryRepository;

    @Transactional(readOnly = true)
    public void publishPaymentReceived(String recipient, UUID orderId, UUID paymentId, BigDecimal amount) {
        OrderEmailSnapshot snapshot = loadSnapshot(orderId);
        publish(new EmailNotificationEvent(
                "payment-received",
                recipient,
                "Recibimos tu pago" + subjectSuffix(snapshot.itemsSummary()),
                buildPaymentReceivedPayload(orderId, paymentId, amount, snapshot)
        ));
    }

    @Transactional(readOnly = true)
    public void publishShipmentConfirmation(ShipmentRecord shipmentRecord) {
        OrderEmailSnapshot snapshot = loadSnapshot(shipmentRecord.getOrderId());
        publish(new EmailNotificationEvent(
                "shipment-confirmation",
                shipmentRecord.getCustomerEmail(),
                "Tu pedido ya va en camino" + subjectSuffix(snapshot.itemsSummary()),
                buildShipmentPayload(shipmentRecord, snapshot)
        ));
    }

    @Transactional(readOnly = true)
    public void publishOrderStatusChanged(String recipient, UUID orderId, OrderStatus status) {
        OrderEmailSnapshot snapshot = loadSnapshot(orderId);
        publish(new EmailNotificationEvent(
                "order-status-changed",
                recipient,
                subjectForOrderStatus(status, snapshot),
                buildOrderStatusPayload(orderId, status, snapshot)
        ));
    }

    @Transactional(readOnly = true)
    public void publishPaymentPending(String recipient, UUID orderId, OrderStatus status) {
        OrderEmailSnapshot snapshot = loadSnapshot(orderId);
        publish(new EmailNotificationEvent(
                "payment-pending",
                recipient,
                "Estamos validando tu pago" + subjectSuffix(snapshot.itemsSummary()),
                buildPaymentPendingPayload(orderId, status, snapshot)
        ));
    }

    private Map<String, Object> buildPaymentReceivedPayload(UUID orderId, UUID paymentId, BigDecimal amount, OrderEmailSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>(basePayload(orderId, snapshot));
        putIfPresent(payload, "paymentId", paymentId);
        putIfPresent(payload, "amount", formatMoney(amount));
        putIfPresent(payload, "momentLabel", "Pago acreditado");
        return payload;
    }

    private Map<String, Object> buildShipmentPayload(ShipmentRecord shipmentRecord, OrderEmailSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>(basePayload(shipmentRecord.getOrderId(), snapshot));
        putIfPresent(payload, "shipmentId", shipmentRecord.getId());
        putIfPresent(payload, "shipmentStatus", shipmentRecord.getStatus() != null ? shipmentRecord.getStatus().name() : null);
        putIfPresent(payload, "shipmentStatusLabel", formatShipmentStatus(shipmentRecord.getStatus()));
        putIfPresent(payload, "momentLabel", shipmentRecord.getStatus() == ShipmentStatus.ENVIADO ? "En camino" : "Preparando envío");
        return payload;
    }

    private Map<String, Object> buildOrderStatusPayload(UUID orderId, OrderStatus status, OrderEmailSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>(basePayload(orderId, snapshot));
        putIfPresent(payload, "status", status != null ? status.name() : null);
        putIfPresent(payload, "statusLabel", formatOrderStatus(status));
        putIfPresent(payload, "momentLabel", formatOrderStatus(status));
        return payload;
    }

    private Map<String, Object> buildPaymentPendingPayload(UUID orderId, OrderStatus status, OrderEmailSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>(basePayload(orderId, snapshot));
        putIfPresent(payload, "status", status != null ? status.name() : null);
        putIfPresent(payload, "statusLabel", formatOrderStatus(status));
        putIfPresent(payload, "momentLabel", "Pago en revisión");
        return payload;
    }

    private Map<String, Object> basePayload(UUID orderId, OrderEmailSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "orderId", orderId);
        putIfPresent(payload, "itemsSummary", snapshot.itemsSummary());
        putIfPresent(payload, "productName", snapshot.productName());
        putIfPresent(payload, "productImage", snapshot.productImage());
        putIfPresent(payload, "productQuantity", snapshot.productQuantity());
        putIfPresent(payload, "totalItems", snapshot.totalItems());
        putIfPresent(payload, "totalAmount", snapshot.totalAmount());
        putIfPresent(payload, "remainingBalance", snapshot.remainingBalance());
        putIfPresent(payload, "orderStatusLabel", snapshot.orderStatusLabel());
        return payload;
    }

    private OrderEmailSnapshot loadSnapshot(UUID orderId) {
        if (orderId == null) {
            return OrderEmailSnapshot.empty();
        }

        return orderRecordRepository.findById(orderId)
                .map(order -> {
                    List<OrderItemRecord> items = order.getItems();
                    OrderItemRecord leadItem = items.isEmpty() ? null : items.get(0);
                    String productName = leadItem != null ? leadItem.getProductName() : null;
                    int extraLineItems = Math.max(items.size() - 1, 0);
                    int totalItems = items.stream()
                            .map(OrderItemRecord::getQuantity)
                            .filter(quantity -> quantity != null)
                            .mapToInt(Integer::intValue)
                            .sum();

                    return new OrderEmailSnapshot(
                            productName,
                            resolveProductImage(leadItem),
                            leadItem != null ? leadItem.getQuantity() : null,
                            buildItemsSummary(productName, extraLineItems),
                            totalItems > 0 ? totalItems : null,
                            formatMoney(order.getTotalAmount()),
                            formatMoney(order.getRemainingBalance()),
                            formatOrderStatus(order.getStatus())
                    );
                })
                .orElseGet(OrderEmailSnapshot::empty);
    }

    private String resolveProductImage(OrderItemRecord leadItem) {
        if (leadItem == null) {
            return null;
        }

        String productId = leadItem.getProductId();
        if (productId == null || productId.isBlank()) {
            return null;
        }

        return productInventoryRepository.findById(productId)
                .map(ProductInventoryDocument::getImage)
                .filter(image -> image != null && !image.isBlank())
                .orElse(null);
    }

    private String buildItemsSummary(String productName, int extraLineItems) {
        if (productName == null || productName.isBlank()) {
            return null;
        }
        if (extraLineItems <= 0) {
            return productName;
        }
        return productName + " y " + extraLineItems + " producto" + (extraLineItems == 1 ? " más" : "s más");
    }

    private String subjectForOrderStatus(OrderStatus status, OrderEmailSnapshot snapshot) {
        String base = switch (status) {
            case PAGADO -> "Tu compra ya fue confirmada";
            case EN_PROCESO -> "Estamos preparando tu pedido";
            case CANCELADA -> "Actualizamos el estado de tu pedido";
            case PENDIENTE_PAGO -> "Tu pago sigue en revisión";
            case CREADA -> "Recibimos tu pedido";
            case null -> "Actualizamos tu pedido";
        };
        return base + subjectSuffix(snapshot.itemsSummary());
    }

    private String subjectSuffix(String itemsSummary) {
        if (itemsSummary == null || itemsSummary.isBlank()) {
            return "";
        }
        return " de " + itemsSummary;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return "$ " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatOrderStatus(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CREADA -> "Pedido recibido";
            case PAGADO -> "Pago confirmado";
            case PENDIENTE_PAGO -> "Pago pendiente";
            case EN_PROCESO -> "Preparando tu pedido";
            case CANCELADA -> "Pedido cancelado";
        };
    }

    private String formatShipmentStatus(ShipmentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PAGADO -> "Listo para despacho";
            case ENVIADO -> "En camino";
        };
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String stringValue && stringValue.isBlank()) {
            return;
        }
        payload.put(key, value);
    }

    private void publish(EmailNotificationEvent event) {
        try {
            kafkaTemplate.send(KafkaTopics.EMAIL_NOTIFICATION_EVENTS, objectMapper.writeValueAsString(event));
            log.info("Delegated email notification to topic {} with template {}", KafkaTopics.EMAIL_NOTIFICATION_EVENTS, event.template());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo publicar el evento de correo", e);
        }
    }

    private record OrderEmailSnapshot(
            String productName,
            String productImage,
            Integer productQuantity,
            String itemsSummary,
            Integer totalItems,
            String totalAmount,
            String remainingBalance,
            String orderStatusLabel
    ) {
        private static OrderEmailSnapshot empty() {
            return new OrderEmailSnapshot(null, null, null, null, null, null, null, null);
        }
    }
}