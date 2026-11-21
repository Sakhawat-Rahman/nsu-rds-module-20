package edu.nsu.rds.administration.model;

import java.time.OffsetDateTime;

/**
 * Entity model representing system_parameters (Table 1 in Module 20 schema).
 * Columns:
 * - param_key   VARCHAR(40)  PRIMARY KEY
 * - param_value VARCHAR(200) NOT NULL
 * - description VARCHAR(200) NOT NULL
 * - updated_by  VARCHAR(40)  NOT NULL DEFAULT 'admin'
 * - updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
 */
public class SystemParameter {
    private String paramKey;
    private String paramValue;
    private String description;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    public SystemParameter() {}

    public SystemParameter(String paramKey, String paramValue, String description, String updatedBy, OffsetDateTime updatedAt) {
        this.paramKey = paramKey;
        this.paramValue = paramValue;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SystemParameter{" +
                "paramKey='" + paramKey + '\'' +
                ", paramValue='" + paramValue + '\'' +
                ", description='" + description + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
