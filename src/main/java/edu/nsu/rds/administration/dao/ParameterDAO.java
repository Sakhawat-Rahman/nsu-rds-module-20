package edu.nsu.rds.administration.dao;

import edu.nsu.rds.administration.config.DatabaseConnection;
import edu.nsu.rds.administration.model.SystemParameter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParameterDAO {
    private static final List<SystemParameter> FALLBACK_STORE = new ArrayList<>();

    static {
        FALLBACK_STORE.add(new SystemParameter("registration.open", "true",
                "Whether course registration is open for the current semester", "admin", OffsetDateTime.now()));
        FALLBACK_STORE.add(new SystemParameter("semester.current", "FALL2026",
                "The semester the portal screens default to", "admin", OffsetDateTime.now()));
        FALLBACK_STORE.add(new SystemParameter("maintenance_mode", "false",
                "When true, a maintenance notice is broadcast portal-wide (module 16)", "admin", OffsetDateTime.now()));
        FALLBACK_STORE.add(new SystemParameter("max_login_attempts", "3",
                "Failed logins before an account locks (module 01 behavior)", "admin", OffsetDateTime.now()));
        FALLBACK_STORE.add(new SystemParameter("support_email", "support@nsu-demo.example",
                "Helpdesk contact shown in the portal", "admin", OffsetDateTime.now()));
    }

    public List<SystemParameter> findAll() {
        String sql = "SELECT param_key, param_value, description, updated_by, updated_at FROM system_parameters ORDER BY param_key ASC";
        List<SystemParameter> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                OffsetDateTime odt = rs.getObject("updated_at", OffsetDateTime.class);
                if (odt == null && rs.getTimestamp("updated_at") != null) {
                    odt = rs.getTimestamp("updated_at").toInstant().atOffset(OffsetDateTime.now().getOffset());
                }
                list.add(new SystemParameter(
                        rs.getString("param_key"),
                        rs.getString("param_value"),
                        rs.getString("description"),
                        rs.getString("updated_by"),
                        odt != null ? odt : OffsetDateTime.now()
                ));
            }
            return list;
        } catch (SQLException e) {
            if (DatabaseConnection.isFallbackEnabled()) {
                synchronized (FALLBACK_STORE) {
                    return new ArrayList<>(FALLBACK_STORE);
                }
            }
            throw new RuntimeException("Database error loading system parameters: " + e.getMessage(), e);
        }
    }

    public Optional<SystemParameter> findByKey(String key) {
        String sql = "SELECT param_key, param_value, description, updated_by, updated_at FROM system_parameters WHERE LOWER(param_key) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    OffsetDateTime odt = rs.getObject("updated_at", OffsetDateTime.class);
                    if (odt == null && rs.getTimestamp("updated_at") != null) {
                        odt = rs.getTimestamp("updated_at").toInstant().atOffset(OffsetDateTime.now().getOffset());
                    }
                    return Optional.of(new SystemParameter(
                            rs.getString("param_key"),
                            rs.getString("param_value"),
                            rs.getString("description"),
                            rs.getString("updated_by"),
                            odt != null ? odt : OffsetDateTime.now()
                    ));
                }
            }
        } catch (SQLException e) {
            if (DatabaseConnection.isFallbackEnabled()) {
                synchronized (FALLBACK_STORE) {
                    return FALLBACK_STORE.stream()
                            .filter(p -> p.getParamKey().equalsIgnoreCase(key))
                            .findFirst();
                }
            }
            throw new RuntimeException("Database error finding parameter by key: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public boolean updateParameterValue(String key, String newValue, String updatedBy) {
        String sql = "UPDATE system_parameters SET param_value = ?, updated_by = ?, updated_at = now() WHERE LOWER(param_key) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newValue);
            stmt.setString(2, updatedBy);
            stmt.setString(3, key);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                // Also update fallback store in memory so in-memory state is synchronized
                synchronized (FALLBACK_STORE) {
                    for (SystemParameter param : FALLBACK_STORE) {
                        if (param.getParamKey().equalsIgnoreCase(key)) {
                            param.setParamValue(newValue);
                            param.setUpdatedBy(updatedBy);
                            param.setUpdatedAt(OffsetDateTime.now());
                            break;
                        }
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Notice updating database parameter '" + key + "': " + e.getMessage());
        }

        if (DatabaseConnection.isFallbackEnabled()) {
            synchronized (FALLBACK_STORE) {
                for (SystemParameter param : FALLBACK_STORE) {
                    if (param.getParamKey().equalsIgnoreCase(key)) {
                        param.setParamValue(newValue);
                        param.setUpdatedBy(updatedBy);
                        param.setUpdatedAt(OffsetDateTime.now());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
