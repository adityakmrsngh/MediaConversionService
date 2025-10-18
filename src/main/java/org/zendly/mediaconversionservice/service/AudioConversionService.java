package org.zendly.mediaconversionservice.service;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import com.rometools.utils.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zendly.mediaconversionservice.constants.ApplicationConstants;
import org.zendly.mediaconversionservice.dto.ConversionMetadata;
import org.zendly.mediaconversionservice.dto.ConversionResponse;
import org.zendly.mediaconversionservice.dto.DocumentResponse;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Google Speech-to-Text API service for audio transcription
 */
@Slf4j
@Service
public class AudioConversionService {

    private final SpeechClient speechClient;

    @Value("${google.speech.enabled}")
    private boolean enabled;

    @Value("${google.speech.max-file-size-mb}")
    private int maxFileSizeMb;

    @Value("${google.speech.language-code}")
    private String languageCode;

    public AudioConversionService(SpeechClient speechClient) {
        this.speechClient = speechClient;
    }

    public ConversionResponse convertAudio(DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        String documentId = documentResponse.getDocumentId();
        log.info("Converting audio with Google Speech-to-Text API: {}", documentId);
        if (!enabled) {
            return buildErrorResponse(documentId, "Google Speech-to-Text API is disabled", startTime);
        }
        try {
            // Validate file size before streaming
            long maxBytes = maxFileSizeMb * 1024L * 1024L;
            URLConnection connection = new URL(documentResponse.getDownloadUrl()).openConnection();
            long fileSize = connection.getContentLengthLong();

            if (fileSize > maxBytes) {
                return buildErrorResponse(documentId, 
                        ApplicationConstants.ERROR_FILE_TOO_LARGE, startTime);
            }

            // Stream audio data directly from URL
            ByteString audioBytes;
            try (InputStream stream = connection.getInputStream()) {
                audioBytes = ByteString.readFrom(stream, (int) maxBytes);
            }

            // Detect audio encoding from MIME type
            RecognitionConfig.AudioEncoding encoding = detectAudioEncoding(
                    documentResponse.getMimeType());

            // Build Speech API request
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(encoding)
                    .setLanguageCode(languageCode)
                    .setEnableAutomaticPunctuation(true)
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            // Call Speech API
            RecognizeResponse response = speechClient.recognize(config, audio);

            if (response.getResultsCount() == 0) {
                return buildErrorResponse(documentId, "No transcription results", startTime);
            }
            
            // Extract transcription from alternatives
            StringBuilder transcription = new StringBuilder();
            response.getResultsList().forEach(result -> {
                if (result.getAlternativesCount() > 0) {
                    transcription.append(result.getAlternatives(0).getTranscript());
                }
            });
            String extractedText = transcription.toString().trim();

            ConversionMetadata metadata = ConversionMetadata.builder()
                    .originalFileName(documentResponse.getOriginalFileName())
                    .mimeType(documentResponse.getMimeType())
                    .documentType(documentResponse.getDocumentType())
                    .language(languageCode)
                    .build();

            log.info("Audio conversion completed for {}: {} chars",
                    documentId, extractedText.length());

            return ConversionResponse.builder()
                    .documentId(documentId)
                    .extractedText(extractedText)
                    .status(ApplicationConstants.CONVERSION_SUCCESS)
                    .conversionMethod(ApplicationConstants.METHOD_AUDIO_STT)
                    .metadata(metadata)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error converting audio {}: {}", documentId, e.getMessage(), e);
            return buildErrorResponse(documentId, 
                    ApplicationConstants.ERROR_CONVERSION_FAILED + ": " + e.getMessage(), startTime);
        }
    }

    private RecognitionConfig.AudioEncoding detectAudioEncoding(String mimeType) {
        return switch (mimeType) {
            case ApplicationConstants.MIME_TYPE_MP3 -> RecognitionConfig.AudioEncoding.MP3;
            case ApplicationConstants.MIME_TYPE_WAV -> RecognitionConfig.AudioEncoding.LINEAR16;
            case ApplicationConstants.MIME_TYPE_FLAC -> RecognitionConfig.AudioEncoding.FLAC;
            case ApplicationConstants.MIME_TYPE_M4A -> RecognitionConfig.AudioEncoding.MP3;
            case ApplicationConstants.MIME_TYPE_ACC -> RecognitionConfig.AudioEncoding.MP3;
            case ApplicationConstants.MIME_TYPE_OGG -> RecognitionConfig.AudioEncoding.OGG_OPUS;
            case ApplicationConstants.MIME_TYPE_AMR -> RecognitionConfig.AudioEncoding.AMR;
            default -> RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED;
        };
    }

    private ConversionResponse buildErrorResponse(String documentId, String errorMessage, long startTime) {
        return ConversionResponse.builder()
                .documentId(documentId)
                .status(ApplicationConstants.CONVERSION_FAILED)
                .errorMessage(errorMessage)
                .conversionMethod(ApplicationConstants.METHOD_AUDIO_STT)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
}
