package com.gasagency.controller;

import com.gasagency.dto.BusinessInfoDto;
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
    public ResponseEntity<BusinessInfoDto> getBusinessInfo() {
        BusinessInfoDto info = businessInfoService.getBusinessInfo();
        return ResponseEntity.ok(info);
    }

    @PostMapping
    public ResponseEntity<BusinessInfoDto> saveBusinessInfo(@Valid @RequestBody BusinessInfoDto businessInfoDto) {
        BusinessInfoDto saved = businessInfoService.saveBusinessInfo(businessInfoDto);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public  ResponseEntity<BusinessInfoDto> getById(@PathVariable Long id){
        BusinessInfoDto businessInfoDto = businessInfoService.getBusinessInfoById(id);
        return ResponseEntity.ok(businessInfoDto);
    }
}
