package com.broker.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRetryJobMongoRepository extends MongoRepository<ProductRetryJobDocument, String> {
}
