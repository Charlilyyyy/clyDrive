package com.clydrive.service;

import com.clydrive.enums.AuditAction;
import jakarta.servlet.http.HttpServletRequest;

public interface AuditService {

    void record(AuditAction action, String identifier, String details);

    void record(AuditAction action, String identifier, String details, HttpServletRequest request);
}
