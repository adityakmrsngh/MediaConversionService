package org.zendly.mediaconversionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata information about the conversion process
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionMetadata {
    
    /**
     * Original file name
     */
    private String originalFileName;
    
    /**
     * MIME type of the original document
     */
    private String mimeType;
    
    /**
     * Document type (DOCUMENT, AUDIO, IMAGE, etc.)
     */
    private DocumentType documentType;
    
    /**
     * File size in bytes
     */
    private Long fileSizeBytes;
    
    /**
     * Number of pages processed (for documents)
     */
    private Integer pageCount;
    
    /**
     * OCR confidence score (0-100) if OCR was used
     */
    private Integer ocrConfidence;
    
    /**
     * Language detected/used for OCR
     */
    private String language;
    
    /**
     * Whether fallback to OCR was used
     */
    private Boolean usedOcrFallback;
    
    /**
     * Additional processing notes
     */
    private String processingNotes;
}
