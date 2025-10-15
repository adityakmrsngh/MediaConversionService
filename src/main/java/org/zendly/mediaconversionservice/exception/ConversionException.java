package org.zendly.mediaconversionservice.exception;

/**
 * Custom exception for document conversion errors
 * Extends the existing WorkflowOrchestratorException pattern
 */
public class ConversionException extends RuntimeException {

    private final String documentId;
    private final String conversionMethod;

    public ConversionException(String message) {
        super(message);
        this.documentId = null;
        this.conversionMethod = null;
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
        this.documentId = null;
        this.conversionMethod = null;
    }

    public ConversionException(String message, String documentId, String conversionMethod) {
        super(message);
        this.documentId = documentId;
        this.conversionMethod = conversionMethod;
    }

    public ConversionException(String message, String documentId, String conversionMethod, Throwable cause) {
        super(message, cause);
        this.documentId = documentId;
        this.conversionMethod = conversionMethod;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getConversionMethod() {
        return conversionMethod;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(": ").append(getMessage());
        
        if (documentId != null) {
            sb.append(" [Document ID: ").append(documentId).append("]");
        }
        
        if (conversionMethod != null) {
            sb.append(" [Method: ").append(conversionMethod).append("]");
        }
        
        return sb.toString();
    }
}
