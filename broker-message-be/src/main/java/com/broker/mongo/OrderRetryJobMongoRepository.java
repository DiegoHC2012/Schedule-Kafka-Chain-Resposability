package com.broker.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRetryJobMongoRepository extends MongoRepository<OrderRetryJobDocument, String> {
}
