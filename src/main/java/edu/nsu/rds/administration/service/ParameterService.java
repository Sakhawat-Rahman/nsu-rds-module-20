package edu.nsu.rds.administration.service;

import edu.nsu.rds.administration.dao.ParameterDAO;
import edu.nsu.rds.administration.model.SystemParameter;
import edu.nsu.rds.administration.util.ValidationException;

import java.util.List;
import java.util.Optional;

public class ParameterService {
    private final ParameterDAO parameterDAO;

    public ParameterService(ParameterDAO parameterDAO) {
        this.parameterDAO = parameterDAO;
    }

    public List<SystemParameter> getAllParameters() {
        return parameterDAO.findAll();
    }

    public Optional<SystemParameter> getParameter(String paramKey) {
        if (paramKey == null || paramKey.isBlank()) {
            return Optional.empty();
        }
        return parameterDAO.findByKey(paramKey.trim());
    }

    public boolean updateParameter(String paramKey, String paramValue, String updatedBy) throws ValidationException {
        if (paramKey == null || paramKey.isBlank()) {
            throw new ValidationException("Parameter key must not be blank.");
        }
        if (paramValue == null || paramValue.trim().isEmpty()) {
            throw new ValidationException("Parameter value must not be empty.");
        }

        String key = paramKey.trim();
        String val = paramValue.trim();

        // Boolean parameters validation as specified in instructions
        if (key.equalsIgnoreCase("registration.open") || key.equalsIgnoreCase("maintenance_mode")) {
            if (!val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("false")) {
                throw new ValidationException("Parameter '" + key + "' is boolean and accepts only 'true' or 'false'.");
            }
            val = val.toLowerCase();
        }

        String user = (updatedBy == null || updatedBy.isBlank()) ? "admin" : updatedBy.trim();
        return parameterDAO.updateParameterValue(key, val, user);
    }

    public boolean isMaintenanceModeActive() {
        return getParameter("maintenance_mode")
                .map(p -> "true".equalsIgnoreCase(p.getParamValue()))
                .orElse(false);
    }

    public boolean isRegistrationOpen() {
        return getParameter("registration.open")
                .map(p -> "true".equalsIgnoreCase(p.getParamValue()))
                .orElse(true);
    }

    public String getCurrentSemester() {
        return getParameter("semester.current")
                .map(SystemParameter::getParamValue)
                .orElse("FALL2026");
    }
}
