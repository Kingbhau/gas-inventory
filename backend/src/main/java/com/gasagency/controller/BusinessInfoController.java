package com.gasagency.controller;

import com.gasagency.dto.response.BusinessInfoDto;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gasagency.service.BusinessInfoService;

@RestController
@RequestMapping("/api/business-info")


public class BusinessInfoController {
    private final BusinessInfoService businessInfoService;

    public BusinessInfoController(BusinessInfoService businessInfoService) {
        this.businessInfoService = businessInfoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BusinessInfoDto>> getBusinessInfo() {
        BusinessInfoDto info = businessInfoService.getBusinessInfo();
        return ResponseEntity.ok(ApiResponseUtil.success("Business info retrieved successfully", info));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessInfoDto>> saveBusinessInfo(
            @Valid @RequestBody BusinessInfoDto businessInfoDto) {
        BusinessInfoDto saved = businessInfoService.saveBusinessInfo(businessInfoDto);
        return ResponseEntity.ok(ApiResponseUtil.success("Business info saved successfully", saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessInfoDto>> getById(@PathVariable Long id) {
        BusinessInfoDto businessInfoDto = businessInfoService.getBusinessInfoById(id);
        return ResponseEntity.ok(ApiResponseUtil.success("Business info retrieved successfully", businessInfoDto));
    }
}

