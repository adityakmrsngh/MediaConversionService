package org.zendly.mediaconversionservice.context;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.zendly.mediaconversionservice.dto.TenantResponse;
import org.zendly.mediaconversionservice.service.WorkflowOrchestratorService;

/**
 * Service to resolve tenant information from HTTP requests
 * Extracts tenant ID from headers and resolves tenant configuration
 */
@Component
@Slf4j
public class TenantResolver {

    @Value("${message-converter.tenant.header.name}")
    private String tenantHeader;
    private final WorkflowOrchestratorService workflowOrchestratorService;
    
    @Autowired
    public TenantResolver(WorkflowOrchestratorService workflowOrchestratorService) {
        this.workflowOrchestratorService = workflowOrchestratorService;
    }

    public TenantContext resolveTenantContext(HttpServletRequest request) {
        String tenantId = extractTenantId(request);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.error("No tenant ID found in request,pass a valid tenant.");
            throw new RuntimeException("No tenant ID found in request,pass a valid tenant.");
        }
        return resolveTenantContext(tenantId);
    }

    /**
     * Resolve tenant context by tenant ID (internal method)
     * Uses caching to reduce API calls to WorkflowOrchestratorService
     * @param tenantId the tenant identifier
     * @return tenant context if resolved successfully, null otherwise
     */
    @Cacheable(value = "tenantCache", key = "#tenantId", unless = "#result == null")
    public TenantContext resolveTenantContext(final String tenantId) {
        try {
            log.debug("Fetching tenant context from API for: {}", tenantId);
            TenantResponse tenant = workflowOrchestratorService.getTenantById(tenantId);
            log.debug("Resolved tenant context from API: {}", tenantId);
            return new TenantContext(
                tenant.getTenantId(),
                tenant.getDbName(),
                tenant.getConnectionString(),
                tenant.getIsActive()
            );
            
        } catch (Exception e) {
            log.error("Error resolving tenant context for {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Error resolving tenant context for tenant " +tenantId,e);
        }
    }

    /**
     * Extract tenant ID from HTTP request headers
     * @param request the HTTP request
     * @return tenant ID from header, or null if not found
     */
    private String extractTenantId(HttpServletRequest request) {
        if (request == null) {
            log.warn("Cannot extract tenant ID: request is null");
            return null;
        }

        String tenantId = request.getHeader(tenantHeader);

        if (tenantId != null) {
            tenantId = tenantId.trim();
            log.info("Extracted tenant ID from header: {}", tenantId);
        } else {
            log.info("No {} header found in request", tenantHeader);
        }

        return tenantId;
    }
}
