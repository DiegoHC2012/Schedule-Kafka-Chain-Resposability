package com.broker.repository;

import com.broker.model.RetryJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RetryJobRepository extends JpaRepository<RetryJob, UUID> {

    long countByStatus(String status);

    @Query("""
        SELECT r
        FROM RetryJob r
        WHERE r.status = :status
          AND r.topic = :topic
          AND r.nextRetryAt <= :now
          AND r.attemptCount < r.maxAttempts
        ORDER BY r.createdAt ASC
    """)
    List<RetryJob> findPendingDueByTopic(
            @Param("status") String status,
            @Param("topic") String topic,
            @Param("now") LocalDateTime now,
            Pageable pageable);
}
