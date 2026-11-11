-- dev-seed.sql
-- NOT a migration. Do not put this in db/migration. Run by hand against a DEV database only:
--     mariadb -u ticketadmin -p ticketsystem < dev-seed.sql
--
-- Safe to re-run: every account is inserted with an explicit id, and a re-run restores
-- it to the state below (role, job title, active flag, password). Use that to undo
-- whatever a test round changed (promotions, deactivations, password changes).
--
-- Passwords (bcrypt $2a$, cost 12 - the format jBCrypt reads):
--   Abc123!      every account except testadmin2
--   NewPass123!  testadmin2  (matches the saved Insomnium login request)
--
-- Ids 1-7 and 10 are fixed on purpose so request bodies like assigneeId 7 stay valid.
--
-- If accounts with these usernames already exist under OTHER ids (e.g. users created by
-- hand through the API), uncomment the reset block first. Children before parents.
--
-- DELETE FROM audit_log;
-- DELETE FROM tickets;
-- DELETE FROM users;

INSERT INTO users (id, name, username, email, hash, birth_date, role, job_title, active, password_changed_at)
VALUES
 (1,  'Test Superadmin2', 'testsuper2',   'super2@example.com', '$2a$12$UuhaCRmGrsXr/T41qhYmcOD8uVoVpA9rVlSdxAxAhF0jnfrUkssT6', '1988-02-02', 'superadmin', 'DevOps Engineer',      TRUE,  UTC_TIMESTAMP(6)),
 (2,  'Former Employee',  'formeremp',    'former@example.com', '$2a$12$UuhaCRmGrsXr/T41qhYmcOD8uVoVpA9rVlSdxAxAhF0jnfrUkssT6', '1985-07-07', 'user',       'Warehouse Technician', FALSE, UTC_TIMESTAMP(6)),
 (3,  'Jane HR',          'janehr',       'jane@example.com',   '$2a$12$UuhaCRmGrsXr/T41qhYmcOD8uVoVpA9rVlSdxAxAhF0jnfrUkssT6', '1990-05-15', 'admin',      'Senior HR Manager',    TRUE,  UTC_TIMESTAMP(6)),
 (4,  'Test User3',       'testuser3',    'user3@example.com',  '$2a$12$UuhaCRmGrsXr/T41qhYmcOD8uVoVpA9rVlSdxAxAhF0jnfrUkssT6', '1997-09-09', 'user',       'Receptionist',         TRUE,  UTC_TIMESTAMP(6)),
 (5,  'Test Admin',       'testuser',     'test@example.com',   '$2a$12$UuhaCRmGrsXr/T41qhYmcOD8uVoVpA9rVlSdxAxAhF0jnfrUkssT6', '1990-01-01', 'superadmin', 'Software Engineer',    TRUE,  UTC_TIMESTAMP(6)),
 (6,  'Test User2',       'testuser2',    't2@example.com',     '$2a$12$UuhaCRmGrsXr/T41qhYmcOD8uVoVpA9rVlSdxAxAhF0jnfrUkssT6', '1995-03-20', 'user',       'Warehouse Technician', TRUE,  UTC_TIMESTAMP(6)),
 (7,  'Test Admin2',      'testadmin2',   'ta2@example.com',    '$2a$12$71g6jHX7W0pePGyHjrKgSuGWu47.yH4nDE8AfzFz0MN5MNrbXvwpa', '1992-01-01', 'admin',      'Area Manager',         TRUE,  UTC_TIMESTAMP(6)),
 (10, 'Area Coworker',    'areacoworker', 'ac@example.com',     '$2a$12$UuhaCRmGrsXr/T41qhYmcOD8uVoVpA9rVlSdxAxAhF0jnfrUkssT6', '1999-01-01', 'user',       'Area Manager',         TRUE,  UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    name                = VALUES(name),
    username            = VALUES(username),
    email               = VALUES(email),
    hash                = VALUES(hash),
    birth_date          = VALUES(birth_date),
    role                = VALUES(role),
    job_title           = VALUES(job_title),
    active              = VALUES(active),
    password_changed_at = VALUES(password_changed_at);

-- Sanity check: should list 8 rows, with formeremp inactive.
SELECT id, username, role, job_title, active FROM users ORDER BY id;
