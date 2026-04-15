package com.broker.repository;

import com.broker.model.ProductRetryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRetryJobRepository extends JpaRepository<ProductRetryJob, UUID> {
}
