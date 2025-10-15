package org.zendly.mediaconversionservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.tika.Tika;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;

import java.io.File;

/**
 * Configuration for document conversion services
 */
@Configuration
@Slf4j
public class ConversionServiceConfig {

    @Value("${conversion.tika.max-string-length}")
    private int tikaMaxStringLength;
    
    @Value("${conversion.file.temp-dir}")
    private String tempDir;
    
    @Value("${conversion.logging.enabled}")
    private boolean loggingEnabled;

    /**
     * Apache Tika bean for document text extraction
     */
    @Bean
    public Tika tika() {
        if (loggingEnabled) {
            log.info("Configuring Apache Tika - Max string length: {}", tikaMaxStringLength);
        }
        
        Tika tika = new Tika();
        tika.setMaxStringLength(tikaMaxStringLength);
        return tika;
    }

    /**
     * Tika AutoDetectParser for advanced parsing
     */
    @Bean
    public AutoDetectParser autoDetectParser() {
        return new AutoDetectParser();
    }

    /**
     * Parse context for Tika operations
     */
    @Bean
    public ParseContext parseContext() {
        return new ParseContext();
    }

    /**
     * Ensure temp directory exists for file processing
     */
    @Bean
    public File tempDirectory() {
        File tempDirectory = new File(tempDir);
        if (!tempDirectory.exists()) {
            boolean created = tempDirectory.mkdirs();
            if (loggingEnabled) {
                log.info("Temp directory created: {} - Success: {}", tempDir, created);
            }
        }
        return tempDirectory;
    }
}
