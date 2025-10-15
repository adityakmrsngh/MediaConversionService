package org.zendly.mediaconversionservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.zendly.mediaconversionservice.context.TenantInterceptor;

/**
 * Configuration for registering the tenant interceptor
 * Applies tenant context resolution to all API requests
 */
@Configuration
public class TenantInterceptorConfig implements WebMvcConfigurer {
    
    private final TenantInterceptor tenantInterceptor;
    
    @Autowired
    public TenantInterceptorConfig(TenantInterceptor tenantInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")  // Apply to all API endpoints
                .excludePathPatterns(
                    "/actuator/**",           // Exclude actuator endpoints
                    "/swagger-ui/**",         // Exclude Swagger UI
                    "/v3/api-docs/**",         // Exclude OpenAPI docs
                    "/api/whatsapp/webhook"
                );
    }
}
