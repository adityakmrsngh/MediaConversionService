package org.zendly.mediaconversionservice.context;

import lombok.Getter;



/**
 * Thread-local storage for tenant context information
 * Provides tenant isolation per request thread
 */
@Getter
public class TenantContext {
    
    private final String tenantId;
    private final String databaseName;
    private final String connectionString;
    private final boolean isActive;
    
    public TenantContext(String tenantId, String databaseName, String connectionString,
                         boolean isActive) {
        this.tenantId = tenantId;
        this.databaseName = databaseName;
        this.connectionString = connectionString;
        this.isActive = isActive;
    }
    
    @Override
    public String toString() {
        return "TenantContext{" +
                "tenantId='" + tenantId + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
