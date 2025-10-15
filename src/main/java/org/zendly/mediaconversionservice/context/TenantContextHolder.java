package org.zendly.mediaconversionservice.context;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local holder for tenant context information
 * Manages tenant context lifecycle per request thread
 */
@Slf4j
public class TenantContextHolder {
    
    private static final ThreadLocal<TenantContext> contextHolder = new ThreadLocal<>();
    
    /**
     * Set the tenant context for the current thread
     * @param context the tenant context to set
     */
    public static void setContext(TenantContext context) {
        if (context == null) {
            log.error("Attempted to set null tenant context");
            throw new RuntimeException("Attempted to set null tenant context");
        }
        
        log.debug("Setting tenant context for thread: {}", context.getTenantId());
        contextHolder.set(context);
    }
    
    /**
     * Get the tenant context for the current thread
     * @return the tenant context, or null if not set
     */
    public static TenantContext getContext() {
        return contextHolder.get();
    }
    
    /**
     * Get the current tenant ID
     * @return the tenant ID, or null if no context is set
     */
    public static String getCurrentTenantId() {
        TenantContext context = getContext();
        return context != null ? context.getTenantId() : null;
    }
    
    /**
     * Get the current tenant database name
     * @return the database name, or null if no context is set
     */
    public static String getCurrentDatabaseName() {
        TenantContext context = getContext();
        return context != null ? context.getDatabaseName() : null;
    }
    
    /**
     * Get the current tenant connection string
     * @return the connection string, or null if no context is set
     */
    public static String getCurrentConnectionString() {
        TenantContext context = getContext();
        return context != null ? context.getConnectionString() : null;
    }

    /**
     * Check if tenant context is set for current thread
     * @return true if context is set, false otherwise
     */
    public static boolean hasContext() {
        return getContext() != null;
    }
    
    /**
     * Clear the tenant context for the current thread
     * Should be called at the end of request processing to prevent memory leaks
     */
    public static void clear() {
        TenantContext context = getContext();
        if (context != null) {
            log.debug("Clearing tenant context for thread: {}", context.getTenantId());
            contextHolder.remove();
        }
    }
}
