package com.fingerprint.server.config;

import com.maxmind.geoip2.DatabaseReader;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * GeoIP配置类。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "fingerprint.geoip", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GeoIpConfig {

    private static final Logger log = LoggerFactory.getLogger(GeoIpConfig.class);

    private final GeoIpProperties geoIpProperties;
    private final ResourceLoader resourceLoader;

    /**
     * 创建MaxMind DatabaseReader Bean。
     *
     * @return DatabaseReader实例
     * @throws IOException 如果数据库文件读取失败
     */
    @Bean
    public DatabaseReader databaseReader() throws IOException {
        String databasePath = geoIpProperties.getDatabasePath();
        log.info("Initializing GeoIP DatabaseReader with database path: {}", databasePath);

        Resource resource = resourceLoader.getResource(databasePath);
        
        if (!resource.exists()) {
            log.warn("GeoIP database not found at: {}, trying classpath location", databasePath);
            resource = resourceLoader.getResource("classpath:" + databasePath);
        }

        if (!resource.exists()) {
            throw new IOException("GeoIP database file not found: " + databasePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            DatabaseReader reader = new DatabaseReader.Builder(inputStream)
                    .withCache(new com.maxmind.db.CHMCache(geoIpProperties.getCacheSize()))
                    .build();
            log.info("GeoIP DatabaseReader initialized successfully");
            return reader;
        } catch (IOException e) {
            log.error("Failed to initialize GeoIP DatabaseReader", e);
            throw e;
        }
    }
}
