package com.example.ticketsystem.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Immutable 
@Entity 
public class AuditLog {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "actor_id", nullable = false) 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User actor;

    @JoinColumn(name = "target_user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User targetUser;

    @Enumerated(EnumType.STRING) 
    @Column(length = 40, nullable = false)
    private AuditAction action;

    @Column(length = 255)
    private String details;

    @Column(updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    protected AuditLog() {}

    public AuditLog(User actor, User targetUser, AuditAction action, String details) {
        this.actor = actor;
        this.targetUser = targetUser;
        this.action = action;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public User getActor() {
        return actor;
    }

    public User getTargetUser() {
        return targetUser;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
