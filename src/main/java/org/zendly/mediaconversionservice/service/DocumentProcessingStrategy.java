package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;

import java.util.Arrays;

/**
 * Simplified strategy class for determining optimal processing approach
 * Ultra-simple logic: Text documents and PDFs use TEXT_ONLY, Images use OCR
 */
@Slf4j
@Component
public class DocumentProcessingStrategy {

    /**
     * Determine processing strategy based on MIME type (simplified logic)
     * @param mimeType the document MIME type
     * @return processing strategy constant
     */
    public String determineProcessingStrategy(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            log.info("MIME type is null or empty, defaulting to TEXT_ONLY");
            return ApplicationConstants.PROCESSING_TEXT_ONLY;
        }

        String normalizedMimeType = mimeType.toLowerCase().trim();
        
        // Text-based documents (Word, Excel, PowerPoint, JSON, HTML, TXT, RTF)
        if (isTextBasedDocument(normalizedMimeType)) {
            log.info("Document {} identified as text-based, using TEXT_ONLY strategy", mimeType);
            return ApplicationConstants.PROCESSING_TEXT_ONLY;
        }
        
        // ALL PDFs default to TEXT_ONLY (fast path) - no complex analysis
        if (isPdfDocument(normalizedMimeType)) {
            log.info("PDF {} defaulting to TEXT_ONLY strategy (fast path)", mimeType);
            return ApplicationConstants.PROCESSING_TEXT_ONLY;
        }
        
        // Images use OCR with Vision fallback
        if (isImageDocument(normalizedMimeType)) {
            log.info("Image {} identified, using OCR_WITH_VISION_FALLBACK strategy", mimeType);
            return ApplicationConstants.PROCESSING_OCR_WITH_VISION_FALLBACK;
        }
        
        // Safe default for unknown types
        log.info("Document {} type unknown, defaulting to TEXT_ONLY strategy", mimeType);
        return ApplicationConstants.PROCESSING_TEXT_ONLY;
    }

    /**
     * Check if document is text-based (no OCR needed)
     */
    public boolean isTextBasedDocument(String mimeType) {
        return Arrays.asList(ApplicationConstants.TEXT_BASED_MIME_TYPES).contains(mimeType);
    }

    /**
     * Check if document is an image (OCR required)
     */
    public boolean isImageDocument(String mimeType) {
        return Arrays.asList(ApplicationConstants.IMAGE_MIME_TYPES).contains(mimeType);
    }

    /**
     * Check if document is PDF
     */
    public boolean isPdfDocument(String mimeType) {
        return ApplicationConstants.MIME_TYPE_PDF.equals(mimeType);
    }
}
