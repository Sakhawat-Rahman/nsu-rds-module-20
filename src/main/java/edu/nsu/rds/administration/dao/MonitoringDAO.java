package edu.nsu.rds.administration.dao;

import edu.nsu.rds.administration.config.DatabaseConnection;
import edu.nsu.rds.administration.model.MonitoringSample;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class MonitoringDAO {
    private static final List<MonitoringSample> FALLBACK_STORE = new ArrayList<>();
    private static long fallbackIdSequence = 5;

    static {
        FALLBACK_STORE.add(new MonitoringSample(5L, OffsetDateTime.parse("2026-11-09T09:00:00+06:00"), 47, 8, 0));
        FALLBACK_STORE.add(new MonitoringSample(4L, OffsetDateTime.parse("2026-11-08T09:00:00+06:00"), 51, 15, 0));
        FALLBACK_STORE.add(new MonitoringSample(3L, OffsetDateTime.parse("2026-11-07T09:00:00+06:00"), 45, 11, 1));
        FALLBACK_STORE.add(new MonitoringSample(2L, OffsetDateTime.parse("2026-11-06T09:00:00+06:00"), 38, 9, 0));
        FALLBACK_STORE.add(new MonitoringSample(1L, OffsetDateTime.parse("2026-11-05T09:00:00+06:00"), 42, 12, 0));
    }

    public List<MonitoringSample> findAll() {
        String sql = "SELECT sample_id, sampled_at, active_users, db_latency_ms, error_count " +
                     "FROM monitoring_samples ORDER BY sampled_at DESC, sample_id DESC";
        List<MonitoringSample> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                OffsetDateTime odt = rs.getObject("sampled_at", OffsetDateTime.class);
                if (odt == null && rs.getTimestamp("sampled_at") != null) {
                    odt = rs.getTimestamp("sampled_at").toInstant().atOffset(OffsetDateTime.now().getOffset());
                }
                list.add(new MonitoringSample(
                        rs.getLong("sample_id"),
                        odt != null ? odt : OffsetDateTime.now(),
                        rs.getInt("active_users"),
                        rs.getInt("db_latency_ms"),
                        rs.getInt("error_count")
                ));
            }
            return list;
        } catch (SQLException e) {
            if (DatabaseConnection.isFallbackEnabled()) {
                synchronized (FALLBACK_STORE) {
                    return new ArrayList<>(FALLBACK_STORE);
                }
            }
            throw new RuntimeException("Database error loading monitoring samples: " + e.getMessage(), e);
        }
    }

    public MonitoringSample insert(MonitoringSample sample) {
        String sql = "INSERT INTO monitoring_samples (active_users, db_latency_ms, error_count) " +
                     "VALUES (?, ?, ?) RETURNING sample_id, sampled_at";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sample.getActiveUsers());
            stmt.setInt(2, sample.getDbLatencyMs());
            stmt.setInt(3, sample.getErrorCount());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    sample.setSampleId(rs.getLong("sample_id"));
                    OffsetDateTime odt = rs.getObject("sampled_at", OffsetDateTime.class);
                    if (odt == null && rs.getTimestamp("sampled_at") != null) {
                        odt = rs.getTimestamp("sampled_at").toInstant().atOffset(OffsetDateTime.now().getOffset());
                    }
                    if (odt != null) {
                        sample.setSampledAt(odt);
                    }
                    return sample;
                }
            }
            return sample;
        } catch (SQLException e) {
            if (DatabaseConnection.isFallbackEnabled()) {
                synchronized (FALLBACK_STORE) {
                    sample.setSampleId(++fallbackIdSequence);
                    sample.setSampledAt(OffsetDateTime.now());
                    FALLBACK_STORE.add(0, sample); // newest first
                    return sample;
                }
            }
            throw new RuntimeException("Database error inserting monitoring sample: " + e.getMessage(), e);
        }
    }
}
