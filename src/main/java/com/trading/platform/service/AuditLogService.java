package com.trading.platform.service;

import com.trading.platform.dto.AuditLogResponse;
import com.trading.platform.entity.AuditLog;
import com.trading.platform.entity.User;
import com.trading.platform.repository.AuditLogRepository;
import com.trading.platform.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void log(String operationType, String entityName, Long entityId, Object beforeState, Object afterState) {
        String operatorName = "SYSTEM";
        Long operatorId = null;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            operatorName = auth.getName();
            var userOpt = userRepository.findByUsername(operatorName);
            if (userOpt.isPresent()) {
                operatorId = userOpt.get().getId();
            }
        }

        String beforeJson = serialize(beforeState);
        String afterJson = serialize(afterState);

        AuditLog log = AuditLog.builder()
                .operatorId(operatorId)
                .operatorName(operatorName)
                .operationType(operationType)
                .entityName(entityName)
                .entityId(entityId)
                .beforeValue(beforeJson)
                .afterValue(afterJson)
                .build();

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogs(String entityName, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityNameAndEntityId(entityName, entityId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    private String serialize(Object state) {
        if (state == null) return null;
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            return "Serialization failed: " + e.getMessage();
        }
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .operationType(log.getOperationType())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .beforeValue(log.getBeforeValue())
                .afterValue(log.getAfterValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
