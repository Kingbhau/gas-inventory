package com.gasagency.repository;

import com.gasagency.entity.ApiIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiIdempotencyRecordRepository extends JpaRepository<ApiIdempotencyRecord, Long> {
    Optional<ApiIdempotencyRecord> findByIdempotencyKeyAndUsernameAndEndpoint(
            String idempotencyKey,
            String username,
            String endpoint
    );
}
