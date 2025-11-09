package com.fingerprint.server.config;

import lombok.RequiredArgsConstructor;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ElasticSearch 客户端定制化配置。
 */
@Configuration
@RequiredArgsConstructor
public class ElasticsearchConfig {

    private final FingerprintElasticsearchProperties properties;

    /**
     * 自定义 REST 客户端构建器以配置超时与连接池。
     */
    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer() {
        return builder -> builder
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(Math.toIntExact(properties.getConnectTimeout().toMillis()))
                        .setSocketTimeout(Math.toIntExact(properties.getSocketTimeout().toMillis())))
                .setHttpClientConfigCallback((HttpAsyncClientBuilder httpClientBuilder) -> httpClientBuilder
                        .setMaxConnTotal(properties.getMaxConnections())
                        .setMaxConnPerRoute(properties.getMaxConnectionsPerRoute()));
    }
}
