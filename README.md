# MediaConversionService

A Spring Boot service that implements a single responsibility: **take a media input → produce structured text output**.

## Overview

The MediaConversionService converts various media types to structured text using different extraction strategies:

- **Documents** (PDF/Word/Excel): Apache Tika → PDFBox + Tess4J OCR fallback
- **Audio** (MP3/WAV/FLAC): Google Speech-to-Text API (stub implementation)
- **Images** (JPEG/PNG/TIFF): Google Vision API (stub implementation)

## Architecture

### Core Components

1. **DocumentConverterController** - REST endpoint for document conversion
2. **DocumentConversionService** - Main orchestrator implementing conversion strategy
3. **TikaTextExtractor** - Apache Tika integration for document text extraction
4. **PdfOcrService** - PDFBox + Tess4J OCR for scanned PDFs
5. **AudioConversionService** - Stub for Google Speech-to-Text integration
6. **ImageConversionService** - Stub for Google Vision API integration

### Conversion Flow

```
Document Request → Get Metadata → Download from GCP → Convert Based on Type → Return Structured Text
```

#### Document Conversion Strategy:
1. **Try Apache Tika first** - Fast text extraction for text-based documents
2. **If text is empty/minimal** → **Fallback to OCR** (PDFBox + Tess4J) for scanned PDFs
3. **Return structured response** with metadata

## Features

### Multi-Language OCR Support
- **English + Spanish** OCR using Tess4J
- Configurable via `conversion.ocr.languages: "eng+spa"`
- Confidence scoring and language detection

### Cloud Run Optimized
- **Self-contained deployment** - No system dependencies needed
- **Multi-stage Docker build** for optimized container size
- **Health checks** via Spring Actuator
- **Structured logging** for Google Cloud Logging
- **Memory optimization** for Cloud Run constraints

### Configuration-Driven
- All settings configurable via `application.yml`
- Cloud Run specific settings in `application-cloudrun.yml`
- No hardcoded values - follows established patterns

## API Endpoint

### Convert Document
```http
GET /api/documents/{documentId}
Headers:
  X-Tenant-ID: {tenantId}
```

**Response:**
```json
{
  "documentId": "doc123",
  "extractedText": "Extracted text content...",
  "status": "CONVERSION_SUCCESS",
  "conversionMethod": "APACHE_TIKA",
  "metadata": {
    "originalFileName": "document.pdf",
    "mimeType": "application/pdf",
    "documentType": "DOCUMENT",
    "pageCount": 5,
    "ocrConfidence": 95,
    "language": "eng+spa",
    "usedOcrFallback": false,
    "processingNotes": "Extracted using Apache Tika"
  },
  "processingTimeMs": 1250
}
```

## Dependencies

### Core Libraries
- **Apache Tika 2.9.1** - Document text extraction
- **PDFBox 2.0.29** - PDF processing (compatible with Tika 2.9.1)
- **Tess4J 5.16.0** - OCR with English + Spanish support
- **Spring Boot 3.5.6** - Framework
- **Spring Boot Actuator** - Health checks

### System Requirements

#### For Local Development (macOS)
```bash
# Install Tesseract OCR
brew install tesseract

# Install language packs (optional)
brew install tesseract-lang
```

#### For Docker Deployment
The Dockerfile automatically installs Tesseract and required language packs:
```dockerfile
# Tesseract installation included in Docker image
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng tesseract-ocr-spa curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
```

### Version Compatibility Notes
- **PDFBox 2.0.29** is used instead of 3.x for compatibility with Tika 2.9.1
- This resolves `NoSuchMethodError` issues with PDFBox 3.x API changes
- OCR functionality requires Tesseract native library installation

### Configuration

#### application.yml
```yaml
conversion:
  ocr:
    languages: "eng+spa"  # English + Spanish
    enabled: true
    confidence-threshold: 60
    page-segmentation-mode: 1
  tika:
    enabled: true
    max-string-length: 100000
    timeout-seconds: 30
  pdfbox:
    enabled: true
    dpi: 300
    image-type: "png"
  file:
    max-size-mb: 50
    temp-dir: "/tmp/conversion"
  logging:
    enabled: true
```

## Deployment

### Local Development
```bash
./mvnw spring-boot:run
```

### Docker Build
```bash
docker build -t media-conversion-service .
```

### Cloud Run Deployment
```bash
# Build and deploy to Google Cloud Run
gcloud run deploy media-conversion-service \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=cloudrun
```

## Health Checks

The service includes health check endpoints for Cloud Run:
- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

## Error Handling

The service provides comprehensive error handling with appropriate HTTP status codes:
- `404 NOT_FOUND` - Document not found
- `403 FORBIDDEN` - Access denied
- `500 INTERNAL_SERVER_ERROR` - Conversion failures

## Future Enhancements

### Ready for Integration
- **Google Speech-to-Text API** - Audio conversion implementation ready
- **Google Vision API** - Image text extraction implementation ready
- **Additional document formats** - Extensible via Tika parsers
- **Enhanced OCR** - Custom Tesseract models support

## Technical Notes

### Tess4J Language Data
The service uses Tess4J 5.16.0 which includes native Tesseract binaries but requires language data files. For English + Spanish support, ensure the container has access to the appropriate traineddata files.

### Memory Considerations
OCR operations are memory-intensive. Cloud Run deployment is configured with:
- **JVM Settings**: `-Xmx512m -Xms256m -XX:+UseG1GC`
- **Reduced DPI**: 200 DPI for faster processing
- **Connection Pool**: Optimized for Cloud Run constraints

### Security
- **Non-root user** in Docker container
- **Graceful shutdown** support
- **Input validation** for file sizes and formats
- **Error sanitization** to prevent information leakage

## Monitoring

The service includes structured logging with:
- **Request/response logging** for conversion operations
- **Performance metrics** (processing time, confidence scores)
- **Error tracking** with detailed context
- **Cloud Logging compatible** JSON format
