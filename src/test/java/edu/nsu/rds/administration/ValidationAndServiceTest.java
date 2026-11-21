package edu.nsu.rds.administration;

import edu.nsu.rds.administration.dao.CalendarDAO;
import edu.nsu.rds.administration.dao.MonitoringDAO;
import edu.nsu.rds.administration.dao.ParameterDAO;
import edu.nsu.rds.administration.model.AcademicCalendarEvent;
import edu.nsu.rds.administration.model.MonitoringSample;
import edu.nsu.rds.administration.model.SystemParameter;
import edu.nsu.rds.administration.service.CalendarService;
import edu.nsu.rds.administration.service.MonitoringService;
import edu.nsu.rds.administration.service.ParameterService;
import edu.nsu.rds.administration.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationAndServiceTest {
    private ParameterService parameterService;
    private CalendarService calendarService;
    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        parameterService = new ParameterService(new ParameterDAO());
        calendarService = new CalendarService(new CalendarDAO());
        monitoringService = new MonitoringService(new MonitoringDAO());
    }

    // =========================================================================
    // 1. SYSTEM_PARAMETERS SERVICE TESTS
    // =========================================================================

    @Test
    void testGetAllParametersContainsSeededKeys() {
        List<SystemParameter> params = parameterService.getAllParameters();
        assertFalse(params.isEmpty());
        assertTrue(params.stream().anyMatch(p -> p.getParamKey().equals("registration.open")));
        assertTrue(params.stream().anyMatch(p -> p.getParamKey().equals("semester.current")));
        assertTrue(params.stream().anyMatch(p -> p.getParamKey().equals("maintenance_mode")));
        assertTrue(params.stream().anyMatch(p -> p.getParamKey().equals("max_login_attempts")));
        assertTrue(params.stream().anyMatch(p -> p.getParamKey().equals("support_email")));
    }

    @Test
    void testValidBooleanParameterUpdates() {
        assertDoesNotThrow(() -> parameterService.updateParameter("registration.open", "false", "admin"));
        assertDoesNotThrow(() -> parameterService.updateParameter("registration.open", "true", "admin"));
        assertDoesNotThrow(() -> parameterService.updateParameter("maintenance_mode", "true", "admin"));
        assertDoesNotThrow(() -> parameterService.updateParameter("maintenance_mode", "false", "admin"));
    }

    @Test
    void testInvalidBooleanParameterThrowsException() {
        assertThrows(ValidationException.class, () ->
                parameterService.updateParameter("registration.open", "yes", "admin"));
        assertThrows(ValidationException.class, () ->
                parameterService.updateParameter("registration.open", "1", "admin"));
        assertThrows(ValidationException.class, () ->
                parameterService.updateParameter("maintenance_mode", "enabled", "admin"));
        assertThrows(ValidationException.class, () ->
                parameterService.updateParameter("maintenance_mode", "0", "admin"));
    }

    @Test
    void testEmptyOrBlankParameterValueThrowsException() {
        assertThrows(ValidationException.class, () ->
                parameterService.updateParameter("max_login_attempts", "", "admin"));
        assertThrows(ValidationException.class, () ->
                parameterService.updateParameter("max_login_attempts", "   ", "admin"));
        assertThrows(ValidationException.class, () ->
                parameterService.updateParameter("max_login_attempts", null, "admin"));
    }

    @Test
    void testParameterConvenienceMethods() throws ValidationException {
        parameterService.updateParameter("semester.current", "FALL2026", "admin");
        assertEquals("FALL2026", parameterService.getCurrentSemester());

        parameterService.updateParameter("maintenance_mode", "true", "admin");
        assertTrue(parameterService.isMaintenanceModeActive());

        parameterService.updateParameter("maintenance_mode", "false", "admin");
        assertFalse(parameterService.isMaintenanceModeActive());
    }

    // =========================================================================
    // 2. ACADEMIC_CALENDAR SERVICE TESTS
    // =========================================================================

    @Test
    void testValidEventCreationAndRetrieval() throws ValidationException {
        AcademicCalendarEvent event = calendarService.createEvent(
                "FALL2026", "Orientation Day", LocalDate.of(2026, 9, 5), "START", "New student orientation"
        );
        assertNotNull(event);
        assertEquals("Orientation Day", event.getEventName());
        assertEquals("START", event.getEventType());

        List<AcademicCalendarEvent> fallEvents = calendarService.getEventsBySemester("FALL2026");
        assertFalse(fallEvents.isEmpty());
    }

    @Test
    void testAllAllowedEventTypesAccepted() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        assertDoesNotThrow(() -> calendarService.createEvent("FALL2026", "Test Start", d, "START", "Desc"));
        assertDoesNotThrow(() -> calendarService.createEvent("FALL2026", "Test Deadline", d, "DEADLINE", "Desc"));
        assertDoesNotThrow(() -> calendarService.createEvent("FALL2026", "Test Holiday", d, "HOLIDAY", "Desc"));
        assertDoesNotThrow(() -> calendarService.createEvent("FALL2026", "Test Exam", d, "EXAM", "Desc"));
        assertDoesNotThrow(() -> calendarService.createEvent("FALL2026", "Test End", d, "END", "Desc"));
    }

    @Test
    void testInvalidEventTypeThrowsException() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        assertThrows(ValidationException.class, () ->
                calendarService.createEvent("FALL2026", "Invalid Type Test", d, "MIDTERM", "Desc"));
        assertThrows(ValidationException.class, () ->
                calendarService.createEvent("FALL2026", "Invalid Type Test", d, "ALERT", "Desc"));
        assertThrows(ValidationException.class, () ->
                calendarService.createEvent("FALL2026", "Invalid Type Test", d, "", "Desc"));
        assertThrows(ValidationException.class, () ->
                calendarService.createEvent("FALL2026", "Invalid Type Test", d, null, "Desc"));
    }

    @Test
    void testBlankCalendarEventFieldsThrowException() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        assertThrows(ValidationException.class, () ->
                calendarService.createEvent("", "Event", d, "START", "Desc"));
        assertThrows(ValidationException.class, () ->
                calendarService.createEvent("FALL2026", "", d, "START", "Desc"));
        assertThrows(ValidationException.class, () ->
                calendarService.createEvent("FALL2026", "Event", null, "START", "Desc"));
    }

    @Test
    void testCalendarUpdateAndDelete() throws ValidationException {
        AcademicCalendarEvent event = calendarService.createEvent(
                "FALL2026", "Temp Event", LocalDate.of(2026, 11, 1), "HOLIDAY", "Temp"
        );
        Long id = event.getEventId();
        assertNotNull(id);

        boolean updated = calendarService.updateEvent(id, "FALL2026", "Updated Temp Event",
                LocalDate.of(2026, 11, 2), "DEADLINE", "Updated desc");
        assertTrue(updated);

        boolean deleted = calendarService.deleteEvent(id);
        assertTrue(deleted);
    }

    // =========================================================================
    // 3. MONITORING_SAMPLES SERVICE TESTS
    // =========================================================================

    @Test
    void testGetHistoricalSamplesNotEmpty() {
        List<MonitoringSample> samples = monitoringService.getHistoricalSamples();
        assertFalse(samples.isEmpty());
        assertTrue(samples.size() >= 5);
    }

    @Test
    void testRecordValidSample() throws ValidationException {
        MonitoringSample sample = monitoringService.recordSample(45, 12, 0);
        assertNotNull(sample);
        assertEquals(45, sample.getActiveUsers());
        assertEquals(12, sample.getDbLatencyMs());
        assertEquals(0, sample.getErrorCount());
    }

    @Test
    void testActiveUsersBoundaryAndExceedanceValidation() {
        // Valid exact boundaries: 0 and 10000
        assertDoesNotThrow(() -> monitoringService.recordSample(0, 10, 0));
        assertDoesNotThrow(() -> monitoringService.recordSample(10000, 10, 0));

        // Negative value
        assertThrows(ValidationException.class, () -> monitoringService.recordSample(-1, 10, 0));
        // Exceeds 10000
        assertThrows(ValidationException.class, () -> monitoringService.recordSample(10001, 10, 0));
    }

    @Test
    void testDbLatencyBoundaryAndExceedanceValidation() {
        // Valid exact boundaries: 0 and 60000
        assertDoesNotThrow(() -> monitoringService.recordSample(50, 0, 0));
        assertDoesNotThrow(() -> monitoringService.recordSample(50, 60000, 0));

        // Negative value
        assertThrows(ValidationException.class, () -> monitoringService.recordSample(50, -1, 0));
        // Exceeds 60000
        assertThrows(ValidationException.class, () -> monitoringService.recordSample(50, 60001, 0));
    }

    @Test
    void testErrorCountBoundaryAndExceedanceValidation() {
        // Valid exact boundaries: 0 and 100000
        assertDoesNotThrow(() -> monitoringService.recordSample(50, 10, 0));
        assertDoesNotThrow(() -> monitoringService.recordSample(50, 10, 100000));

        // Negative value
        assertThrows(ValidationException.class, () -> monitoringService.recordSample(50, 10, -1));
        // Exceeds 100000
        assertThrows(ValidationException.class, () -> monitoringService.recordSample(50, 10, 100001));
    }

    @Test
    void testMonitoringMetricsCalculations() {
        int avgLatency = monitoringService.getAverageLatencyMs();
        assertTrue(avgLatency >= 0 && avgLatency <= 60000);

        int totalErrors = monitoringService.getTotalErrors();
        assertTrue(totalErrors >= 0);
    }
}
