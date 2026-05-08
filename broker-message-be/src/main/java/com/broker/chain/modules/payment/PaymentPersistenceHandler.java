package com.broker.chain.modules.payment;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.mongo.MongoBusinessSyncService;
import com.broker.model.payment.PaymentRecord;
import com.broker.repository.payment.PaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceHandler extends AbstractEndpointHandler<PaymentCommandContext> {

    private final PaymentRecordRepository paymentRecordRepository;
    private final MongoBusinessSyncService mongoBusinessSyncService;

    @Override
    public void handle(PaymentCommandContext context) {
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderId(context.getRequest().orderId());
        paymentRecord.setCustomerEmail(context.getRequest().customerEmail());
        paymentRecord.setAmount(context.getRequest().amount());
        paymentRecord.setRemainingBalance(context.getRequest().remainingBalance());
        PaymentRecord savedPayment = paymentRecordRepository.save(paymentRecord);
        mongoBusinessSyncService.syncPayment(savedPayment);
        context.setPaymentRecord(savedPayment);
        handleNext(context);
    }
}