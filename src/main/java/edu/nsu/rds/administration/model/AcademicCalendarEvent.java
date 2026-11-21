package edu.nsu.rds.administration.model;

import java.time.LocalDate;

/**
 * Entity model representing academic_calendar (Table 2 in Module 20 schema).
 * Columns:
 * - event_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
 * - semester_code VARCHAR(10) NOT NULL REFERENCES semesters (semester_code)
 * - event_name    VARCHAR(60) NOT NULL
 * - event_date    DATE        NOT NULL
 * - event_type    VARCHAR(10) NOT NULL CHECK (event_type IN ('START', 'DEADLINE', 'HOLIDAY', 'EXAM', 'END'))
 * - description   VARCHAR(200)
 * - UNIQUE (semester_code, event_name)
 */
public class AcademicCalendarEvent {
    private Long eventId;
    private String semesterCode;
    private String eventName;
    private LocalDate eventDate;
    private String eventType;
    private String description;

    public AcademicCalendarEvent() {}

    public AcademicCalendarEvent(Long eventId, String semesterCode, String eventName, LocalDate eventDate, String eventType, String description) {
        this.eventId = eventId;
        this.semesterCode = semesterCode;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.description = description;
    }

    public AcademicCalendarEvent(String semesterCode, String eventName, LocalDate eventDate, String eventType, String description) {
        this(null, semesterCode, eventName, eventDate, eventType, description);
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getSemesterCode() {
        return semesterCode;
    }

    public void setSemesterCode(String semesterCode) {
        this.semesterCode = semesterCode;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "AcademicCalendarEvent{" +
                "eventId=" + eventId +
                ", semesterCode='" + semesterCode + '\'' +
                ", eventName='" + eventName + '\'' +
                ", eventDate=" + eventDate +
                ", eventType='" + eventType + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
