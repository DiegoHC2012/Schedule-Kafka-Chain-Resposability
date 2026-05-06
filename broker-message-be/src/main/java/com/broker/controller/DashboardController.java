package com.broker.controller;

import com.broker.config.KafkaTopics;
import com.broker.model.common.ShipmentStatus;
import com.broker.model.order.OrderRecord;
import com.broker.model.payment.PaymentRecord;
import com.broker.model.RetryJob;
import com.broker.model.shipment.ShipmentRecord;
import com.broker.repository.order.OrderRecordRepository;
import com.broker.repository.payment.PaymentRecordRepository;
import com.broker.repository.RetryJobRepository;
import com.broker.repository.shipment.ShipmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final RetryJobRepository retryJobRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ShipmentRecordRepository shipmentRecordRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/retry-jobs")
    public List<RetryJob> getRetryJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return retryJobRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }

    @GetMapping("/retry-jobs/stats")
    public Map<String, Long> getStats() {
        long success = retryJobRepository.countByStatus("SUCCESS");
        return Map.of(
                "total",     retryJobRepository.count(),
                "pending",   retryJobRepository.countByStatus("PENDING"),
            "success",   success,
            "completed", success,
                "failed",    retryJobRepository.countByStatus("FAILED")
        );
    }

        @GetMapping("/dashboard/module-stats")
        public Map<String, Long> getModuleStats() {
        return Map.of(
            "ordersTotal", orderRecordRepository.count(),
            "paymentsTotal", paymentRecordRepository.count(),
            "shipmentsPaid", shipmentRecordRepository.countByStatus(ShipmentStatus.PAGADO),
            "shipmentsSent", shipmentRecordRepository.countByStatus(ShipmentStatus.ENVIADO)
        );
        }

        @GetMapping("/dashboard/orders")
        public List<DashboardOrderView> getOrders(@RequestParam(defaultValue = "8") int size) {
        return orderRecordRepository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt")))
            .getContent()
            .stream()
            .map(this::toDashboardOrderView)
            .toList();
        }

        @GetMapping("/dashboard/payments")
        public List<DashboardPaymentView> getPayments(@RequestParam(defaultValue = "8") int size) {
        return paymentRecordRepository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt")))
            .getContent()
            .stream()
            .map(this::toDashboardPaymentView)
            .toList();
        }

        @GetMapping("/dashboard/shipments")
        public List<DashboardShipmentView> getShipments(@RequestParam(defaultValue = "8") int size) {
        return shipmentRecordRepository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt")))
            .getContent()
            .stream()
            .map(this::toDashboardShipmentView)
            .toList();
        }

    @PostMapping("/kafka/publish")
    public Map<String, Object> publish(@RequestBody Map<String, Object> body) {
        String domain = (String) body.getOrDefault("domain", "payment");

        String topic = switch (domain) {
            case "order"   -> KafkaTopics.ORDER_RETRY_JOBS;
            case "product" -> KafkaTopics.PRODUCT_RETRY_JOBS;
            default        -> KafkaTopics.PAYMENTS_RETRY_JOBS;
        };

        String id          = blankOr(body, "id",          "dash-" + UUID.randomUUID().toString().substring(0, 8));
        String name        = blankOr(body, "name",        domain + "-item");
        String description = blankOr(body, "description", "test event");
        String price       = blankOr(body, "price",       "99.99");
        String quantity    = blankOr(body, "quantity",    "1");
        String category    = blankOr(body, "category",    "demo");
        String brand       = blankOr(body, "brand",       "dashboard");
        String image       = blankOr(body, "image",       "");

        // Escape strings to avoid JSON injection
        String payload = String.format(
            "{\"data\":{\"id\":%s,\"name\":%s,\"description\":%s,\"price\":%s,\"quantity\":%s,\"category\":%s,\"brand\":%s,\"image\":%s}}",
            jsonStr(id), jsonStr(name), jsonStr(description),
            jsonNum(price), jsonNum(quantity),
            jsonStr(category), jsonStr(brand), jsonStr(image)
        );

        kafkaTemplate.send(topic, payload);

        return Map.of("status", "published", "topic", topic, "id", id, "payload", payload);
    }

    private String blankOr(Map<String, Object> body, String key, String fallback) {
        Object val = body.get(key);
        String s = val != null ? val.toString().trim() : "";
        return s.isBlank() ? fallback : s;
    }

    private String jsonStr(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String jsonNum(String value) {
        try { Double.parseDouble(value); return value; } catch (NumberFormatException e) { return "0"; }
    }

        private DashboardOrderView toDashboardOrderView(OrderRecord order) {
        return new DashboardOrderView(
            order.getId(),
            order.getCustomerEmail(),
            order.getStatus().name(),
            order.getTotalAmount(),
            order.getRemainingBalance(),
            order.getCreatedAt()
        );
        }

        private DashboardPaymentView toDashboardPaymentView(PaymentRecord payment) {
        return new DashboardPaymentView(
            payment.getId(),
            payment.getOrderId(),
            payment.getCustomerEmail(),
            payment.getAmount(),
            payment.getRemainingBalance(),
            payment.getCreatedAt()
        );
        }

        private DashboardShipmentView toDashboardShipmentView(ShipmentRecord shipment) {
        return new DashboardShipmentView(
            shipment.getId(),
            shipment.getOrderId(),
            shipment.getPaymentId(),
            shipment.getCustomerEmail(),
            shipment.getStatus().name(),
            shipment.getShippedAt(),
            shipment.getCreatedAt()
        );
        }

        public record DashboardOrderView(
            UUID id,
            String customerEmail,
            String status,
            BigDecimal totalAmount,
            BigDecimal remainingBalance,
            LocalDateTime createdAt
        ) {
        }

        public record DashboardPaymentView(
            UUID id,
            UUID orderId,
            String customerEmail,
            BigDecimal amount,
            BigDecimal remainingBalance,
            LocalDateTime createdAt
        ) {
        }

        public record DashboardShipmentView(
            UUID id,
            UUID orderId,
            UUID paymentId,
            String customerEmail,
            String status,
            LocalDateTime shippedAt,
            LocalDateTime createdAt
        ) {
        }
}
