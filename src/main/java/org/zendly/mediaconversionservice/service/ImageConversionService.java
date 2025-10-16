package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.dto.ConversionMetadata;
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

/**
 * Service for converting images to text using Tika OCR
 * Google Vision fallback can be added later when dependency is resolved
 */
@Slf4j
@Service
public class ImageConversionService {

    private final TikaTextExtractor tikaTextExtractor;

    // Supported image MIME types
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            ApplicationConstants.MIME_TYPE_JPEG,
            ApplicationConstants.MIME_TYPE_PNG,
            ApplicationConstants.MIME_TYPE_TIFF,
            ApplicationConstants.MIME_TYPE_BMP
    );

    public ImageConversionService(TikaTextExtractor tikaTextExtractor) {
        this.tikaTextExtractor = tikaTextExtractor;
    }

    /**
     * Convert image to text using Tika OCR
     */
    public ConversionResponse convertImage(DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting image conversion for document: {} with MIME type: {}", 
                documentResponse.getDocumentId(), documentResponse.getMimeType());

        // Validate image format
        if (!isSupportedImageFormat(documentResponse.getMimeType())) {
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    "Unsupported image format: " + documentResponse.getMimeType(), startTime);
        }

        try {
            // Use Tika OCR for image text extraction
            ConversionResponse tikaResponse = extractWithTika(documentResponse);
            
            if (ApplicationConstants.CONVERSION_SUCCESS.equals(tikaResponse.getStatus())) {
                int tikaConfidence = tikaResponse.getMetadata() != null ? 
                        tikaResponse.getMetadata().getOcrConfidence() : 0;
                
                log.info("Tika OCR completed for image: {} - Text length: {}, Confidence: {}", 
                        documentResponse.getDocumentId(), 
                        tikaResponse.getExtractedText().length(), 
                        tikaConfidence);

                return tikaResponse;
            } else {
                log.error("Tika OCR failed for document: {}", documentResponse.getDocumentId());
                return tikaResponse;
            }

        } catch (Exception e) {
            log.error("Error during image conversion for document {}: {}", 
                    documentResponse.getDocumentId(), e.getMessage(), e);
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    "Image conversion failed: " + e.getMessage(), startTime);
        }
    }

    /**
     * Extract text using Tika OCR
     */
    private ConversionResponse extractWithTika(DocumentResponse documentResponse) {
        try (InputStream inputStream = new URL(documentResponse.getDownloadUrl()).openStream()) {
            return tikaTextExtractor.extractText(inputStream, documentResponse);
        } catch (IOException e) {
            log.error("Error downloading image for Tika OCR: {}", e.getMessage());
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    "Failed to download image for Tika OCR: " + e.getMessage(), System.currentTimeMillis());
        }
    }

    /**
     * Check if image format is supported
     */
    private boolean isSupportedImageFormat(String mimeType) {
        return SUPPORTED_IMAGE_TYPES.contains(mimeType);
    }

    /**
     * Build error response for image conversion failures
     */
    private ConversionResponse buildErrorResponse(String documentId, String errorMessage, long startTime) {
        return ConversionResponse.builder()
                .documentId(documentId)
                .status(ApplicationConstants.CONVERSION_FAILED)
                .conversionMethod(ApplicationConstants.METHOD_IMAGE_VISION)
                .errorMessage(errorMessage)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
}
