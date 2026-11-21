package edu.nsu.rds.administration.model;

import java.time.OffsetDateTime;

/**
 * Entity model representing monitoring_samples (Table 3 in Module 20 schema).
 * Columns:
 * - sample_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
 * - sampled_at    TIMESTAMPTZ NOT NULL DEFAULT now()
 * - active_users  INTEGER     NOT NULL CHECK (active_users BETWEEN 0 AND 10000)
 * - db_latency_ms INTEGER     NOT NULL CHECK (db_latency_ms BETWEEN 0 AND 60000)
 * - error_count   INTEGER     NOT NULL DEFAULT 0 CHECK (error_count BETWEEN 0 AND 100000)
 */
public class MonitoringSample {
    private Long sampleId;
    private OffsetDateTime sampledAt;
    private int activeUsers;
    private int dbLatencyMs;
    private int errorCount;

    public MonitoringSample() {}

    public MonitoringSample(Long sampleId, OffsetDateTime sampledAt, int activeUsers, int dbLatencyMs, int errorCount) {
        this.sampleId = sampleId;
        this.sampledAt = sampledAt;
        this.activeUsers = activeUsers;
        this.dbLatencyMs = dbLatencyMs;
        this.errorCount = errorCount;
    }

    public MonitoringSample(int activeUsers, int dbLatencyMs, int errorCount) {
        this(null, OffsetDateTime.now(), activeUsers, dbLatencyMs, errorCount);
    }

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public OffsetDateTime getSampledAt() {
        return sampledAt;
    }

    public void setSampledAt(OffsetDateTime sampledAt) {
        this.sampledAt = sampledAt;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getDbLatencyMs() {
        return dbLatencyMs;
    }

    public void setDbLatencyMs(int dbLatencyMs) {
        this.dbLatencyMs = dbLatencyMs;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    @Override
    public String toString() {
        return "MonitoringSample{" +
                "sampleId=" + sampleId +
                ", sampledAt=" + sampledAt +
                ", activeUsers=" + activeUsers +
                ", dbLatencyMs=" + dbLatencyMs +
                ", errorCount=" + errorCount +
                '}';
    }
}
