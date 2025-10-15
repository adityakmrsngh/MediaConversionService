package org.zendly.mediaconversionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for document conversion results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionResponse {
    
    /**
     * Unique document identifier
     */
    private String documentId;
    
    /**
     * Extracted text content from the document
     */
    private String extractedText;
    
    /**
     * Conversion status (SUCCESS, FAILED, NOT_SUPPORTED)
     */
    private String status;
    
    /**
     * Method used for conversion (APACHE_TIKA, PDFBOX_TESS4J_OCR, etc.)
     */
    private String conversionMethod;
    
    /**
     * Metadata about the conversion process
     */
    private ConversionMetadata metadata;
    
    /**
     * Error message if conversion failed
     */
    private String errorMessage;
    
    /**
     * Processing time in milliseconds
     */
    private Long processingTimeMs;
}
