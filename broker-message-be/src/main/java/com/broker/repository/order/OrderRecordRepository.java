package com.broker.repository.order;

import com.broker.model.order.OrderRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRecordRepository extends JpaRepository<OrderRecord, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderRecord> findWithItemsById(UUID id);
}