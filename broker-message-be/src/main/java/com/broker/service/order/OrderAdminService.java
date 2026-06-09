package com.broker.service.order;

import com.broker.dto.order.OrderEditRequest;
import com.broker.model.common.OrderStatus;
import com.broker.model.order.OrderRecord;
import com.broker.model.payment.PaymentRecord;
import com.broker.model.shipment.ShipmentRecord;
import com.broker.mongo.MongoBusinessSyncService;
import com.broker.repository.order.OrderRecordRepository;
import com.broker.repository.payment.PaymentRecordRepository;
import com.broker.repository.shipment.ShipmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderAdminService {

    private final OrderRecordRepository orderRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ShipmentRecordRepository shipmentRecordRepository;
    private final MongoBusinessSyncService mongoBusinessSyncService;

    @Transactional
    public void updateOrder(UUID orderId, OrderEditRequest request) {
        OrderRecord order = orderRecordRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));

        validate(request);

        BigDecimal remainingBalance = request.remainingBalance();
        OrderStatus nextStatus = request.status();

        if (nextStatus == OrderStatus.PAGADO) {
            remainingBalance = BigDecimal.ZERO;
        } else if (remainingBalance.compareTo(BigDecimal.ZERO) == 0 && nextStatus != OrderStatus.CANCELADA) {
            nextStatus = OrderStatus.PAGADO;
        }

        order.setCustomerEmail(request.customerEmail().trim());
        order.setTotalAmount(request.totalAmount());
        order.setRemainingBalance(remainingBalance);
        order.setStatus(nextStatus);

        OrderRecord savedOrder = orderRecordRepository.save(order);
        mongoBusinessSyncService.syncOrder(savedOrder);
    }

    @Transactional
    public void deleteOrder(UUID orderId) {
        OrderRecord order = orderRecordRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));

        shipmentRecordRepository.findByOrderId(orderId).ifPresent((ShipmentRecord shipment) -> {
            shipmentRecordRepository.delete(shipment);
            mongoBusinessSyncService.deleteShipment(shipment.getId());
        });

        List<PaymentRecord> payments = paymentRecordRepository.findAllByOrderId(orderId);
        if (!payments.isEmpty()) {
            paymentRecordRepository.deleteAll(payments);
            mongoBusinessSyncService.deletePayments(
                    payments.stream()
                            .map(PaymentRecord::getId)
                            .toList()
            );
        }

        orderRecordRepository.delete(order);
        mongoBusinessSyncService.deleteOrder(orderId);
    }

    private void validate(OrderEditRequest request) {
        if (request.remainingBalance().compareTo(request.totalAmount()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El saldo restante no puede exceder el total de la orden"
            );
        }

        if (request.status() == OrderStatus.PAGADO && request.remainingBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Una orden PAGADO no puede tener saldo restante"
            );
        }
    }
}