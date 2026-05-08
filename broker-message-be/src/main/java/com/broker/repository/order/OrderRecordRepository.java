package com.broker.repository.order;

import com.broker.model.common.OrderStatus;
import com.broker.model.order.OrderRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface OrderRecordRepository extends JpaRepository<OrderRecord, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderRecord> findWithItemsById(UUID id);

    Page<OrderRecord> findByStatusNotIn(Collection<OrderStatus> statuses, Pageable pageable);

    Page<OrderRecord> findByStatusInAndRemainingBalanceGreaterThan(Collection<OrderStatus> statuses, BigDecimal remainingBalance, Pageable pageable);
}