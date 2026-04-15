package com.broker.scheduler;

import com.broker.service.OrderRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRetryScheduler {

    private final OrderRetryService orderRetryService;

    @Scheduled(fixedDelay = 10000)
    public void run() {
        log.debug("OrderRetryScheduler triggered");
        orderRetryService.processRetries();
    }
}
