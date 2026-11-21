-- =====================================================================
-- NSU RDS 2026 - sample data for module 20 (RDS Administration &
-- Monitoring).
--
-- Canonical seed (BUILD-ALL-20-MODULES.md "Administration (20)"):
-- * system_parameters: registration.open=true, semester.current=FALL2026,
--   maintenance_mode=false (+ max_login_attempts, support_email);
-- * academic_calendar: FALL2026 events consistent with 00_core's semester
--   dates (2026-09-07 .. 2026-12-19) and module 05's add/drop rule
--   (deadline 2026-11-30);
-- * monitoring_samples: 5 historical health samples.
--
-- Idempotency: parameters and calendar events use natural keys with
-- ON CONFLICT DO NOTHING; the samples (IDENTITY pk, no natural key) are
-- guarded by a marker-row check inside one DO block.
-- =====================================================================

INSERT INTO system_parameters (param_key, param_value, description, updated_by) VALUES
    ('registration.open', 'true',
     'Whether course registration is open for the current semester', 'admin'),
    ('semester.current', 'FALL2026',
     'The semester the portal screens default to', 'admin'),
    ('maintenance_mode', 'false',
     'When true, a maintenance notice is broadcast portal-wide (module 16)', 'admin'),
    ('max_login_attempts', '3',
     'Failed logins before an account locks (module 01 behavior)', 'admin'),
    ('support_email', 'support@nsu-demo.example',
     'Helpdesk contact shown in the portal', 'admin')
ON CONFLICT (param_key) DO NOTHING;

INSERT INTO academic_calendar
    (semester_code, event_name, event_date, event_type, description)
VALUES
    ('FALL2026', 'Semester begins', DATE '2026-09-07', 'START',
     'Classes start for the Fall 2026 semester'),
    ('FALL2026', 'Add/drop deadline', DATE '2026-11-30', 'DEADLINE',
     'Last day to add or drop a course (matches the module 05 academic rule)'),
    ('FALL2026', 'Mid-semester break', DATE '2026-10-26', 'HOLIDAY',
     'University closed for the mid-semester break'),
    ('FALL2026', 'Final examinations begin', DATE '2026-12-07', 'EXAM',
     'Final examination period starts'),
    ('FALL2026', 'Semester ends', DATE '2026-12-19', 'END',
     'Last day of the Fall 2026 semester')
ON CONFLICT (semester_code, event_name) DO NOTHING;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM monitoring_samples
                   WHERE active_users = 51 AND db_latency_ms = 15) THEN
        INSERT INTO monitoring_samples
            (sampled_at, active_users, db_latency_ms, error_count)
        VALUES
            ('2026-11-05 09:00:00+06', 42, 12, 0),
            ('2026-11-06 09:00:00+06', 38, 9,  0),
            ('2026-11-07 09:00:00+06', 45, 11, 1),
            ('2026-11-08 09:00:00+06', 51, 15, 0),
            ('2026-11-09 09:00:00+06', 47, 8,  0);
    END IF;
END $$;
