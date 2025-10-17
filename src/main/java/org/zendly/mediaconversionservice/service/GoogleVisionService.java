package org.zendly.mediaconversionservice.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Google Vision API service for image text extraction
 */
@Slf4j
@Service
public class GoogleVisionService {

    private final ImageAnnotatorClient visionClient;

    @Value("${google.vision.enabled}")
    private boolean enabled;

    @Value("${google.vision.max-file-size-mb}")
    private int maxFileSizeMb;

    public GoogleVisionService(ImageAnnotatorClient visionClient) {
        this.visionClient = visionClient;
    }

    public ConversionResponse convertImage(DocumentResponse documentResponse) {
        long startTime = System.currentTimeMillis();
        String documentId = documentResponse.getDocumentId();
        log.info("Converting image with Google Vision API: {}", documentId);
        if (!enabled) {
            return buildErrorResponse(documentId, "Google Vision API is disabled", startTime);
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

            // Stream image data directly from URL
            ByteString imageBytes;
            try (InputStream stream = connection.getInputStream()) {
                imageBytes = ByteString.readFrom(stream, (int) maxBytes);
            }

            // Build Vision API request
            Image image = Image.newBuilder().setContent(imageBytes).build();
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                    .build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            // Call Vision API
            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(
                    List.of(request));

            if (response.getResponsesCount() == 0) {
                return buildErrorResponse(documentId, "No response from Vision API", startTime);
            }

            AnnotateImageResponse imageResponse = response.getResponses(0);

            if (imageResponse.hasError()) {
                String error = imageResponse.getError().getMessage();
                log.error("Vision API error for {}: {}", documentId, error);
                return buildErrorResponse(documentId, error, startTime);
            }

            // Extract text and confidence
            String extractedText = imageResponse.hasFullTextAnnotation() 
                    ? imageResponse.getFullTextAnnotation().getText() 
                    : "";

            ConversionMetadata metadata = ConversionMetadata.builder()
                    .originalFileName(documentResponse.getOriginalFileName())
                    .mimeType(documentResponse.getMimeType())
                    .documentType(documentResponse.getDocumentType())
                    .usedOcrFallback(true)
                    .build();

            log.info("Image conversion completed for {}: {} chars",
                    documentId, extractedText.length());

            return ConversionResponse.builder()
                    .documentId(documentId)
                    .extractedText(extractedText)
                    .status(ApplicationConstants.CONVERSION_SUCCESS)
                    .conversionMethod(ApplicationConstants.METHOD_IMAGE_VISION)
                    .metadata(metadata)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error converting image {}: {}", documentId, e.getMessage(), e);
            return buildErrorResponse(documentId, 
                    ApplicationConstants.ERROR_CONVERSION_FAILED + ": " + e.getMessage(), startTime);
        }
    }

    private ConversionResponse buildErrorResponse(String documentId, String errorMessage, long startTime) {
        return ConversionResponse.builder()
                .documentId(documentId)
                .status(ApplicationConstants.CONVERSION_FAILED)
                .errorMessage(errorMessage)
                .conversionMethod(ApplicationConstants.METHOD_IMAGE_VISION)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
}
