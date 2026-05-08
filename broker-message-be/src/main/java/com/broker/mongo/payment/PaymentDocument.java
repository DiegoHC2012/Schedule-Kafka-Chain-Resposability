package com.broker.mongo.payment;

import com.broker.model.payment.PaymentRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "pagos")
public class PaymentDocument {

    @Id
    private String id;

    private String orderId;

    private String customerEmail;

    private BigDecimal amount;

    private BigDecimal remainingBalance;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PaymentDocument from(PaymentRecord payment) {
        PaymentDocument doc = new PaymentDocument();
        doc.setId(payment.getId() != null ? payment.getId().toString() : null);
        doc.setOrderId(payment.getOrderId() != null ? payment.getOrderId().toString() : null);
        doc.setCustomerEmail(payment.getCustomerEmail());
        doc.setAmount(payment.getAmount());
        doc.setRemainingBalance(payment.getRemainingBalance());
        doc.setCreatedAt(payment.getCreatedAt());
        doc.setUpdatedAt(payment.getUpdatedAt());
        return doc;
    }
}