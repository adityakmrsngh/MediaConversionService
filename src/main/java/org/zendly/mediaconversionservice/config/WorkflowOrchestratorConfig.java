package org.zendly.mediaconversionservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Workflow Orchestrator HTTP client
 */
@Configuration
@Slf4j
public class WorkflowOrchestratorConfig {

    // RestTemplate configuration from application.yml
    @Value("${rest-template.connection-pool.max-total}")
    private int maxTotal;
    @Value("${rest-template.connection-pool.max-per-route}")
    private int maxPerRoute;
    @Value("${rest-template.connection-pool.idle-connection-timeout-seconds}")
    private int idleConnectionTimeoutSeconds;
    @Value("${rest-template.connection-pool.validate-after-inactivity-seconds}")
    private int validateAfterInactivitySeconds;
    @Value("${rest-template.timeout.connection}")
    private int connectionTimeout;
    @Value("${rest-template.timeout.read}")
    private int readTimeout;
    @Value("${rest-template.timeout.connection-request}")
    private int connectionRequestTimeout;
    @Value("${rest-template.compression.enabled}")
    private boolean compressionEnabled;
    @Value("${rest-template.compression.min-request-size}")
    private int minRequestSizeForCompression;
    @Value("${rest-template.headers.default-content-type}")
    private String defaultContentType;
    @Value("${rest-template.logging.enabled}")
    private boolean loggingEnabled;

    /**
     * RestTemplate bean configured for workflow orchestrator communication
     */
    @Bean("workflowOrchestratorRestTemplate")
    public RestTemplate workflowOrchestratorRestTemplate(HttpComponentsClientHttpRequestFactory factory) {
        return new RestTemplate(factory);
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory getHttpComponentsClientHttpRequestFactory() {
        // Configure connection pool with configurable values
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(maxTotal)
                .setMaxConnPerRoute(maxPerRoute)
                .setDefaultConnectionConfig(ConnectionConfig
                        .custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(connectionTimeout))
                        .setValidateAfterInactivity(TimeValue.ofSeconds(validateAfterInactivitySeconds))
                        .setSocketTimeout(Timeout.ofMilliseconds(connectionTimeout))
                        .build())
                .build();

        // Configure request timeouts and compression
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout));

        // Enable gzip compression if configured
        if (compressionEnabled) {
            requestConfigBuilder.setContentCompressionEnabled(true);
            if (loggingEnabled) {
                log.info("HTTP compression enabled - Min request size: {} bytes", minRequestSizeForCompression);
            }
        }

        RequestConfig requestConfig = requestConfigBuilder.build();

        // Create HTTP client with connection pooling and configurable idle timeout
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(idleConnectionTimeoutSeconds))
                .build();

        if (loggingEnabled) {
            log.info("HTTP Client configured - Connection timeout: {}ms, Read timeout: {}ms, Idle timeout: {}s, Compression: {}",
                    connectionTimeout, readTimeout, idleConnectionTimeoutSeconds, compressionEnabled);
        }

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return factory;
    }
}
