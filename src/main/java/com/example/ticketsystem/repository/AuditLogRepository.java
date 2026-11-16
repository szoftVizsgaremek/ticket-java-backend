package com.example.ticketsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticketsystem.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
