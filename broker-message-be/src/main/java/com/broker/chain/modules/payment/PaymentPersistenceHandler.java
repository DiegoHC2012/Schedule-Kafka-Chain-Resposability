package com.broker.chain.modules.payment;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.model.payment.PaymentRecord;
import com.broker.repository.payment.PaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceHandler extends AbstractEndpointHandler<PaymentCommandContext> {

    private final PaymentRecordRepository paymentRecordRepository;

    @Override
    public void handle(PaymentCommandContext context) {
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderId(context.getRequest().orderId());
        paymentRecord.setCustomerEmail(context.getRequest().customerEmail());
        paymentRecord.setAmount(context.getRequest().amount());
        paymentRecord.setRemainingBalance(context.getRequest().remainingBalance());
        context.setPaymentRecord(paymentRecordRepository.save(paymentRecord));
        handleNext(context);
    }
}