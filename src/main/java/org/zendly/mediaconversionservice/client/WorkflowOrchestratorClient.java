package org.zendly.mediaconversionservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.context.TenantContext;
import org.zendly.mediaconversionservice.context.TenantContextHolder;
import org.zendly.mediaconversionservice.dto.DocumentResponse;
import org.zendly.mediaconversionservice.dto.TenantResponse;
import org.zendly.mediaconversionservice.exception.WorkflowOrchestratorException;

import java.util.Optional;

/**
 * HTTP client for communicating with the Workflow Orchestrator service
 */
@Slf4j
@Component
public class WorkflowOrchestratorClient {
    
    private final RestTemplate restTemplate;
    @Value("${workflow.orchestrator.base-url}")
    private String orchestratorBaseUrl;
    @Value("${message-converter.tenant.header.name}")
    private String tenantHeader;
    @Value("${workflow.orchestrator.fetch-tenant.endpoint}")
    private String getTenantEndpoint;
    @Value("${workflow.orchestrator.document-get.endpoint}")
    private String documentGetEndpoint;


    public WorkflowOrchestratorClient(@Qualifier("workflowOrchestratorRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Get document from GCP cloud store via workflow orchestrator
     */
    public DocumentResponse getDocument(String documentId) {
        String url = orchestratorBaseUrl + documentGetEndpoint;
        url = url.replace("{documentId}", documentId);
        try {
            HttpEntity<Void> entity = createHttpEntity(null, Optional.empty());
            log.info("Getting document from orchestrator: {}", documentId);
            ResponseEntity<DocumentResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, DocumentResponse.class);
            DocumentResponse result = response.getBody();
            log.info("Successfully get document: {}", result != null ? result.getDocumentId() : "null");
            return result;
        } catch (RestClientException e) {
            log.error("Failed to get document: {}", documentId, e);
            throw new WorkflowOrchestratorException("Failed to get document: " + documentId, e);
        }
    }
    
    /**
     * Get tenant information by tenant ID
     */
    public TenantResponse getTenantById(String tenantId) {
        String url = orchestratorBaseUrl + getTenantEndpoint;
        url = url.replace("{tenantId}", tenantId);
        try {
            HttpEntity<Void> entity = createHttpEntity(null, Optional.of(tenantId));
            log.info("Getting tenant information for tenantId: {}", tenantId);
            ResponseEntity<TenantResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, TenantResponse.class);
            TenantResponse result = response.getBody();
            log.info("Successfully retrieved tenant information for tenantId: {}", tenantId);
            return result;
        } catch (RestClientException e) {
            log.error("Failed to get tenant information for tenantId: {}", tenantId, e);
            throw new WorkflowOrchestratorException("Failed to get tenant information for tenantId: " + tenantId, e);
        }
    }
    
    /**
     * Create HTTP entity with tenant header and proper content type
     */
    private <T> HttpEntity<T> createHttpEntity(T body, Optional<String> tenantId) {
        TenantContext tenantContext = TenantContextHolder.getContext();
        HttpHeaders headers = new HttpHeaders();
        if(tenantContext!=null && tenantContext.getTenantId()!=null){
            headers.set(tenantHeader, tenantContext.getTenantId());
        }
        tenantId.ifPresent(tenant->headers.set(tenantHeader,tenant));
        headers.set(HttpHeaders.CONTENT_TYPE, ApplicationConstants.CONTENT_TYPE_JSON);
        return new HttpEntity<>(body, headers);
    }
}
