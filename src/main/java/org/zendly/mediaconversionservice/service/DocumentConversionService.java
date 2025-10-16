package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Simplified main service for orchestrating document conversion
 * Always enabled optimizations - routes all document/image processing to TikaTextExtractor
 * Audio processing handled separately by AudioConversionService
 */
@Slf4j
@Service
public class DocumentConversionService {

    private final TikaTextExtractor tikaTextExtractor;
    private final AudioConversionService audioConversionService;

    @Value("${tika.file.max-size-mb}")
    private int maxFileSizeMb;

    public DocumentConversionService(TikaTextExtractor tikaTextExtractor,
                                   AudioConversionService audioConversionService) {
        this.tikaTextExtractor = tikaTextExtractor;
        this.audioConversionService = audioConversionService;
    }

    /**
     * Convert document to text based on document type
     * Simplified routing - TikaTextExtractor handles all document/image processing with smart optimization
     */
    public ConversionResponse convertDocument(DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting conversion for document: {} with MIME type: {}", 
                documentResponse.getDocumentId(), documentResponse.getMimeType());

        try {
            // Validate file size
            if (!isFileSizeValid(documentResponse.getDownloadUrl())) {
                return buildErrorResponse(documentResponse.getDocumentId(), 
                        ApplicationConstants.ERROR_FILE_TOO_LARGE, startTime);
            }

            // Simple, direct routing based on document type
            ConversionResponse response = switch (documentResponse.getDocumentType()) {
                case AUDIO -> audioConversionService.convertAudio(documentResponse);
                case DOCUMENT, IMAGE -> tikaTextExtractor.convertWithTika(documentResponse, startTime);
                default -> buildErrorResponse(documentResponse.getDocumentId(), 
                        ApplicationConstants.ERROR_UNSUPPORTED_FORMAT, startTime);
            };

            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return response;

        } catch (Exception e) {
            log.error("Error converting document {}: {}", documentResponse.getDocumentId(), e.getMessage(), e);
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    ApplicationConstants.ERROR_CONVERSION_FAILED + ": " + e.getMessage(), startTime);
        }
    }

    /**
     * Validate file size against configured maximum
     */
    private boolean isFileSizeValid(String downloadUrl) {
        try {
            URL url = new URL(downloadUrl);
            long contentLength = url.openConnection().getContentLengthLong();
            long maxSizeBytes = maxFileSizeMb * 1024L * 1024L;

            return contentLength <= maxSizeBytes;
        } catch (IOException e) {
            log.warn("Could not determine file size for URL: {}", downloadUrl);
            return true; // Allow processing if size cannot be determined
        }
    }

    /**
     * Build error response with consistent structure
     */
    private ConversionResponse buildErrorResponse(String documentId, String errorMessage, long startTime) {
        return ConversionResponse.builder()
                .documentId(documentId)
                .status(ApplicationConstants.CONVERSION_FAILED)
                .errorMessage(errorMessage)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
}
