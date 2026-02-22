package com.gasagency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gasagency.entity.AuditRecord;
import com.gasagency.repository.AuditRecordRepository;
import com.gasagency.util.AuditLogger;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

@Service
public class AuditRecordService {
    private final AuditRecordRepository repository;
    private final ObjectMapper objectMapper;

    public AuditRecordService(AuditRecordRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    public void recordChange(String entityType,
                             Long entityId,
                             String action,
                             String fieldName,
                             Object oldValue,
                             Object newValue,
                             String note,
                             String source) {
        recordChange(entityType, entityId, action, fieldName, oldValue, newValue, note, source, null);
    }

    public void recordChange(String entityType,
                             Long entityId,
                             String action,
                             String fieldName,
                             Object oldValue,
                             Object newValue,
                             String note,
                             String source,
                             Map<String, Object> metadata) {
        AuditRecord record = new AuditRecord();
        record.setEntityType(entityType);
        record.setEntityId(entityId);
        record.setAction(action);
        record.setFieldName(fieldName);
        record.setOldValue(stringify(oldValue));
        record.setNewValue(stringify(newValue));
        record.setNote(note);
        record.setMetadata(serializeMetadata(metadata));
        record.setRequestId(AuditLogger.getRequestId());
        record.setSource(source);
        repository.save(record);
    }

    public Page<AuditRecord> getAuditRecords(String entityType, Long entityId, String fieldName, Pageable pageable) {
        if (fieldName != null && !fieldName.trim().isEmpty()) {
            return repository.findByEntityTypeAndEntityIdAndFieldName(entityType, entityId, fieldName, pageable);
        }
        return repository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"failed_to_serialize_metadata\"}";
        }
    }
}
