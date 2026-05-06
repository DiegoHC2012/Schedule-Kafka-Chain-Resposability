package com.broker.chain.modules.payment;

import com.broker.chain.common.AbstractEndpointHandler;
import com.broker.model.order.OrderRecord;
import com.broker.repository.order.OrderRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PaymentOrderValidationHandler extends AbstractEndpointHandler<PaymentCommandContext> {

    private final OrderRecordRepository orderRecordRepository;

    @Override
    public void handle(PaymentCommandContext context) {
        OrderRecord order = orderRecordRepository.findById(Objects.requireNonNull(context.getRequest().orderId(), "orderId is required"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));

        if (!order.getCustomerEmail().equalsIgnoreCase(context.getRequest().customerEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo del pago no coincide con la orden");
        }

        if (context.getRequest().amount().compareTo(order.getRemainingBalance()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto del pago excede el saldo restante");
        }

        BigDecimal expectedRemaining = order.getRemainingBalance().subtract(context.getRequest().amount());
        if (expectedRemaining.compareTo(context.getRequest().remainingBalance()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El saldo restante no coincide con el monto aplicado");
        }

        context.setOrder(order);
        handleNext(context);
    }
}