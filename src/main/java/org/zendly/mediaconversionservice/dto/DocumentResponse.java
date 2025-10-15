package org.zendly.mediaconversionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for document retrieval
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    
    /**
     * Unique document identifier
     */
    private String documentId;
    
    /**
     * Original filename provided by user
     */
    private String originalFileName;
    
    /**
     * MIME type of the document
     */
    private String mimeType;

    private DocumentType documentType;
    
    /**
     * Document creation timestamp
     */
    private Long createdAt;
    
    /**
     * Pre-signed URL for downloading the document from GCP
     */
    private String downloadUrl;
}
