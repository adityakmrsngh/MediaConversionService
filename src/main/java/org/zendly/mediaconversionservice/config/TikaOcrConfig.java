package org.zendly.mediaconversionservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Tika OCR settings
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tika.ocr")
public class TikaOcrConfig {

    /**
     * Enable/disable OCR processing
     */
    private boolean enabled = true;

    /**
     * OCR language(s) - e.g., "eng", "spa", "eng+spa"
     */
    private String language = "eng+spa";

    /**
     * Minimum confidence threshold for OCR results (0-100)
     */
    private int confidenceThreshold = 70;

    /**
     * Page segmentation mode for Tesseract
     * 1 = Automatic page segmentation with OSD
     */
    private int pageSegmentationMode = 1;

    /**
     * OCR processing timeout in seconds
     */
    private int timeoutSeconds = 120;

    /**
     * Maximum file size for OCR processing in MB
     */
    private int maxFileSizeMb = 5;

    /**
     * Enable image preprocessing (rotation detection, normalization)
     */
    private boolean enableImagePreprocessing = true;

    /**
     * DPI for rendering PDF pages to images
     */
    private int renderDpi = 300;

    /**
     * OCR strategy for PDFs
     * Options: NO_OCR, OCR_ONLY, OCR_AND_TEXT_EXTRACTION, AUTO
     */
    private String ocrStrategy = "OCR_AND_TEXT_EXTRACTION";

    /**
     * Extract inline images from PDFs for OCR
     */
    private boolean extractInlineImages = true;

    private Integer writeLimit = 100000;
}
