package com.broker.mongo.order;

import com.broker.model.order.OrderItemRecord;
import com.broker.model.order.OrderRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "ordenes")
public class OrderDocument {

    @Id
    private String id;

    private String customerEmail;

    private BigDecimal totalAmount;

    private BigDecimal remainingBalance;

    private String status;

    private List<OrderItemDocument> items = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static OrderDocument from(OrderRecord order) {
        OrderDocument doc = new OrderDocument();
        doc.setId(order.getId() != null ? order.getId().toString() : null);
        doc.setCustomerEmail(order.getCustomerEmail());
        doc.setTotalAmount(order.getTotalAmount());
        doc.setRemainingBalance(order.getRemainingBalance());
        doc.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        doc.setCreatedAt(order.getCreatedAt());
        doc.setUpdatedAt(order.getUpdatedAt());

        if (order.getItems() != null) {
            doc.setItems(order.getItems().stream()
                    .map(OrderItemDocument::from)
                    .toList());
        }

        return doc;
    }

    @Data
    @NoArgsConstructor
    public static class OrderItemDocument {

        private String productId;

        private String productName;

        private Integer quantity;

        public static OrderItemDocument from(OrderItemRecord item) {
            OrderItemDocument doc = new OrderItemDocument();
            doc.setProductId(item.getProductId());
            doc.setProductName(item.getProductName());
            doc.setQuantity(item.getQuantity());
            return doc;
        }
    }
}