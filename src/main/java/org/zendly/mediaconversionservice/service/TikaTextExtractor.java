package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.dto.ConversionMetadata;
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for extracting text from documents using Apache Tika
 */
@Slf4j
@Service
public class TikaTextExtractor {

    private final Tika tika;
    private final AutoDetectParser autoDetectParser;
    private final ParseContext parseContext;

    @Value("${conversion.tika.max-string-length}")
    private int tikaMaxStringLength;
    
    @Value("${conversion.logging.enabled}")
    private boolean loggingEnabled;

    public TikaTextExtractor(Tika tika, 
                           AutoDetectParser autoDetectParser, 
                           ParseContext parseContext) {
        this.tika = tika;
        this.autoDetectParser = autoDetectParser;
        this.parseContext = parseContext;
    }

    /**
     * Extract text from document using Apache Tika
     */
    public ConversionResponse extractText(InputStream inputStream, DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        
        if (loggingEnabled) {
            log.info("Starting Tika text extraction for document: {}", documentResponse.getDocumentId());
        }

        try {
            // Use Tika with metadata extraction
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler(tikaMaxStringLength);
            
            // Parse document
            autoDetectParser.parse(inputStream, handler, metadata, parseContext);
            String extractedText = handler.toString();

            if (loggingEnabled) {
                log.info("Tika extraction completed for document: {} - Text length: {}", 
                        documentResponse.getDocumentId(), extractedText.length());
            }

            // Build conversion metadata
            ConversionMetadata conversionMetadata = ConversionMetadata.builder()
                    .originalFileName(documentResponse.getOriginalFileName())
                    .mimeType(documentResponse.getMimeType())
                    .documentType(documentResponse.getDocumentType())
                    .fileSizeBytes(getFileSizeFromMetadata(metadata))
                    .pageCount(getPageCountFromMetadata(metadata))
                    .usedOcrFallback(false)
                    .processingNotes("Extracted using Apache Tika")
                    .build();

            return ConversionResponse.builder()
                    .documentId(documentResponse.getDocumentId())
                    .extractedText(extractedText)
                    .status(ApplicationConstants.CONVERSION_SUCCESS)
                    .conversionMethod(ApplicationConstants.METHOD_TIKA)
                    .metadata(conversionMetadata)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (IOException e) {
            log.error("IO error during Tika extraction for document {}: {}", 
                    documentResponse.getDocumentId(), e.getMessage());
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    "IO error: " + e.getMessage(), startTime);
                    
        } catch (SAXException e) {
            log.error("SAX parsing error during Tika extraction for document {}: {}", 
                    documentResponse.getDocumentId(), e.getMessage());
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    "Parsing error: " + e.getMessage(), startTime);
                    
        } catch (TikaException e) {
            log.error("Tika error during extraction for document {}: {}", 
                    documentResponse.getDocumentId(), e.getMessage());
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    "Tika error: " + e.getMessage(), startTime);
                    
        } catch (Exception e) {
            log.error("Unexpected error during Tika extraction for document {}: {}", 
                    documentResponse.getDocumentId(), e.getMessage(), e);
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    "Unexpected error: " + e.getMessage(), startTime);
        }
    }

    /**
     * Extract file size from Tika metadata
     */
    private Long getFileSizeFromMetadata(Metadata metadata) {
        try {
            String contentLength = metadata.get(Metadata.CONTENT_LENGTH);
            return contentLength != null ? Long.parseLong(contentLength) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extract page count from Tika metadata
     */
    private Integer getPageCountFromMetadata(Metadata metadata) {
        try {
            String pageCount = metadata.get("xmpTPg:NPages");
            if (pageCount == null) {
                pageCount = metadata.get("meta:page-count");
            }
            return pageCount != null ? Integer.parseInt(pageCount) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Build error response for Tika extraction failures
     */
    private ConversionResponse buildErrorResponse(String documentId, String errorMessage, long startTime) {
        return ConversionResponse.builder()
                .documentId(documentId)
                .status(ApplicationConstants.CONVERSION_FAILED)
                .conversionMethod(ApplicationConstants.METHOD_TIKA)
                .errorMessage(errorMessage)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
}
