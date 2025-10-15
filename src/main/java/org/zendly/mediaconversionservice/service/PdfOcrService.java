package org.zendly.mediaconversionservice.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.dto.ConversionMetadata;
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for extracting text from scanned PDFs using PDFBox + Tess4J OCR
 */
@Slf4j
@Service
public class PdfOcrService {

    private final File tempDirectory;

    @Value("${conversion.ocr.languages}")
    private String ocrLanguages;
    
    @Value("${conversion.ocr.confidence-threshold}")
    private int ocrConfidenceThreshold;
    
    @Value("${conversion.ocr.page-segmentation-mode}")
    private int pageSegmentationMode;
    
    @Value("${conversion.pdfbox.dpi}")
    private int pdfboxDpi;
    
    @Value("${conversion.pdfbox.image-type}")
    private String pdfboxImageType;
    
    @Value("${conversion.logging.enabled}")
    private boolean loggingEnabled;

    public PdfOcrService(File tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    /**
     * Extract text from PDF using OCR (PDFBox + Tess4J)
     */
    public ConversionResponse extractTextWithOcr(InputStream inputStream, DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        
        if (loggingEnabled) {
            log.info("Starting OCR extraction for document: {}", documentResponse.getDocumentId());
        }

        try {
            // Load PDF document
            PDDocument document = PDDocument.load(inputStream);
            
            if (loggingEnabled) {
                log.info("PDF loaded for OCR - Pages: {}, Document: {}", 
                        document.getNumberOfPages(), documentResponse.getDocumentId());
            }

            // Configure Tesseract
            Tesseract tesseract = configureTesseract();
            
            // Render PDF pages to images and perform OCR
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            List<String> pageTexts = new ArrayList<>();
            int totalConfidence = 0;
            int pageCount = document.getNumberOfPages();

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                try {
                    // Render page to image
                    BufferedImage image = pdfRenderer.renderImageWithDPI(
                            pageIndex, 
                            pdfboxDpi, 
                            getImageType()
                    );

                    // Perform OCR on the image
                    String pageText = tesseract.doOCR(image);
                    pageTexts.add(pageText);

                    // Get confidence score for this page
                    int confidence = getOcrConfidence(tesseract, image);
                    totalConfidence += confidence;

                    if (loggingEnabled) {
                        log.debug("OCR completed for page {} of document {} - Text length: {}, Confidence: {}", 
                                pageIndex + 1, documentResponse.getDocumentId(), pageText.length(), confidence);
                    }

                } catch (TesseractException e) {
                    log.warn("OCR failed for page {} of document {}: {}", 
                            pageIndex + 1, documentResponse.getDocumentId(), e.getMessage());
                    pageTexts.add(""); // Add empty text for failed pages
                }
            }

            document.close();

            // Combine all page texts
            String extractedText = String.join("\n\n", pageTexts);
            int averageConfidence = pageCount > 0 ? totalConfidence / pageCount : 0;

            if (loggingEnabled) {
                log.info("OCR extraction completed for document: {} - Total text length: {}, Average confidence: {}", 
                        documentResponse.getDocumentId(), extractedText.length(), averageConfidence);
            }

            // Build conversion metadata
            ConversionMetadata conversionMetadata = ConversionMetadata.builder()
                    .originalFileName(documentResponse.getOriginalFileName())
                    .mimeType(documentResponse.getMimeType())
                    .documentType(documentResponse.getDocumentType())
                    .pageCount(pageCount)
                    .ocrConfidence(averageConfidence)
                    .language(ocrLanguages)
                    .usedOcrFallback(true)
                    .processingNotes("Extracted using PDFBox + Tess4J OCR")
                    .build();

            return ConversionResponse.builder()
                    .documentId(documentResponse.getDocumentId())
                    .extractedText(extractedText)
                    .status(ApplicationConstants.CONVERSION_SUCCESS)
                    .conversionMethod(ApplicationConstants.METHOD_OCR)
                    .metadata(conversionMetadata)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (IOException e) {
            log.error("IO error during OCR extraction for document {}: {}", 
                    documentResponse.getDocumentId(), e.getMessage());
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    "IO error: " + e.getMessage(), startTime);
                    
        } catch (Exception e) {
            log.error("Unexpected error during OCR extraction for document {}: {}", 
                    documentResponse.getDocumentId(), e.getMessage(), e);
            return buildErrorResponse(documentResponse.getDocumentId(), 
                    "OCR error: " + e.getMessage(), startTime);
        }
    }

    /**
     * Configure Tesseract OCR engine
     */
    private Tesseract configureTesseract() {
        Tesseract tesseract = new Tesseract();
        
        // Set language (English + Spanish)
        tesseract.setLanguage(ocrLanguages);
        
        // Set page segmentation mode
        tesseract.setPageSegMode(pageSegmentationMode);
        
        // Set OCR Engine Mode (LSTM only for better accuracy)
        tesseract.setOcrEngineMode(1);
        
        if (loggingEnabled) {
            log.info("Tesseract configured - Languages: {}, PSM: {}", 
                    ocrLanguages, pageSegmentationMode);
        }
        
        return tesseract;
    }

    /**
     * Get ImageType from configuration
     */
    private ImageType getImageType() {
        return switch (pdfboxImageType.toLowerCase()) {
            case "rgb" -> ImageType.RGB;
            case "argb" -> ImageType.ARGB;
            case "gray" -> ImageType.GRAY;
            case "binary" -> ImageType.BINARY;
            default -> ImageType.RGB; // Default to RGB
        };
    }

    /**
     * Get OCR confidence score for an image
     */
    private int getOcrConfidence(Tesseract tesseract, BufferedImage image) {
        try {
            // Tesseract 4.x doesn't directly provide confidence in doOCR
            // This is a simplified approach - in production you might want to use
            // more sophisticated confidence calculation
            String text = tesseract.doOCR(image);
            
            // Simple heuristic: longer text with more alphanumeric characters = higher confidence
            if (text == null || text.trim().isEmpty()) {
                return 0;
            }
            
            long alphanumericCount = text.chars()
                    .filter(Character::isLetterOrDigit)
                    .count();
            
            double ratio = (double) alphanumericCount / text.length();
            return Math.min(100, (int) (ratio * 100));
            
        } catch (TesseractException e) {
            return 0;
        }
    }

    /**
     * Build error response for OCR extraction failures
     */
    private ConversionResponse buildErrorResponse(String documentId, String errorMessage, long startTime) {
        return ConversionResponse.builder()
                .documentId(documentId)
                .status(ApplicationConstants.CONVERSION_FAILED)
                .conversionMethod(ApplicationConstants.METHOD_OCR)
                .errorMessage(errorMessage)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
}
