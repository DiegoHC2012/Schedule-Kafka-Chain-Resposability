package com.broker.service.internal;

import com.broker.dto.event.InventoryUpdateEvent;
import com.broker.dto.event.OrderStatusChangedEvent;
import com.broker.dto.event.PaymentReceivedEvent;
import com.broker.model.common.OrderStatus;
import com.broker.model.order.OrderRecord;
import com.broker.repository.order.OrderRecordRepository;
import com.broker.service.inventory.InventoryProjectionService;
import com.broker.service.notifications.EmailNotificationPublisher;
import com.broker.service.shipment.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ModuleEventProcessor {

    private final OrderRecordRepository orderRecordRepository;
    private final EmailNotificationPublisher emailNotificationPublisher;
    private final ShipmentService shipmentService;
    private final InventoryProjectionService inventoryProjectionService;

    @Transactional
    public void processPaymentReceived(PaymentReceivedEvent event) {
        emailNotificationPublisher.publishPaymentReceived(event.customerEmail(), event.orderId(), event.paymentId(), event.amount());

        OrderRecord order = orderRecordRepository.findById(Objects.requireNonNull(event.orderId(), "orderId is required"))
                .orElseThrow(() -> new IllegalStateException("Orden no encontrada para el pago recibido"));

        order.setRemainingBalance(event.remainingBalance());
        if (event.remainingBalance().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(OrderStatus.PAGADO);
            shipmentService.createPaidShipmentIfAbsent(order.getId(), event.paymentId(), order.getCustomerEmail());
        } else {
            order.setStatus(OrderStatus.PENDIENTE_PAGO);
        }

        orderRecordRepository.save(order);
    }

    public void processInventoryUpdate(InventoryUpdateEvent event) {
        inventoryProjectionService.apply(event);
    }

    @Transactional
    public void processOrderStatusChanged(OrderStatusChangedEvent event) {
        emailNotificationPublisher.publishOrderStatusChanged(event.customerEmail(), event.orderId(), event.status());

        if (event.status() == OrderStatus.PAGADO) {
            shipmentService.createPaidShipmentIfAbsent(event.orderId(), null, event.customerEmail());
        } else {
            emailNotificationPublisher.publishPaymentPending(event.customerEmail(), event.orderId(), event.status());
        }
    }
}