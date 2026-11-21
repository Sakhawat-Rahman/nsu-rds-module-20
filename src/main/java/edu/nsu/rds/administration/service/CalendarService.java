package edu.nsu.rds.administration.service;

import edu.nsu.rds.administration.dao.CalendarDAO;
import edu.nsu.rds.administration.model.AcademicCalendarEvent;
import edu.nsu.rds.administration.util.ValidationException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarService {
    private static final Set<String> ALLOWED_EVENT_TYPES = new HashSet<>(
            Arrays.asList("START", "DEADLINE", "HOLIDAY", "EXAM", "END")
    );

    private final CalendarDAO calendarDAO;

    public CalendarService(CalendarDAO calendarDAO) {
        this.calendarDAO = calendarDAO;
    }

    public List<AcademicCalendarEvent> getAllEvents() {
        return calendarDAO.findAll();
    }

    public List<AcademicCalendarEvent> getEventsBySemester(String semesterCode) {
        return calendarDAO.findBySemester(semesterCode);
    }

    public AcademicCalendarEvent createEvent(String semesterCode, String eventName, LocalDate eventDate,
                                             String eventType, String description) throws ValidationException {
        validateEventFields(semesterCode, eventName, eventDate, eventType);

        AcademicCalendarEvent event = new AcademicCalendarEvent(
                semesterCode.trim(),
                eventName.trim(),
                eventDate,
                eventType.trim().toUpperCase(),
                description != null ? description.trim() : ""
        );
        return calendarDAO.insert(event);
    }

    public boolean updateEvent(Long eventId, String semesterCode, String eventName, LocalDate eventDate,
                               String eventType, String description) throws ValidationException {
        if (eventId == null || eventId <= 0) {
            throw new ValidationException("Invalid event ID for update.");
        }
        validateEventFields(semesterCode, eventName, eventDate, eventType);

        AcademicCalendarEvent event = new AcademicCalendarEvent(
                eventId,
                semesterCode.trim(),
                eventName.trim(),
                eventDate,
                eventType.trim().toUpperCase(),
                description != null ? description.trim() : ""
        );
        return calendarDAO.update(event);
    }

    public boolean deleteEvent(Long eventId) throws ValidationException {
        if (eventId == null || eventId <= 0) {
            throw new ValidationException("Invalid event ID for deletion.");
        }
        return calendarDAO.delete(eventId);
    }

    private void validateEventFields(String semesterCode, String eventName, LocalDate eventDate, String eventType)
            throws ValidationException {
        if (semesterCode == null || semesterCode.isBlank()) {
            throw new ValidationException("Semester code must not be blank.");
        }
        if (eventName == null || eventName.isBlank()) {
            throw new ValidationException("Event name must not be blank.");
        }
        if (eventDate == null) {
            throw new ValidationException("Event date must not be null.");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new ValidationException("Event type must not be blank.");
        }
        String normalizedType = eventType.trim().toUpperCase();
        if (!ALLOWED_EVENT_TYPES.contains(normalizedType)) {
            throw new ValidationException("Event type must be strictly one of: START, DEADLINE, HOLIDAY, EXAM, END.");
        }
    }
}
