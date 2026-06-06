package com.trading.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operationType;
    private String entityName;
    private Long entityId;
    private String beforeValue;
    private String afterValue;
    private LocalDateTime createdAt;
}
