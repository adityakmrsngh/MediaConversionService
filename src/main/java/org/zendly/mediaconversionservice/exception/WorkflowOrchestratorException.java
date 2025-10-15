package org.zendly.mediaconversionservice.exception;

/**
 * Exception thrown when workflow orchestrator operations fail
 */
public class WorkflowOrchestratorException extends RuntimeException {
    
    public WorkflowOrchestratorException(String message) {
        super(message);
    }
    
    public WorkflowOrchestratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
