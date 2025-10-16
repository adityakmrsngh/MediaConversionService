package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import org.zendly.mediaconversionservice.config.TikaOcrConfig;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.dto.ConversionMetadata;
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for extracting text from documents using Apache Tika 3.0 with OCR support
 */
@Slf4j
@Service
public class TikaTextExtractor {

    private final TikaOcrConfig tikaOcrConfig;
    private final AutoDetectParser autoDetectParser;
    private final ParseContext parseContext;
    private final TesseractOCRConfig tesseractOCRConfig;
    private final PDFParserConfig pdfParserConfig;

    public TikaTextExtractor(TikaOcrConfig tikaOcrConfig) {
        this.tikaOcrConfig = tikaOcrConfig;
        this.autoDetectParser = new AutoDetectParser();
        this.parseContext = createParseContext();
        this.tesseractOCRConfig = createTesseractOCRConfig();
        this.pdfParserConfig = createPDFParserConfig();
    }

    /**
     * Extract text from document using Apache Tika 3.0 with OCR support
     */
    public ConversionResponse extractText(InputStream inputStream, DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting Tika 3.0 text extraction for document: {}", documentResponse.getDocumentId());

        try {
            // Use Tika with metadata extraction and OCR configuration
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler(tikaOcrConfig.getWriteLimit()); // 100KB limit
            
            // Parse document with OCR-enabled context
            autoDetectParser.parse(inputStream, handler, metadata, parseContext);
            String extractedText = handler.toString();

            // Calculate OCR confidence if OCR was used
            int ocrConfidence = calculateOcrConfidence(metadata, extractedText);
            boolean usedOcr = wasOcrUsed(metadata);

            log.info("Tika extraction completed for document: {} - Text length: {}, OCR used: {}, Confidence: {}", 
                    documentResponse.getDocumentId(), extractedText.length(), usedOcr, ocrConfidence);

            // Build conversion metadata
            ConversionMetadata conversionMetadata = ConversionMetadata.builder()
                    .originalFileName(documentResponse.getOriginalFileName())
                    .mimeType(documentResponse.getMimeType())
                    .documentType(documentResponse.getDocumentType())
                    .fileSizeBytes(getFileSizeFromMetadata(metadata))
                    .pageCount(getPageCountFromMetadata(metadata))
                    .ocrConfidence(ocrConfidence)
                    .language(tikaOcrConfig.getLanguage())
                    .usedOcrFallback(usedOcr)
                    .processingNotes("Extracted using Apache Tika 3.0 with OCR support")
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
     * Create ParseContext with OCR and PDF configurations
     */
    private ParseContext createParseContext() {
        ParseContext context = new ParseContext();
        context.set(TesseractOCRConfig.class, tesseractOCRConfig);
        context.set(PDFParserConfig.class, pdfParserConfig);
        return context;
    }

    /**
     * Create TesseractOCRConfig from application properties
     */
    private TesseractOCRConfig createTesseractOCRConfig() {
        TesseractOCRConfig config = new TesseractOCRConfig();
        
        if (tikaOcrConfig.isEnabled()) {
            config.setLanguage(tikaOcrConfig.getLanguage());
            config.setPageSegMode(String.valueOf(tikaOcrConfig.getPageSegmentationMode()));
            config.setTimeoutSeconds(tikaOcrConfig.getTimeoutSeconds());
            config.setMaxFileSizeToOcr(tikaOcrConfig.getMaxFileSizeMb() * 1024L * 1024L);
            config.setDensity(tikaOcrConfig.getRenderDpi());
            config.setEnableImagePreprocessing(tikaOcrConfig.isEnableImagePreprocessing());
        } else {
            config.setSkipOcr(true);
        }
        
        return config;
    }

    /**
     * Create PDFParserConfig from application properties
     */
    private PDFParserConfig createPDFParserConfig() {
        PDFParserConfig config = new PDFParserConfig();
        
        // Set OCR strategy based on configuration
        switch (tikaOcrConfig.getOcrStrategy().toUpperCase()) {
            case "NO_OCR":
                config.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
                break;
            case "OCR_ONLY":
                config.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_ONLY);
                break;
            case "OCR_AND_TEXT_EXTRACTION":
                config.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION);
                break;
            case "AUTO":
                config.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.AUTO);
                break;
            default:
                config.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION);
        }
        
        config.setExtractInlineImages(tikaOcrConfig.isExtractInlineImages());
        config.setOcrDPI(tikaOcrConfig.getRenderDpi());
        
        return config;
    }

    /**
     * Calculate OCR confidence from metadata and extracted text
     */
    private int calculateOcrConfidence(Metadata metadata, String extractedText) {
        // Check if OCR confidence is available in metadata
        String ocrConfidence = metadata.get("ocr:confidence");
        if (ocrConfidence != null) {
            try {
                return Integer.parseInt(ocrConfidence);
            } catch (NumberFormatException e) {
                // Fall through to heuristic calculation
            }
        }

        // Heuristic confidence calculation based on text characteristics
        if (extractedText == null || extractedText.trim().isEmpty()) {
            return 0;
        }

        // Simple heuristic: ratio of alphanumeric characters to total characters
        long alphanumericCount = extractedText.chars()
                .filter(Character::isLetterOrDigit)
                .count();
        
        if (extractedText.length() == 0) {
            return 0;
        }
        
        double ratio = (double) alphanumericCount / extractedText.length();
        int confidence = Math.min(100, (int) (ratio * 100));
        
        // Boost confidence if text seems well-structured
        if (extractedText.contains(" ") && extractedText.length() > 50) {
            confidence = Math.min(100, confidence + 10);
        }
        
        return Math.max(confidence, tikaOcrConfig.getConfidenceThreshold());
    }

    /**
     * Check if OCR was used based on metadata
     */
    private boolean wasOcrUsed(Metadata metadata) {
        // Check for OCR-related metadata
        String ocrUsed = metadata.get("ocr:used");
        if ("true".equalsIgnoreCase(ocrUsed)) {
            return true;
        }

        // Check for Tesseract-specific metadata
        String tesseractVersion = metadata.get("tesseract:version");
        if (tesseractVersion != null) {
            return true;
        }

        // Check for OCR confidence metadata (indicates OCR was attempted)
        String ocrConfidence = metadata.get("ocr:confidence");
        return ocrConfidence != null;
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
