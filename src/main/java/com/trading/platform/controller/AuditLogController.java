package com.trading.platform.controller;

import com.trading.platform.dto.AuditLogResponse;
import com.trading.platform.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logging", description = "Endpoints for retrieving system audit trails. Restricted to ADMIN.")
@SecurityRequirement(name = "Bearer Authentication")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Get all audit logs (ADMIN)", description = "Retrieves all audit logs chronologically. Restricted to ADMIN users.")
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<AuditLogResponse> response = auditLogService.getAllLogs(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search logs by entity (ADMIN)", description = "Filters audit logs by entity name and entity ID. Restricted to ADMIN users.")
    public ResponseEntity<Page<AuditLogResponse>> searchLogs(
            @RequestParam String entityName,
            @RequestParam Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<AuditLogResponse> response = auditLogService.getLogs(entityName, entityId, pageable);
        return ResponseEntity.ok(response);
    }
}
