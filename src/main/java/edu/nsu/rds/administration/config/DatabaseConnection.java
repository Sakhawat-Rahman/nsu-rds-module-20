package edu.nsu.rds.administration.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseConnection {
    private static String url = "jdbc:postgresql://localhost:5432/nsu_rds_db?connectTimeout=2&socketTimeout=5&loginTimeout=2";
    private static String adminUrl = "jdbc:postgresql://localhost:5432/postgres?connectTimeout=2&socketTimeout=5&loginTimeout=2";
    private static String username = "postgres";
    private static String password = "postgres";
    private static boolean fallbackEnabled = true;

    static {
        try {
            DriverManager.setLoginTimeout(2);
        } catch (Exception ignored) {}
        loadProperties();
    }

    public static synchronized void loadProperties() {
        String envPass = System.getenv("PGPASSWORD");
        if (envPass != null && !envPass.isBlank()) {
            password = envPass;
        }
        String envUser = System.getenv("PGUSER");
        if (envUser != null && !envUser.isBlank()) {
            username = envUser;
        }

        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                url = appendTimeouts(prop.getProperty("db.url", url));
                username = prop.getProperty("db.username", username);
                password = prop.getProperty("db.password", password);
                fallbackEnabled = Boolean.parseBoolean(prop.getProperty("db.fallback.memory", "true"));

                deriveAdminUrl();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load db.properties, using defaults.");
        }
    }

    private static String appendTimeouts(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return rawUrl;
        if (!rawUrl.contains("connectTimeout")) {
            String sep = rawUrl.contains("?") ? "&" : "?";
            return rawUrl + sep + "connectTimeout=2&socketTimeout=5&loginTimeout=2";
        }
        return rawUrl;
    }

    private static void deriveAdminUrl() {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash > 0) {
            int qIndex = url.indexOf('?', lastSlash);
            String queryParams = (qIndex >= 0) ? url.substring(qIndex) : "?connectTimeout=2&socketTimeout=5&loginTimeout=2";
            adminUrl = url.substring(0, lastSlash + 1) + "postgres" + queryParams;
        }
    }

    public static synchronized void setCredentials(String newUrl, String newUser, String newPass) {
        url = appendTimeouts(newUrl);
        username = newUser;
        password = newPass;
        deriveAdminUrl();
    }

    public static Connection getConnection() throws SQLException {
        try {
            DriverManager.setLoginTimeout(2);
        } catch (Exception ignored) {}
        return DriverManager.getConnection(url, username, password);
    }

    public static Connection getAdminConnection() throws SQLException {
        try {
            DriverManager.setLoginTimeout(2);
        } catch (Exception ignored) {}
        return DriverManager.getConnection(adminUrl, username, password);
    }

    public static boolean isDatabaseReachable() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public static String getUrl() {
        return url;
    }

    public static String getUsername() {
        return username;
    }

    public static synchronized boolean initializeDatabase() {
        loadProperties();
        try {
            DriverManager.setLoginTimeout(2);
        } catch (Exception ignored) {}

        try {
            // Step 1: Ensure target database exists if connected as postgres superuser
            try (Connection adminConn = getAdminConnection()) {
                String targetDbName = extractDatabaseName(url);
                if (targetDbName != null && !targetDbName.equalsIgnoreCase("postgres")) {
                    boolean exists = false;
                    try (PreparedStatement checkStmt = adminConn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                        checkStmt.setQueryTimeout(2);
                        checkStmt.setString(1, targetDbName);
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next()) {
                                exists = true;
                            }
                        }
                    }

                    if (!exists) {
                        try (Statement createStmt = adminConn.createStatement()) {
                            createStmt.setQueryTimeout(5);
                            createStmt.executeUpdate("CREATE DATABASE " + targetDbName);
                            System.out.println("Created database '" + targetDbName + "'.");
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Database existence check info: " + e.getMessage());
            }

            // Step 2: Connect to the active database and initialize prerequisite and schema
            try (Connection conn = getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(5);

                    // Drop legacy draft tables if they linger from earlier versions
                    stmt.executeUpdate(
                            "DROP TABLE IF EXISTS academic_calendar_events, system_error_logs, " +
                            "subsystem_health_status, system_configurations, admin_calendar_event, " +
                            "admin_error_log, admin_module_status, admin_performance_metric, " +
                            "admin_system_parameter CASCADE"
                    );

                    // Ensure semesters table has semester_code
                    boolean needsRecreateSemesters = false;
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT 1 FROM information_schema.columns " +
                            "WHERE table_name = 'semesters' AND column_name = 'semester_id'")) {
                        if (rs.next()) {
                            needsRecreateSemesters = true;
                        }
                    }

                    if (needsRecreateSemesters) {
                        stmt.executeUpdate("DROP TABLE IF EXISTS academic_calendar CASCADE; DROP TABLE IF EXISTS semesters CASCADE;");
                    }

                    // Create prerequisite semesters table
                    stmt.executeUpdate(
                            "CREATE TABLE IF NOT EXISTS semesters (\n" +
                            "    semester_code VARCHAR(10) PRIMARY KEY\n" +
                            ");\n" +
                            "INSERT INTO semesters (semester_code) VALUES ('FALL2026') ON CONFLICT DO NOTHING;"
                    );
                }

                // Step 3: Run official 20_administration.sql and 20_administration_data.sql idempotently
                executeSqlScript(conn, "database/20_administration.sql");
                executeSqlScript(conn, "database/20_administration_data.sql");
                System.out.println("Module 20 database initialized with official schema and data.");
                return true;
            }
        } catch (Exception e) {
            System.err.println("PostgreSQL live connection notice: " + e.getMessage());
            return false;
        }
    }

    private static String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        int lastSlash = jdbcUrl.lastIndexOf('/');
        if (lastSlash < 0) return null;
        int q = jdbcUrl.indexOf('?', lastSlash);
        return q >= 0 ? jdbcUrl.substring(lastSlash + 1, q) : jdbcUrl.substring(lastSlash + 1);
    }

    public static void executeSqlScript(Connection conn, String scriptPath) {
        String content = "";
        try {
            Path p = Paths.get(scriptPath);
            if (Files.exists(p)) {
                content = Files.readString(p, StandardCharsets.UTF_8);
            } else {
                try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(scriptPath)) {
                    if (in != null) {
                        content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }

            if (content.isBlank()) {
                return;
            }

            List<String> statements = parseSqlStatements(content);
            try (Statement statement = conn.createStatement()) {
                statement.setQueryTimeout(10);
                for (String sql : statements) {
                    try {
                        statement.execute(sql);
                    } catch (SQLException ex) {
                        // Idempotent execution (tables or records already exist)
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Notice executing " + scriptPath + ": " + e.getMessage());
        }
    }

    public static List<String> parseSqlStatements(String content) {
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDollarBlock = false;

        for (String line : content.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") && !inDollarBlock) {
                continue;
            }
            current.append(line).append("\n");

            int idx = 0;
            while ((idx = line.indexOf("$$", idx)) != -1) {
                inDollarBlock = !inDollarBlock;
                idx += 2;
            }

            if (!inDollarBlock && trimmed.endsWith(";")) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    list.add(stmt);
                }
                current.setLength(0);
            }
        }
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            list.add(remaining);
        }
        return list;
    }
}
