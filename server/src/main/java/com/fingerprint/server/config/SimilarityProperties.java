package com.fingerprint.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 相似度权重配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "fingerprint.similarity")
public class SimilarityProperties {

    /**
     * 判定为同一设备的最低相似度阈值。
     */
    private double threshold = 0.95d;

    /**
     * visitorId 权重。
     */
    private double visitorWeight = 0.35d;

    /**
     * 浏览器指纹权重。
     */
    private double browserWeight = 0.20d;

    /**
     * 终端硬件指纹权重。
     */
    private double deviceWeight = 0.20d;

    /**
     * 网络指纹权重。
     */
    private double networkWeight = 0.10d;

    /**
     * 地理位置权重。
     */
    private double geoWeight = 0.10d;

    /**
     * 证书指纹权重。
     */
    private double certificateWeight = 0.05d;
}
