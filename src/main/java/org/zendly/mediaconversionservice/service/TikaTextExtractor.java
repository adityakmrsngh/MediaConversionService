package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.dto.ConversionMetadata;
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Optimized service for extracting text from documents using Apache Tika with smart processing
 * Always enabled features:
 */
@Slf4j
@Service
public class TikaTextExtractor {

    @Value("${tika.ocr.write-limit}")
    private int writeLimit;

    private final AutoDetectParser autoDetectParser;
    private final ParseContext ocrEnabledContext;
    private final ParseContext textOnlyParseContext;
    private final DocumentProcessingStrategy processingStrategy;

    public TikaTextExtractor(AutoDetectParser autoDetectParser,
                             @Qualifier("parseContext") ParseContext createParseContext,
                             @Qualifier("textOnlyParseContext") ParseContext textOnlyParseContext,
                             DocumentProcessingStrategy processingStrategy) {
        this.autoDetectParser = autoDetectParser;
        this.ocrEnabledContext = createParseContext;
        this.textOnlyParseContext = textOnlyParseContext;
        this.processingStrategy = processingStrategy;
    }

    /**
     * Convert document or image using TikaTextExtractor with smart processing
     * TikaTextExtractor handles all optimization logic internally
     */
    public ConversionResponse convertWithTika(DocumentResponse documentResponse, long startTime) {
        try (InputStream inputStream = new URL(documentResponse.getDownloadUrl()).openStream()) {
            ConversionResponse tikaResponse = extractText(inputStream, documentResponse);
            if (ApplicationConstants.CONVERSION_SUCCESS.equals(tikaResponse.getStatus())) {
                log.info("Tika extraction successful for document: {} - Text length: {}, OCR used: {}",
                        documentResponse.getDocumentId(),
                        tikaResponse.getExtractedText().length(),
                        tikaResponse.getMetadata() != null ? tikaResponse.getMetadata().getUsedOcrFallback() : false);
                return tikaResponse;
            } else {
                log.error("Tika extraction failed for document: {}", documentResponse.getDocumentId());
                return tikaResponse; // Return the error response from Tika
            }

        } catch (IOException e) {
            log.error("Error downloading document {}: {}", documentResponse.getDocumentId(), e.getMessage());
            return buildErrorResponse(documentResponse.getDocumentId(),
                    ApplicationConstants.ERROR_FILE_NOT_FOUND, startTime);
        }
    }


    /**
     * Extract text from document using optimized processing with smart routing
     * Always uses the most efficient processing strategy based on content type
     */
    private ConversionResponse extractText(InputStream inputStream, DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        String mimeType = documentResponse.getMimeType();
        String strategy = processingStrategy.determineProcessingStrategy(mimeType);

        log.info("Processing document: {} (MIME: {}, Strategy: {})",
                documentResponse.getDocumentId(), mimeType, strategy);

        try {
            ConversionResponse response = switch (strategy) {
                case ApplicationConstants.PROCESSING_TEXT_ONLY ->
                    extractTextOnly(inputStream, documentResponse, startTime);
                case ApplicationConstants.PROCESSING_OCR ->
                    extractWithOcr(inputStream, documentResponse, startTime);
                default ->
                    throw new IllegalArgumentException("Unsupported processing strategy: " + strategy);
            };

            log.info("Extraction completed for document: {} - Text length: {}, Processing time: {}ms",
                    documentResponse.getDocumentId(),
                    response.getExtractedText().length(),
                    response.getProcessingTimeMs());

            return response;

        } catch (Exception e) {
            log.error("Unexpected error during extraction for document {}: {}",
                    documentResponse.getDocumentId(), e.getMessage(), e);
            return buildErrorResponse(documentResponse.getDocumentId(),
                    "Unexpected error: " + e.getMessage(), startTime);
        }
    }

    /**
     * Extract text only (no OCR) - optimized for text-based documents
     * Uses textOnlyParseContext bean for optimal performance
     */
    private ConversionResponse extractTextOnly(InputStream inputStream, DocumentResponse documentResponse, long startTime)
            throws IOException, SAXException, TikaException {

        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(writeLimit);

        autoDetectParser.parse(inputStream, handler, metadata, textOnlyParseContext);
        String extractedText = handler.toString();

        // Build simplified metadata for text-only processing
        ConversionMetadata conversionMetadata = ConversionMetadata.builder()
                .usedOcrFallback(false)
                .processingNotes("Text extraction only (no OCR)")
                .build();

        return ConversionResponse.builder()
                .documentId(documentResponse.getDocumentId())
                .extractedText(extractedText)
                .status(ApplicationConstants.CONVERSION_SUCCESS)
                .conversionMethod(ApplicationConstants.METHOD_TIKA)
                .metadata(conversionMetadata)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Extract with OCR and Google Vision fallback for images and image-based PDFs
     * Always tries Google Vision if Tika OCR confidence is below threshold
     */
    private ConversionResponse extractWithOcr(InputStream inputStream, DocumentResponse documentResponse, long startTime)
            throws IOException, SAXException, TikaException {

        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(writeLimit);

        // Try Tika OCR first
        autoDetectParser.parse(inputStream, handler, metadata, ocrEnabledContext);
        String extractedText = handler.toString();
        // Build metadata for Tika OCR result
        ConversionMetadata conversionMetadata = ConversionMetadata.builder()
                .usedOcrFallback(true)
                .processingNotes("OCR processing with Tika")
                .build();

        return ConversionResponse.builder()
                .documentId(documentResponse.getDocumentId())
                .extractedText(extractedText)
                .status(ApplicationConstants.CONVERSION_SUCCESS)
                .conversionMethod(ApplicationConstants.METHOD_TIKA)
                .metadata(conversionMetadata)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Build error response for extraction failures
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
