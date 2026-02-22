package com.gasagency.controller;

import com.gasagency.entity.AuditRecord;
import com.gasagency.service.AuditRecordService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import com.gasagency.dto.response.PagedResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-records")
public class AuditRecordController {
    private final AuditRecordService auditRecordService;

    public AuditRecordController(AuditRecordService auditRecordService) {
        this.auditRecordService = auditRecordService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<AuditRecord>>> getAuditRecords(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam(required = false) String fieldName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate", "id"));
        Page<AuditRecord> result = auditRecordService.getAuditRecords(entityType, entityId, fieldName, pageable);
        return ResponseEntity.ok(ApiResponseUtil.success("Audit records retrieved successfully", result));
    }
}
