package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zendly.mediaconversionservice.client.WorkflowOrchestratorClient;
import org.zendly.mediaconversionservice.dto.DocumentResponse;
import org.zendly.mediaconversionservice.dto.DocumentType;
import org.zendly.mediaconversionservice.dto.TenantResponse;
import org.zendly.mediaconversionservice.exception.WorkflowOrchestratorException;

import java.util.List;
import java.util.Map;

/**
 * Service for orchestrating workflow operations
 * Replaces DummyService with actual workflow orchestrator integration
 */
@Slf4j
@Service
public class WorkflowOrchestratorService {

    private final WorkflowOrchestratorClient workflowOrchestratorClient;

    public WorkflowOrchestratorService(WorkflowOrchestratorClient workflowOrchestratorClient) {
        this.workflowOrchestratorClient = workflowOrchestratorClient;
    }

    /**
     * Create a document in GCP cloud store via workflow orchestrator
     * @return The document creation response with upload URL
     */
    public DocumentResponse getDocument(String documentId) throws WorkflowOrchestratorException {
        if (documentId == null) {
            throw new IllegalArgumentException("CreateDocumentRequest cannot be null");
        }
        try {
            log.info("Getting document: {}", documentId);
            DocumentResponse document = workflowOrchestratorClient.getDocument(documentId);
            log.info("Successfully got document with ID: {}",documentId);
            return document;
        }catch (Exception e) {
            log.error("Unexpected error getting document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to get document", e);
        }
    }

    /**
     * Get tenant information by tenant ID
     * @param tenantId The tenant ID
     * @return The tenant information
     */
    public TenantResponse getTenantById(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }

        try {
            log.info("Getting tenant information for tenantId: {}", tenantId);

            TenantResponse response = workflowOrchestratorClient.getTenantById(tenantId);

            log.info("Successfully retrieved tenant information for tenantId: {}", tenantId);

            return response;

        } catch (WorkflowOrchestratorException e) {
            log.error("Error getting tenant information for tenantId {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to get tenant information", e);
        } catch (Exception e) {
            log.error("Unexpected error getting tenant information for tenantId {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to get tenant information", e);
        }
    }
}
