package com.fingerprint.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 存储在 ElasticSearch 中的设备指纹文档。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "device_fingerprints")
public class DeviceFingerprintDocument {

    /**
     * 指纹采集记录唯一标识。
     */
    @Id
    private String id;

    /**
     * FingerprintJS 生成的访客标识。
     */
    @Field(type = FieldType.Keyword)
    private String visitorId;

    /**
     * 浏览器指纹描述。
     */
    @Field(type = FieldType.Object)
    private BrowserFingerprint browser;

    /**
     * 终端硬件信息。
     */
    @Field(type = FieldType.Object)
    private DeviceFingerprint device;

    /**
     * 网络层指纹信息。
     */
    @Field(type = FieldType.Object)
    private NetworkFingerprint network;

    /**
     * IP 地理位置信息。
     */
    @Field(type = FieldType.Object)
    private GeoLocation geoLocation;

    /**
     * TLS 证书相关指纹。
     */
    @Field(type = FieldType.Object)
    private CertificateFingerprint certificate;

    /**
     * 额外指标或客户端自定义字段。
     */
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;

    /**
     * 最近一次匹配相似度得分。
     */
    @Field(type = FieldType.Double)
    private Double similarityScore;

    /**
     * 匹配到的历史设备标识。
     */
    @Field(type = FieldType.Keyword)
    private String matchedDeviceId;

    /**
     * 累计观测次数。
     */
    @Field(type = FieldType.Integer)
    private Integer observationCount;

    /**
     * 历史公网 IP 列表。
     */
    @Field(type = FieldType.Keyword)
    private List<String> ipHistory;

    /**
     * 首次采集时间。
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    /**
     * 最近一次采集时间。
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant updatedAt;

    /**
     * 浏览器指纹细节。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrowserFingerprint {
        @Field(type = FieldType.Keyword)
        private String userAgent;
        @Field(type = FieldType.Keyword)
        private String language;
        @Field(type = FieldType.Keyword)
        private String timezone;
        @Field(type = FieldType.Keyword)
        private List<String> plugins;
        @Field(type = FieldType.Keyword)
        private String canvasFingerprint;
        @Field(type = FieldType.Keyword)
        private String webglFingerprint;
        @Field(type = FieldType.Keyword)
        private String audioFingerprint;
    }

    /**
     * 终端硬件指纹细节。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceFingerprint {
        @Field(type = FieldType.Keyword)
        private String platform;
        @Field(type = FieldType.Keyword)
        private String architecture;
        @Field(type = FieldType.Integer)
        private Integer touchPoints;
        @Field(type = FieldType.Integer)
        private Integer deviceMemory;
        @Field(type = FieldType.Integer)
        private Integer cpuCores;
        @Field(type = FieldType.Keyword)
        private String screenResolution;
        @Field(type = FieldType.Keyword)
        private String colorDepth;
    }

    /**
     * 网络指纹细节。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkFingerprint {
        @Field(type = FieldType.Keyword)
        private String ipAddress;
        @Field(type = FieldType.Keyword)
        private String ipv6Address;
        @Field(type = FieldType.Keyword)
        private String connectionType;
        @Field(type = FieldType.Double)
        private Double downlinkMbps;
        @Field(type = FieldType.Double)
        private Double rtt;
        @Field(type = FieldType.Keyword)
        private String isp;
        @Field(type = FieldType.Object)
        private Map<String, Object> extra;
    }

    /**
     * 地理位置。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocation {
        @Field(type = FieldType.Keyword)
        private String country;
        @Field(type = FieldType.Keyword)
        private String region;
        @Field(type = FieldType.Keyword)
        private String city;
        @Field(type = FieldType.Double)
        private Double latitude;
        @Field(type = FieldType.Double)
        private Double longitude;
        @Field(type = FieldType.Keyword)
        private String timezone;
        @Field(type = FieldType.Object)
        private Map<String, Object> extra;
    }

    /**
     * TLS 证书指纹。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateFingerprint {
        @Field(type = FieldType.Keyword)
        private List<String> fingerprints;
        @Field(type = FieldType.Keyword)
        private List<String> pinningHashes;
    }
}
