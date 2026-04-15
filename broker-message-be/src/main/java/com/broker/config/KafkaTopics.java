package com.broker.config;

public final class KafkaTopics {

    public static final String PAYMENTS_RETRY_JOBS = "payments_retry_jobs";
    public static final String ORDER_RETRY_JOBS    = "order_retry_jobs";
    public static final String PRODUCT_RETRY_JOBS  = "product_retry_jobs";

    private KafkaTopics() {}
}
