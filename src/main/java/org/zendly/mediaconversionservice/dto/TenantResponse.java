package org.zendly.mediaconversionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response payload for tenant information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    
    private String tenantId;
    private String companyName;
    private String connectionString;
    private String contactEmail;
    private String subscriptionPlan;
    private Long maxCallPerHour;
    private String dbName;
    private String createdBy;
    private String updatedBy;
    private Long updatedAt;
    private Long createdAt;
    private Boolean isActive;
    private Map<String, Object> additionalConfigs;
}
