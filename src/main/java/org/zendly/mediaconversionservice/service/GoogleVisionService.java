package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.dto.ConversionMetadata;
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;

import java.io.InputStream;

/**
 * Google Vision API service for text extraction fallback
 * Currently a stub implementation - ready for Google Vision API integration
 */
@Slf4j
@Service
public class GoogleVisionService {

    /**
     * Extract text using Google Vision API (stub implementation)
     * TODO: Implement actual Google Vision API integration
     */
    public ConversionResponse extractText(InputStream inputStream, DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        
        log.info("Google Vision fallback requested for document: {}", documentResponse.getDocumentId());
        
        // TODO: Implement Google Vision API integration
        // Steps for future implementation:
        // 1. Convert InputStream to byte array or upload to Cloud Storage
        // 2. Configure Google Vision API client
        // 3. Call TEXT_DETECTION or DOCUMENT_TEXT_DETECTION
        // 4. Process the response and extract text
        // 5. Calculate confidence score from Vision API response
        
        // For now, return a stub response indicating the service is not yet implemented
        ConversionMetadata metadata = ConversionMetadata.builder()
                .ocrConfidence(0)
                .usedOcrFallback(true)
                .processingNotes("Google Vision API integration pending")
                .build();

        return ConversionResponse.builder()
                .documentId(documentResponse.getDocumentId())
                .extractedText("") // Empty text for stub
                .status(ApplicationConstants.CONVERSION_FAILED)
                .conversionMethod(ApplicationConstants.METHOD_IMAGE_VISION)
                .metadata(metadata)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
}
