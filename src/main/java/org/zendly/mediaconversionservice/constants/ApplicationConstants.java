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
    public static final String METHOD_OCR = "PDFBOX_TESS4J_OCR";
    public static final String METHOD_GOOGLE_VISION = "GOOGLE_VISION_API";
    public static final String METHOD_AUDIO_STT = "GOOGLE_SPEECH_TO_TEXT";
    public static final String METHOD_IMAGE_VISION = "GOOGLE_VISION_API";
    
    // Error messages
    public static final String ERROR_FILE_NOT_FOUND = "Document file not found";
    public static final String ERROR_UNSUPPORTED_FORMAT = "Unsupported document format";
    public static final String ERROR_CONVERSION_FAILED = "Document conversion failed";
    public static final String ERROR_FILE_TOO_LARGE = "File size exceeds maximum limit";
    public static final String ERROR_EMPTY_CONTENT = "No text content found in document";
}
