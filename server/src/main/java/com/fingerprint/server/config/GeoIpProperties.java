package com.fingerprint.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GeoIP配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "fingerprint.geoip")
public class GeoIpProperties {

    /**
     * GeoLite2-City.mmdb 数据库文件路径
     */
    private String databasePath = "GeoLite2-City.mmdb";

    /**
     * 是否启用GeoIP查询
     */
    private boolean enabled = true;

    /**
     * 缓存大小（条目数）
     */
    private int cacheSize = 4096;
}
