package edu.nsu.rds.administration.service;

import edu.nsu.rds.administration.dao.MonitoringDAO;
import edu.nsu.rds.administration.model.MonitoringSample;
import edu.nsu.rds.administration.util.ValidationException;

import java.util.List;
import java.util.Optional;

public class MonitoringService {
    private final MonitoringDAO monitoringDAO;

    public MonitoringService(MonitoringDAO monitoringDAO) {
        this.monitoringDAO = monitoringDAO;
    }

    public List<MonitoringSample> getHistoricalSamples() {
        return monitoringDAO.findAll();
    }

    public MonitoringSample recordSample(int activeUsers, int dbLatencyMs, int errorCount) throws ValidationException {
        if (activeUsers < 0 || activeUsers > 10000) {
            throw new ValidationException("Active users must be between 0 and 10000 (provided: " + activeUsers + ").");
        }
        if (dbLatencyMs < 0 || dbLatencyMs > 60000) {
            throw new ValidationException("Database latency must be between 0 and 60000 ms (provided: " + dbLatencyMs + ").");
        }
        if (errorCount < 0 || errorCount > 100000) {
            throw new ValidationException("Error count must be between 0 and 100000 (provided: " + errorCount + ").");
        }

        MonitoringSample sample = new MonitoringSample(activeUsers, dbLatencyMs, errorCount);
        return monitoringDAO.insert(sample);
    }

    public Optional<MonitoringSample> getLatestSample() {
        List<MonitoringSample> samples = getHistoricalSamples();
        if (samples.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(samples.get(0));
    }

    public int getAverageLatencyMs() {
        List<MonitoringSample> samples = getHistoricalSamples();
        if (samples.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (MonitoringSample s : samples) {
            sum += s.getDbLatencyMs();
        }
        return Math.round((float) sum / samples.size());
    }

    public int getTotalErrors() {
        List<MonitoringSample> samples = getHistoricalSamples();
        int sum = 0;
        for (MonitoringSample s : samples) {
            sum += s.getErrorCount();
        }
        return sum;
    }
}
