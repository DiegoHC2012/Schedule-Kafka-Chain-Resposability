package com.broker.service.payment;

import com.broker.chain.modules.payment.PaymentCommandContext;
import com.broker.chain.modules.payment.PaymentEventPublishHandler;
import com.broker.chain.modules.payment.PaymentOrderValidationHandler;
import com.broker.chain.modules.payment.PaymentPersistenceHandler;
import com.broker.dto.payment.PaymentRequest;
import com.broker.dto.payment.PaymentResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final PaymentOrderValidationHandler paymentOrderValidationHandler;
    private final PaymentPersistenceHandler paymentPersistenceHandler;
    private final PaymentEventPublishHandler paymentEventPublishHandler;

    @PostConstruct
    public void initChain() {
        paymentOrderValidationHandler.setNext(paymentPersistenceHandler);
        paymentPersistenceHandler.setNext(paymentEventPublishHandler);
    }

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        PaymentCommandContext context = new PaymentCommandContext(request);
        paymentOrderValidationHandler.handle(context);

        return new PaymentResponse(
                context.getPaymentRecord().getId(),
                context.getPaymentRecord().getOrderId(),
                "RECIBIDO",
                context.getPaymentRecord().getRemainingBalance(),
                context.isEventPublished()
        );
    }
}