-- =====================================================================
-- NSU RDS 2026 - schema for module 20 (RDS Administration & Monitoring).
--
-- Module 20 - table owner: module 20.
-- Tables: system_parameters, academic_calendar, monitoring_samples.
--
-- CONVENTIONS (INTEGRATION.md §4) - kept:
-- * Idempotent: CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS,
--   so setup-db.py can be re-run safely.
-- * This module OWNS these tables; other modules never write them.
-- * Semesters (00_core) are referenced read-only via FK. Timestamps:
--   TIMESTAMPTZ - synthetic data only.
--
-- system_parameters: runtime configuration (registration.open,
-- semester.current, maintenance_mode, ...). Boolean parameters accept
-- only true/false (enforced in the service layer); changes are audited
-- via module 19 and maintenance_mode flips broadcast via module 16.
-- =====================================================================

CREATE TABLE IF NOT EXISTS system_parameters (
    param_key   VARCHAR(40)  PRIMARY KEY,
    param_value VARCHAR(200) NOT NULL,
    description VARCHAR(200) NOT NULL,
    updated_by  VARCHAR(40)  NOT NULL DEFAULT 'admin',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_system_parameters_value ON system_parameters (param_value);

-- Academic calendar: dated events per semester (the FALL2026 add/drop
-- deadline 2026-11-30 is consistent with module 05's academic rules).
CREATE TABLE IF NOT EXISTS academic_calendar (
    event_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    semester_code VARCHAR(10) NOT NULL REFERENCES semesters (semester_code),
    event_name    VARCHAR(60) NOT NULL,
    event_date    DATE        NOT NULL,
    event_type    VARCHAR(10) NOT NULL
                  CHECK (event_type IN ('START', 'DEADLINE', 'HOLIDAY', 'EXAM', 'END')),
    description   VARCHAR(200),
    UNIQUE (semester_code, event_name)
);

CREATE INDEX IF NOT EXISTS idx_academic_calendar_semester ON academic_calendar (semester_code);
CREATE INDEX IF NOT EXISTS idx_academic_calendar_date     ON academic_calendar (event_date);

-- Health monitoring samples (recorded at runtime; a few historical
-- samples are seeded).
CREATE TABLE IF NOT EXISTS monitoring_samples (
    sample_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sampled_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    active_users  INTEGER     NOT NULL CHECK (active_users BETWEEN 0 AND 10000),
    db_latency_ms INTEGER     NOT NULL CHECK (db_latency_ms BETWEEN 0 AND 60000),
    error_count   INTEGER     NOT NULL DEFAULT 0 CHECK (error_count BETWEEN 0 AND 100000)
);

CREATE INDEX IF NOT EXISTS idx_monitoring_samples_time ON monitoring_samples (sampled_at);
