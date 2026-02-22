package com.gasagency.repository;

import com.gasagency.entity.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {
    Page<AuditRecord> findByEntityTypeAndEntityIdAndFieldName(
            String entityType, Long entityId, String fieldName, Pageable pageable);

    Page<AuditRecord> findByEntityTypeAndEntityId(
            String entityType, Long entityId, Pageable pageable);
}
