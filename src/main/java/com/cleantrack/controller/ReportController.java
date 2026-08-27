package com.cleantrack.controller;

import com.cleantrack.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ReportController {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public ReportController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
}
