package org.zendly.mediaconversionservice.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to populate tenant context for each HTTP request
 * Extracts tenant information from headers and sets up thread-local context
 */
@Component
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {
    
    private final TenantResolver tenantResolver;
    
    @Autowired
    public TenantInterceptor(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // Skip tenant resolution for admin endpoints (they handle their own context)
            String requestURI = request.getRequestURI();
            if (isAdminEndpoint(requestURI)) {
                log.debug("Skipping tenant context setup for admin endpoint: {}", requestURI);
                return true;
            }

            // Resolve tenant context from request
            TenantContext tenantContext = tenantResolver.resolveTenantContext(request);
            
            if (tenantContext == null) {
                log.warn("Failed to resolve tenant context for request: {} {}", request.getMethod(), requestURI);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"Invalid or missing tenant information\"}");
                response.setContentType("application/json");
                return false;
            }
            
            if (!tenantContext.isActive()) {
                log.warn("Inactive tenant attempted access: {} for request: {} {}", 
                        tenantContext.getTenantId(), request.getMethod(), requestURI);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\":\"Tenant is inactive\"}");
                response.setContentType("application/json");
                return false;
            }
            // Set tenant context for current thread
            TenantContextHolder.setContext(tenantContext);
            
            return true;
            
        } catch (Exception e) {
            log.error("Error setting up tenant context for request: {} {}", 
                    request.getMethod(), request.getRequestURI(), e);
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
            response.setContentType("application/json");
            return false;
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        try {
            // Clear both tenant and tracking contexts to prevent memory leaks
            TenantContextHolder.clear();
            log.info("Contexts cleared for request: {} {}",
                    request.getMethod(), request.getRequestURI());
            
        } catch (Exception e) {
            log.error("Error clearing contexts for request: {} {}", 
                    request.getMethod(), request.getRequestURI(), e);
        }
    }
    
    /**
     * Check if the request URI is an admin endpoint that should skip tenant validation
     * @param requestURI the request URI
     * @return true if it's an admin endpoint, false otherwise
     */
    private boolean isAdminEndpoint(String requestURI) {
        if (requestURI == null) {
            return false;
        }
        
        // Admin endpoints that handle their own tenant context
        return requestURI.startsWith("/actuator") ||
               requestURI.startsWith("/swagger") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.equals("/") ||
               requestURI.equals("/health") ||
               requestURI.equals("/info");
    }
}
