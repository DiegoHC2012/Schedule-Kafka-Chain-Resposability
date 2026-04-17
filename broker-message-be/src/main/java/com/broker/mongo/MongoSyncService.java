package com.broker.mongo;

import com.broker.model.OrderRetryJob;
import com.broker.model.PaymentRetryJob;
import com.broker.model.ProductRetryJob;
import com.broker.model.RetryJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MongoSyncService {

    private final RetryJobMongoRepository retryJobMongoRepository;
    private final PaymentRetryJobMongoRepository paymentRetryJobMongoRepository;
    private final OrderRetryJobMongoRepository orderRetryJobMongoRepository;
    private final ProductRetryJobMongoRepository productRetryJobMongoRepository;

    public void sync(RetryJob job) {
        try {
            retryJobMongoRepository.save(RetryJobDocument.from(job));
            log.debug("MongoDB synced retryJobId={} status={}", job.getId(), job.getStatus());
        } catch (Exception e) {
            log.warn("MongoDB sync failed for retryJobId={}: {}", job.getId(), e.getMessage());
        }
    }

    public void syncPaymentFailed(PaymentRetryJob job) {
        try {
            paymentRetryJobMongoRepository.save(PaymentRetryJobDocument.from(job));
            log.debug("MongoDB synced payment_retry_jobs FAILED id={}", job.getId());
        } catch (Exception e) {
            log.warn("MongoDB sync failed for payment_retry_jobs id={}: {}", job.getId(), e.getMessage());
        }
    }

    public void syncPaymentSuccess(RetryJob job) {
        try {
            paymentRetryJobMongoRepository.save(PaymentRetryJobDocument.fromSuccess(job));
            log.debug("MongoDB synced payment_retry_jobs SUCCESS retryJobId={}", job.getId());
        } catch (Exception e) {
            log.warn("MongoDB sync failed for payment_retry_jobs SUCCESS retryJobId={}: {}", job.getId(), e.getMessage());
        }
    }

    public void syncOrderFailed(OrderRetryJob job) {
        try {
            orderRetryJobMongoRepository.save(OrderRetryJobDocument.from(job));
            log.debug("MongoDB synced order_retry_jobs FAILED id={}", job.getId());
        } catch (Exception e) {
            log.warn("MongoDB sync failed for order_retry_jobs id={}: {}", job.getId(), e.getMessage());
        }
    }

    public void syncOrderSuccess(RetryJob job) {
        try {
            orderRetryJobMongoRepository.save(OrderRetryJobDocument.fromSuccess(job));
            log.debug("MongoDB synced order_retry_jobs SUCCESS retryJobId={}", job.getId());
        } catch (Exception e) {
            log.warn("MongoDB sync failed for order_retry_jobs SUCCESS retryJobId={}: {}", job.getId(), e.getMessage());
        }
    }

    public void syncProductFailed(ProductRetryJob job) {
        try {
            productRetryJobMongoRepository.save(ProductRetryJobDocument.from(job));
            log.debug("MongoDB synced product_retry_jobs FAILED id={}", job.getId());
        } catch (Exception e) {
            log.warn("MongoDB sync failed for product_retry_jobs id={}: {}", job.getId(), e.getMessage());
        }
    }

    public void syncProductSuccess(RetryJob job) {
        try {
            productRetryJobMongoRepository.save(ProductRetryJobDocument.fromSuccess(job));
            log.debug("MongoDB synced product_retry_jobs SUCCESS retryJobId={}", job.getId());
        } catch (Exception e) {
            log.warn("MongoDB sync failed for product_retry_jobs SUCCESS retryJobId={}: {}", job.getId(), e.getMessage());
        }
    }
}
