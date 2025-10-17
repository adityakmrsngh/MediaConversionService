package org.zendly.mediaconversionservice.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Configuration for Google Cloud Speech-to-Text API
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "google.speech")
public class GoogleSpeechConfig {

    private boolean enabled = true;
    private String credentialsPath;
    private int maxFileSizeMb = 5;
    private int timeoutSeconds = 300;
    private String languageCode = "en-US";

    private SpeechClient speechClient;

    @Bean
    @ConditionalOnProperty(name = "google.speech.enabled", havingValue = "true")
    public SpeechClient speechClient() throws IOException {
        try {
            SpeechSettings.Builder settingsBuilder = SpeechSettings.newBuilder();

            // Try credentials path first, fallback to Application Default Credentials
            if (credentialsPath != null && !credentialsPath.isEmpty()) {
                try {
                    ServiceAccountCredentials credentials = ServiceAccountCredentials
                            .fromStream(new FileInputStream(credentialsPath));
                    settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials));
                    log.info("Google Speech-to-Text API initialized with credentials from: {}", credentialsPath);
                } catch (IOException e) {
                    log.warn("Failed to load credentials from path, using Application Default Credentials");
                }
            } else {
                log.info("Google Speech-to-Text API initialized with Application Default Credentials");
            }

            this.speechClient = SpeechClient.create(settingsBuilder.build());
            return this.speechClient;
        } catch (IOException e) {
            log.error("Failed to initialize Google Speech-to-Text API client: {}", e.getMessage());
            throw e;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (speechClient != null) {
            try {
                speechClient.close();
                log.info("Google Speech-to-Text API client closed successfully");
            } catch (Exception e) {
                log.error("Error closing Google Speech-to-Text API client: {}", e.getMessage());
            }
        }
    }
}
