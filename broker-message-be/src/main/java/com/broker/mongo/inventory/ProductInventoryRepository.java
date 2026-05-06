package com.broker.mongo.inventory;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductInventoryRepository extends MongoRepository<ProductInventoryDocument, String> {
}