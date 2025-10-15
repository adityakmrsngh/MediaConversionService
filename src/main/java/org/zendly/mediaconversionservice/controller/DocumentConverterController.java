package org.zendly.mediaconversionservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.zendly.mediaconversionservice.service.WorkflowOrchestratorService;

/**
 * REST Controller for document management operations
 */
@RestController
@RequestMapping("/api/documents")
@Slf4j
@Validated
public class DocumentConverterController {
    /**
     * Get document by ID with download URL
     */
    private final WorkflowOrchestratorService workflowOrchestratorService;

    public DocumentConverterController(WorkflowOrchestratorService workflowOrchestratorService) {
        this.workflowOrchestratorService = workflowOrchestratorService;
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<String> convertDocument(
            @PathVariable String documentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("Converting document: {} for tenant: {}", documentId, tenantId);
        try {
            workflowOrchestratorService.getDocument(documentId);
            return ResponseEntity.ok("");
        } catch (RuntimeException e) {
            log.error("Error Converting document: {} for tenant: {}", documentId, tenantId, e);
            if (e.getMessage().contains("not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId);
            } else if (e.getMessage().contains("Access denied")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for document: " + documentId);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error Converting document: " + e.getMessage());
            }
        }
    }
}
