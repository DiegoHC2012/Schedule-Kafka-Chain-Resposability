package com.broker.scheduler;

import com.broker.service.internal.InternalRetryJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalRetryJobScheduler {

    private final InternalRetryJobService internalRetryJobService;

    @Scheduled(fixedDelay = 10000)
    public void run() {
        log.debug("InternalRetryJobScheduler triggered");
        internalRetryJobService.processRetries();
    }
}