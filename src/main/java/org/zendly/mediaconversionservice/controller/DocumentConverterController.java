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
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;
import org.zendly.mediaconversionservice.service.DocumentConversionService;
import org.zendly.mediaconversionservice.service.WorkflowOrchestratorService;

/**
 * REST Controller for document conversion operations
 * Single responsibility: take a media input → produce structured text output
 */
@RestController
@RequestMapping("/api/documents")
@Slf4j
@Validated
public class DocumentConverterController {

    private final WorkflowOrchestratorService workflowOrchestratorService;
    private final DocumentConversionService documentConversionService;

    public DocumentConverterController(WorkflowOrchestratorService workflowOrchestratorService,
                                     DocumentConversionService documentConversionService) {
        this.workflowOrchestratorService = workflowOrchestratorService;
        this.documentConversionService = documentConversionService;
    }

    /**
     * Convert document to text based on document type and MIME type
     * Flow: Get document metadata → Download from GCP → Convert to text
     * 
     * Conversion strategy:
     * - Documents: Apache Tika first → PDFBox + Tess4J OCR fallback for scanned PDFs
     * - Audio: Google Speech-to-Text (stub implementation)
     * - Images: Google Vision API (stub implementation)
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<ConversionResponse> convertDocument(
            @PathVariable String documentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("Converting document: {} for tenant: {}", documentId, tenantId);
        
        try {
            // Step 1: Get document metadata from workflow orchestrator
            DocumentResponse documentResponse = workflowOrchestratorService.getDocument(documentId);
            
            if (documentResponse == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId);
            }

            log.info("Document metadata retrieved - ID: {}, Type: {}, MIME: {}", 
                    documentId, documentResponse.getDocumentType(), documentResponse.getMimeType());

            // Step 2: Convert document based on type and MIME type
            ConversionResponse conversionResponse = documentConversionService.convertDocument(documentResponse);

            // Step 3: Return conversion result
            if (conversionResponse != null) {
                log.info("Document conversion completed - ID: {}, Status: {}, Method: {}", 
                        documentId, conversionResponse.getStatus(), conversionResponse.getConversionMethod());
                return ResponseEntity.ok(conversionResponse);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Conversion service returned null response");
            }

        } catch (ResponseStatusException e) {
            // Re-throw ResponseStatusException as-is
            throw e;
        } catch (RuntimeException e) {
            log.error("Error converting document: {} for tenant: {}", documentId, tenantId, e);
            
            if (e.getMessage().contains("not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId);
            } else if (e.getMessage().contains("Access denied")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for document: " + documentId);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Error converting document: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error converting document: {} for tenant: {}", documentId, tenantId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Unexpected error during conversion: " + e.getMessage());
        }
    }
}
