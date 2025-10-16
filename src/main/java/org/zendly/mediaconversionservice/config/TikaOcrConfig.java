package org.zendly.mediaconversionservice.config;

import lombok.Data;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
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

    @Bean
    public AutoDetectParser getAutoDetectParser() {
        return new AutoDetectParser();
    }

    @Bean
    public ParseContext createParseContext(PDFParserConfig pdfParserConfig,
                                           TesseractOCRConfig tesseractOCRConfig) {
        ParseContext context = new ParseContext();
        context.set(TesseractOCRConfig.class, tesseractOCRConfig);
        context.set(PDFParserConfig.class, pdfParserConfig);
        return context;
    }

    @Bean
    public TesseractOCRConfig createTesseractOCRConfig() {
        TesseractOCRConfig config = new TesseractOCRConfig();

        if (isEnabled()) {
            config.setLanguage(getLanguage());
            config.setPageSegMode(String.valueOf(getPageSegmentationMode()));
            config.setTimeoutSeconds(getTimeoutSeconds());
            config.setMaxFileSizeToOcr(getMaxFileSizeMb() * 1024L * 1024L);
            config.setDensity(getRenderDpi());
            config.setEnableImagePreprocessing(isEnableImagePreprocessing());
        } else {
            config.setSkipOcr(true);
        }

        return config;
    }

    /**
     * Create PDFParserConfig from application properties
     */
    @Bean
    public PDFParserConfig createPDFParserConfig() {
        PDFParserConfig config = new PDFParserConfig();

        // Set OCR strategy based on configuration
        switch (getOcrStrategy().toUpperCase()) {
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

        config.setExtractInlineImages(isExtractInlineImages());
        config.setOcrDPI(getRenderDpi());

        return config;
    }

    @Bean
    public BodyContentHandler getBodyContentHandler() {
        return new BodyContentHandler(getWriteLimit()); // 100KB limit
    }
}
