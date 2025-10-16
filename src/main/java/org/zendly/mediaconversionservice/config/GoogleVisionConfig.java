package org.zendly.mediaconversionservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Google Cloud Vision API
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "google.vision")
public class GoogleVisionConfig {

    /**
     * Enable/disable Google Vision API fallback
     */
    private boolean enabled = true;

    /**
     * Minimum confidence threshold to trigger Google Vision fallback (0-100)
     * If Tika OCR confidence is below this, use Google Vision
     */
    private int confidenceThreshold = 85;

    /**
     * Google Cloud Project ID
     */
    private String projectId;

    /**
     * Path to Google Cloud credentials JSON file
     */
    private String credentialsPath;

    /**
     * Maximum file size for Google Vision processing in MB
     */
    private int maxFileSizeMb = 5;

    /**
     * Request timeout in seconds
     */
    private int timeoutSeconds = 30;

    /**
     * Language hints for text detection (e.g., "en", "es")
     */
    private String[] languageHints = {"en", "es"};

    /**
     * Enable text detection features
     */
    private boolean enableTextDetection = true;

    /**
     * Enable document text detection (better for dense text)
     */
    private boolean enableDocumentTextDetection = true;
}
