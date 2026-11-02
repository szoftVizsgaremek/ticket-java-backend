-- V1__init.sql
-- Ticket system schema for MariaDB 10.2.1+ (CHECK constraints are enforced from that version).
-- All timestamps are UTC. DATETIME(6) stores no timezone, so the application
-- must write UTC (see spring.jpa.properties.hibernate.jdbc.time_zone=UTC).
-- Place at: src/main/resources/db/migration/V1__init.sql

-- ---------------------------------------------------------------- users

CREATE TABLE users (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    name                VARCHAR(100) NOT NULL,
    username            VARCHAR(50)  NOT NULL,
    email               VARCHAR(255) NOT NULL,
    hash                VARCHAR(100) NOT NULL,            -- bcrypt output is 60 chars; extra room for other algorithms later
    birth_date          DATE         NOT NULL,
    role                VARCHAR(20)  NOT NULL DEFAULT 'user',
    job_title           VARCHAR(100) NOT NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE, -- soft delete: FALSE means deactivated, row is kept
    password_changed_at DATETIME(6)  NULL,                  -- NULL = password must be changed; drives the 3-month expiry
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_role    CHECK (role IN ('user', 'admin', 'superadmin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------- tickets

CREATE TABLE tickets (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(250) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'open',
    priority    VARCHAR(20)  NOT NULL DEFAULT 'low',
    type        VARCHAR(20)  NOT NULL,
    uploaded_by BIGINT       NOT NULL,
    assignee_id BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT PRIMARY KEY (id),
    CONSTRAINT fk_tickets_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_assignee    FOREIGN KEY (assignee_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_tickets_status     CHECK (status   IN ('open', 'in_progress', 'closed')),
    CONSTRAINT chk_tickets_priority   CHECK (priority IN ('low', 'moderate', 'high', 'critical')),
    CONSTRAINT chk_tickets_type       CHECK (type     IN ('thermal', 'software', 'hardware')),

    INDEX idx_tickets_created_at (created_at)             -- serves ORDER BY created_at DESC
    -- uploaded_by and assignee_id are indexed automatically by their foreign keys
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------- audit_log
-- Append-only history of sensitive actions (password changes, role changes,
-- deactivations). Never store passwords or hashes here.

CREATE TABLE audit_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    actor_id       BIGINT       NOT NULL,                  -- who did it
    target_user_id BIGINT       NOT NULL,                  -- who it was done to
    action         VARCHAR(40)  NOT NULL,                  -- e.g. PASSWORD_CHANGED, PASSWORD_RESET, ROLE_CHANGED, USER_DEACTIVATED
    details        VARCHAR(255) NULL,                      -- e.g. 'user -> admin'
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT PRIMARY KEY (id),
    CONSTRAINT fk_audit_actor   FOREIGN KEY (actor_id)       REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_audit_target  FOREIGN KEY (target_user_id) REFERENCES users (id) ON DELETE RESTRICT,

    INDEX idx_audit_target_created (target_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;