package com.fingerprint.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * ElasticSearch 连接自定义配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "fingerprint.elasticsearch")
public class FingerprintElasticsearchProperties {

    /**
     * 连接超时时间。
     */
    private Duration connectTimeout = Duration.ofSeconds(3);

    /**
     * 套接字超时时间。
     */
    private Duration socketTimeout = Duration.ofSeconds(30);

    /**
     * 最大连接数。
     */
    private int maxConnections = 100;

    /**
     * 单路由最大连接数。
     */
    private int maxConnectionsPerRoute = 50;
}
