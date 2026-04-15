package com.broker.repository;

import com.broker.model.RetryJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RetryJobRepository extends JpaRepository<RetryJob, UUID> {

    List<RetryJob> findByStatusAndTopic(String status, String topic, Pageable pageable);
}
