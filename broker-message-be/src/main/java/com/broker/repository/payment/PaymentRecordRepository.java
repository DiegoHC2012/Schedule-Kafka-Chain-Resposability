package com.broker.repository.payment;

import com.broker.model.payment.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {
}