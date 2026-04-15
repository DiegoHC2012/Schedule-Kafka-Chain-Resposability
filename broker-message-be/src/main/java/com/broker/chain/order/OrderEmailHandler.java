package com.broker.chain.order;

import com.broker.chain.RetryContext;
import com.broker.chain.RetryHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEmailHandler implements RetryHandler {

    private RetryHandler next;

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String from;

    @Value("${app.email.success-recipient}")
    private String successRecipient;

    public OrderEmailHandler(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        log.debug("PASO B - OrderEmailHandler: sending success email for retryJobId={}", context.getRetryJob().getId());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(successRecipient);
            message.setSubject("Orden creada correctamente");
            message.setText("La orden con ID " + context.getRetryJob().getId() + " fue procesada exitosamente.\n\nPayload: " + context.getRetryJob().getPayload());
            mailSender.send(message);
            context.getRetryJob().setStepBStatus("SUCCESS");
            log.info("PASO B - Success email sent for retryJobId={}", context.getRetryJob().getId());
            if (next != null) {
                next.handle(context);
            }
        } catch (Exception e) {
            log.error("PASO B - Failed to send order success email: {}", e.getMessage());
            context.getRetryJob().setStepBStatus("FAILED");
            context.fail("PASO B failed: " + e.getMessage());
        }
    }
}
