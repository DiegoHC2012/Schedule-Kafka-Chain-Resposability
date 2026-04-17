package com.broker.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetryJobMongoRepository extends MongoRepository<RetryJobDocument, String> {
}
