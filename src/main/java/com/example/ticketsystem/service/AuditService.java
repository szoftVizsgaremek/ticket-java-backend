package com.example.ticketsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticketsystem.model.AuditAction;
import com.example.ticketsystem.model.AuditLog;
import com.example.ticketsystem.model.User;
import com.example.ticketsystem.repository.AuditLogRepository;

@Service
@Transactional (readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void log(User actor, User target, AuditAction action, String details) {
        AuditLog log = new AuditLog(actor, target, action, details);
        auditLogRepository.save(log);
    }
}