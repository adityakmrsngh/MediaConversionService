package org.zendly.mediaconversionservice.constants;

public class ApplicationConstants {

    public static final String APPLICATION_NAME="MediaConversionService";
    public static final String SYSTEM_USER="message-conversion";
    public static final String CONTENT_TYPE_JSON = "application/json";
    
    // Conversion related constants
    public static final String CONVERSION_SUCCESS = "CONVERSION_SUCCESS";
    public static final String CONVERSION_FAILED = "CONVERSION_FAILED";
    public static final String CONVERSION_NOT_SUPPORTED = "CONVERSION_NOT_SUPPORTED";
    
    // Supported MIME types
    public static final String MIME_TYPE_PDF = "application/pdf";
    public static final String MIME_TYPE_DOC = "application/msword";
    public static final String MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String MIME_TYPE_XLS = "application/vnd.ms-excel";
    public static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String MIME_TYPE_PPT = "application/vnd.ms-powerpoint";
    public static final String MIME_TYPE_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static final String MIME_TYPE_TXT = "text/plain";
    public static final String MIME_TYPE_RTF = "application/rtf";
    public static final String MIME_TYPE_JSON = "application/json";
    public static final String MIME_TYPE_HTML = "text/html";
    public static final String MIME_TYPE_XML = "text/xml";
    public static final String MIME_TYPE_APP_XML = "application/xml";
    
    // Audio MIME types (for future implementation)
    public static final String MIME_TYPE_MP3 = "audio/mpeg";
    public static final String MIME_TYPE_WAV = "audio/wav";
    public static final String MIME_TYPE_M4A = "audio/mp4";
    public static final String MIME_TYPE_FLAC = "audio/flac";
    
    // Image MIME types (for future implementation)
    public static final String MIME_TYPE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_PNG = "image/png";
    public static final String MIME_TYPE_TIFF = "image/tiff";
    public static final String MIME_TYPE_BMP = "image/bmp";
    
    // Conversion methods
    public static final String METHOD_TIKA = "APACHE_TIKA";
    public static final String METHOD_AUDIO_STT = "GOOGLE_SPEECH_TO_TEXT";
    public static final String METHOD_IMAGE_VISION = "GOOGLE_VISION_API";
    
    // Error messages
    public static final String ERROR_FILE_NOT_FOUND = "Document file not found";
    public static final String ERROR_UNSUPPORTED_FORMAT = "Unsupported document format";
    public static final String ERROR_CONVERSION_FAILED = "Document conversion failed";
    public static final String ERROR_FILE_TOO_LARGE = "File size exceeds maximum limit";
    
    // Processing strategy constants
    public static final String PROCESSING_TEXT_ONLY = "TEXT_ONLY";
    public static final String PROCESSING_OCR_ONLY = "OCR_ONLY";
    public static final String PROCESSING_OCR_WITH_VISION_FALLBACK = "OCR_WITH_VISION_FALLBACK";
    public static final String PROCESSING_TEXT_WITH_OCR_FALLBACK = "TEXT_WITH_OCR_FALLBACK";
    
    // Text-based MIME types (no OCR needed)
    public static final String[] TEXT_BASED_MIME_TYPES = {
        MIME_TYPE_DOC,
        MIME_TYPE_DOCX,
        MIME_TYPE_XLS,
        MIME_TYPE_XLSX,
        MIME_TYPE_PPT,
        MIME_TYPE_PPTX,
        MIME_TYPE_TXT,
        MIME_TYPE_RTF,
        MIME_TYPE_JSON,
        MIME_TYPE_HTML,
        MIME_TYPE_XML,
        MIME_TYPE_APP_XML
    };
    
    // Image MIME types (OCR required)
    public static final String[] IMAGE_MIME_TYPES = {
        MIME_TYPE_JPEG,
        MIME_TYPE_PNG,
        MIME_TYPE_TIFF,
        MIME_TYPE_BMP
    };
    
    // Google Vision fallback threshold
    public static final int DEFAULT_GOOGLE_VISION_THRESHOLD = 75;
}
