package com.broker.mongo.shipment;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentMongoRepository extends MongoRepository<ShipmentDocument, String> {
}