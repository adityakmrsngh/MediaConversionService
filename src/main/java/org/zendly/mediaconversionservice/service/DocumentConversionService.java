package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.dto.ConversionMetadata;
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;
import org.zendly.mediaconversionservice.dto.DocumentType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

/**
 * Main service for orchestrating document conversion
 * Implements the conversion flow: Tika first → OCR fallback if needed
 */
@Slf4j
@Service
public class DocumentConversionService {

    private final TikaTextExtractor tikaTextExtractor;
    private final PdfOcrService pdfOcrService;
    private final AudioConversionService audioConversionService;
    private final ImageConversionService imageConversionService;

    @Value("${conversion.tika.enabled}")
    private boolean tikaEnabled;
    
    @Value("${conversion.ocr.enabled}")
    private boolean ocrEnabled;
    
    @Value("${conversion.file.max-size-mb}")
    private int maxFileSizeMb;
    
    @Value("${conversion.logging.enabled}")
    private boolean loggingEnabled;

    // Supported document MIME types for Tika
    private static final Set<String> TIKA_SUPPORTED_TYPES = Set.of(
            ApplicationConstants.MIME_TYPE_PDF,
            ApplicationConstants.MIME_TYPE_DOC,
            ApplicationConstants.MIME_TYPE_DOCX,
            ApplicationConstants.MIME_TYPE_XLS,
            ApplicationConstants.MIME_TYPE_XLSX,
            ApplicationConstants.MIME_TYPE_PPT,
            ApplicationConstants.MIME_TYPE_PPTX,
            ApplicationConstants.MIME_TYPE_TXT,
            ApplicationConstants.MIME_TYPE_RTF
    );

    // Audio MIME types
    private static final Set<String> AUDIO_TYPES = Set.of(
            ApplicationConstants.MIME_TYPE_MP3,
            ApplicationConstants.MIME_TYPE_WAV,
            ApplicationConstants.MIME_TYPE_M4A,
            ApplicationConstants.MIME_TYPE_FLAC
    );

    // Image MIME types
    private static final Set<String> IMAGE_TYPES = Set.of(
            ApplicationConstants.MIME_TYPE_JPEG,
            ApplicationConstants.MIME_TYPE_PNG,
            ApplicationConstants.MIME_TYPE_TIFF,
            ApplicationConstants.MIME_TYPE_BMP
    );

    public DocumentConversionService(TikaTextExtractor tikaTextExtractor,
                                   PdfOcrService pdfOcrService,
                                   AudioConversionService audioConversionService,
                                   ImageConversionService imageConversionService) {
        this.tikaTextExtractor = tikaTextExtractor;
        this.pdfOcrService = pdfOcrService;
        this.audioConversionService = audioConversionService;
        this.imageConversionService = imageConversionService;
    }

    /**
     * Convert document to text based on document type and MIME type
     * Implements the conversion strategy: Tika first → OCR fallback for PDFs
     */
    public ConversionResponse convertDocument(DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        
        if (loggingEnabled) {
            log.info("Starting conversion for document: {} with MIME type: {}", 
                    documentResponse.getDocumentId(), documentResponse.getMimeType());
        }

        try {
            // Validate file size
            if (!isFileSizeValid(documentResponse.getDownloadUrl())) {
                return buildErrorResponse(documentResponse.getDocumentId(), 
                        ApplicationConstants.ERROR_FILE_TOO_LARGE, startTime);
            }

            // Route to appropriate conversion service based on document type
            ConversionResponse response = switch (documentResponse.getDocumentType()) {
                case DOCUMENT -> convertDocumentType(documentResponse, startTime);
                case AUDIO -> audioConversionService.convertAudio(documentResponse);
                case IMAGE -> imageConversionService.convertImage(documentResponse);
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
     * Convert document type files using Tika first, then OCR fallback for PDFs
     */
    private ConversionResponse convertDocumentType(DocumentResponse documentResponse, long startTime) {
        String mimeType = documentResponse.getMimeType();
        
        if (!TIKA_SUPPORTED_TYPES.contains(mimeType)) {
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    ApplicationConstants.ERROR_UNSUPPORTED_FORMAT, startTime);
        }

        try (InputStream inputStream = new URL(documentResponse.getDownloadUrl()).openStream()) {
            
            // Step 1: Try Apache Tika first
            if (tikaEnabled) {
                ConversionResponse tikaResponse = tikaTextExtractor.extractText(inputStream, documentResponse);
                
                // Check if Tika extraction was successful and has meaningful content
                if (ApplicationConstants.CONVERSION_SUCCESS.equals(tikaResponse.getStatus()) && 
                    hasSignificantContent(tikaResponse.getExtractedText())) {
                    if (loggingEnabled) {
                        log.info("Tika extraction successful for document: {}", documentResponse.getDocumentId());
                    }
                    return tikaResponse;
                }
            }

            // Step 2: For PDFs with empty/minimal text, try OCR fallback
            if (ApplicationConstants.MIME_TYPE_PDF.equals(mimeType) && ocrEnabled) {
                if (loggingEnabled) {
                    log.info("Falling back to OCR for document: {}", documentResponse.getDocumentId());
                }
                
                // Reopen stream for OCR processing
                try (InputStream ocrInputStream = new URL(documentResponse.getDownloadUrl()).openStream()) {
                    ConversionResponse ocrResponse = pdfOcrService.extractTextWithOcr(ocrInputStream, documentResponse);
                    
                    // Mark that OCR fallback was used
                    if (ocrResponse.getMetadata() != null) {
                        ocrResponse.getMetadata().setUsedOcrFallback(true);
                    }
                    
                    return ocrResponse;
                }
            }

            // If we reach here, no successful conversion was possible
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    ApplicationConstants.ERROR_EMPTY_CONTENT, startTime);

        } catch (IOException e) {
            log.error("Error downloading document {}: {}", documentResponse.getDocumentId(), e.getMessage());
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    ApplicationConstants.ERROR_FILE_NOT_FOUND, startTime);
        }
    }

    /**
     * Check if extracted text has significant content (not just whitespace or minimal text)
     */
    private boolean hasSignificantContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        // Consider text significant if it has more than 10 non-whitespace characters
        String cleanText = text.replaceAll("\\s+", "");
        return cleanText.length() > 10;
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
