package com.broker.scheduler;

import com.broker.service.ProductRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRetryScheduler {

    private final ProductRetryService productRetryService;

    @Scheduled(fixedDelay = 10000)
    public void run() {
        log.debug("ProductRetryScheduler triggered");
        productRetryService.processRetries();
    }
}
