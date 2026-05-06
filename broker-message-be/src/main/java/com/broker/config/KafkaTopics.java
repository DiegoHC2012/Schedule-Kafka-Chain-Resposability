package com.broker.config;

public final class KafkaTopics {

    public static final String PAYMENTS_RETRY_JOBS = "payments_retry_jobs";
    public static final String ORDER_RETRY_JOBS    = "order_retry_jobs";
    public static final String PRODUCT_RETRY_JOBS  = "product_retry_jobs";
    public static final String INTERNAL_RETRY_JOBS = "internal_retry_jobs";
    public static final String PAYMENT_RECEIVED_EVENTS = "payment_received_events";
    public static final String INVENTORY_UPDATE_EVENTS = "inventory_update_events";
    public static final String ORDER_STATUS_CHANGED_EVENTS = "order_status_changed_events";
    public static final String EMAIL_NOTIFICATION_EVENTS = "email_notification_events";

    private KafkaTopics() {}
}
