package com.clydrive.service.impl;

import com.clydrive.enums.AuditAction;
import com.clydrive.module.AuditLog;
import com.clydrive.repository.AuditLogRepository;
import com.clydrive.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void record(AuditAction action, String identifier, String details) {
        record(action, identifier, details, null);
    }

    @Override
    public void record(AuditAction action, String identifier, String details, HttpServletRequest request) {
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .action(action)
                .identifier(identifier)
                .details(details)
                .timestamp(LocalDateTime.now());

        if (request != null) {
            builder.ipAddress(request.getRemoteAddr())
                    .deviceInfo(request.getHeader("User-Agent"));
        }

        auditLogRepository.save(builder.build());
    }
}
