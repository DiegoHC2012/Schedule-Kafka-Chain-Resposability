package com.broker.mongo.payment;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMongoRepository extends MongoRepository<PaymentDocument, String> {
}