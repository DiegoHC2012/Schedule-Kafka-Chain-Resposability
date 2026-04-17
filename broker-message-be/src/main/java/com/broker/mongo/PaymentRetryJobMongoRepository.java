package com.broker.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRetryJobMongoRepository extends MongoRepository<PaymentRetryJobDocument, String> {
}
