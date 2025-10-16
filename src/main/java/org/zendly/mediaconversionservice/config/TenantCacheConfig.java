package org.zendly.mediaconversionservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for tenant caching to reduce API calls to WorkflowOrchestratorService
 */
@Data
@Configuration
@EnableCaching
@ConfigurationProperties(prefix = "tenant.cache")
public class TenantCacheConfig {

    /**
     * Enable/disable tenant caching
     */
    private boolean enabled = true;

    /**
     * Cache TTL in minutes
     */
    private int ttlMinutes = 30;

    /**
     * Maximum number of cached tenants
     */
    private int maxSize = 1000;

    /**
     * Refresh cache when remaining TTL is below this factor (0.0-1.0)
     * e.g., 0.8 means refresh when 80% of TTL has passed
     */
    private double refreshAheadFactor = 0.8;

    /**
     * Cache manager bean for tenant caching
     */
    @Bean
    public CacheManager tenantCacheManager() {
        return new ConcurrentMapCacheManager("tenantCache");
    }
}
