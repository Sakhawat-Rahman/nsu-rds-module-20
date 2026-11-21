package edu.nsu.rds.administration.dao;

import edu.nsu.rds.administration.config.DatabaseConnection;
import edu.nsu.rds.administration.model.AcademicCalendarEvent;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CalendarDAO {
    private static final List<AcademicCalendarEvent> FALLBACK_STORE = new ArrayList<>();
    private static long fallbackIdSequence = 5;

    static {
        FALLBACK_STORE.add(new AcademicCalendarEvent(1L, "FALL2026", "Semester begins",
                LocalDate.of(2026, 9, 7), "START", "Classes start for the Fall 2026 semester"));
        FALLBACK_STORE.add(new AcademicCalendarEvent(2L, "FALL2026", "Add/drop deadline",
                LocalDate.of(2026, 11, 30), "DEADLINE", "Last day to add or drop a course (matches the module 05 academic rule)"));
        FALLBACK_STORE.add(new AcademicCalendarEvent(3L, "FALL2026", "Mid-semester break",
                LocalDate.of(2026, 10, 26), "HOLIDAY", "University closed for the mid-semester break"));
        FALLBACK_STORE.add(new AcademicCalendarEvent(4L, "FALL2026", "Final examinations begin",
                LocalDate.of(2026, 12, 7), "EXAM", "Final examination period starts"));
        FALLBACK_STORE.add(new AcademicCalendarEvent(5L, "FALL2026", "Semester ends",
                LocalDate.of(2026, 12, 19), "END", "Last day of the Fall 2026 semester"));
    }

    public List<AcademicCalendarEvent> findAll() {
        String sql = "SELECT event_id, semester_code, event_name, event_date, event_type, description " +
                     "FROM academic_calendar ORDER BY event_date ASC, event_id ASC";
        List<AcademicCalendarEvent> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Date d = rs.getDate("event_date");
                list.add(new AcademicCalendarEvent(
                        rs.getLong("event_id"),
                        rs.getString("semester_code"),
                        rs.getString("event_name"),
                        d != null ? d.toLocalDate() : null,
                        rs.getString("event_type"),
                        rs.getString("description")
                ));
            }
            return list;
        } catch (SQLException e) {
            if (DatabaseConnection.isFallbackEnabled()) {
                synchronized (FALLBACK_STORE) {
                    return new ArrayList<>(FALLBACK_STORE);
                }
            }
            throw new RuntimeException("Database error loading academic calendar: " + e.getMessage(), e);
        }
    }

    public List<AcademicCalendarEvent> findBySemester(String semesterCode) {
        if (semesterCode == null || semesterCode.isBlank() || semesterCode.equalsIgnoreCase("ALL")) {
            return findAll();
        }
        String sql = "SELECT event_id, semester_code, event_name, event_date, event_type, description " +
                     "FROM academic_calendar WHERE semester_code = ? ORDER BY event_date ASC, event_id ASC";
        List<AcademicCalendarEvent> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, semesterCode);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("event_date");
                    list.add(new AcademicCalendarEvent(
                            rs.getLong("event_id"),
                            rs.getString("semester_code"),
                            rs.getString("event_name"),
                            d != null ? d.toLocalDate() : null,
                            rs.getString("event_type"),
                            rs.getString("description")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            if (DatabaseConnection.isFallbackEnabled()) {
                synchronized (FALLBACK_STORE) {
                    List<AcademicCalendarEvent> filtered = new ArrayList<>();
                    for (AcademicCalendarEvent ev : FALLBACK_STORE) {
                        if (ev.getSemesterCode().equalsIgnoreCase(semesterCode)) {
                            filtered.add(ev);
                        }
                    }
                    return filtered;
                }
            }
            throw new RuntimeException("Database error filtering academic calendar: " + e.getMessage(), e);
        }
    }

    public AcademicCalendarEvent insert(AcademicCalendarEvent event) {
        String sql = "INSERT INTO academic_calendar (semester_code, event_name, event_date, event_type, description) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING event_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, event.getSemesterCode());
            stmt.setString(2, event.getEventName());
            stmt.setDate(3, Date.valueOf(event.getEventDate()));
            stmt.setString(4, event.getEventType());
            stmt.setString(5, event.getDescription());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    event.setEventId(rs.getLong(1));
                    return event;
                }
            }
            return event;
        } catch (SQLException e) {
            if (DatabaseConnection.isFallbackEnabled()) {
                synchronized (FALLBACK_STORE) {
                    event.setEventId(++fallbackIdSequence);
                    FALLBACK_STORE.add(event);
                    return event;
                }
            }
            throw new RuntimeException("Database error inserting calendar event: " + e.getMessage(), e);
        }
    }

    public boolean update(AcademicCalendarEvent event) {
        String sql = "UPDATE academic_calendar SET semester_code = ?, event_name = ?, event_date = ?, " +
                     "event_type = ?, description = ? WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, event.getSemesterCode());
            stmt.setString(2, event.getEventName());
            stmt.setDate(3, Date.valueOf(event.getEventDate()));
            stmt.setString(4, event.getEventType());
            stmt.setString(5, event.getDescription());
            stmt.setLong(6, event.getEventId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (DatabaseConnection.isFallbackEnabled()) {
                synchronized (FALLBACK_STORE) {
                    for (int i = 0; i < FALLBACK_STORE.size(); i++) {
                        if (FALLBACK_STORE.get(i).getEventId().equals(event.getEventId())) {
                            FALLBACK_STORE.set(i, event);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public boolean delete(Long eventId) {
        String sql = "DELETE FROM academic_calendar WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, eventId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (DatabaseConnection.isFallbackEnabled()) {
                synchronized (FALLBACK_STORE) {
                    return FALLBACK_STORE.removeIf(ev -> ev.getEventId().equals(eventId));
                }
            }
            return false;
        }
    }
}
