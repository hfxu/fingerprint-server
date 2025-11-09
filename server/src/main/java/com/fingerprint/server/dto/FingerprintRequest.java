package com.fingerprint.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 客户端上报的设备指纹原始数据。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FingerprintRequest(
        @NotBlank(message = "visitorId 不能为空")
        String visitorId,

        @NotNull(message = "浏览器指纹不能为空")
        @Valid
        BrowserFingerprint browser,

        @NotNull(message = "终端硬件指纹不能为空")
        @Valid
        DeviceFingerprint device,

        @NotNull(message = "网络指纹不能为空")
        @Valid
        NetworkFingerprint network,

        @Valid
        GeoLocation geoLocation,

        @Valid
        CertificateFingerprint certificate,

        @NotNull(message = "采集时间不能为空")
        Instant collectedAt,

        Map<String, Object> metadata
) {

    /**
     * 浏览器相关指纹信息。
     */
    public record BrowserFingerprint(
            @NotBlank(message = "userAgent 不能为空")
            String userAgent,
            @NotBlank(message = "language 不能为空")
            String language,
            @NotBlank(message = "timezone 不能为空")
            String timezone,
            @Size(min = 1, message = "plugins 至少需要一个元素")
            List<String> plugins,
            String canvasFingerprint,
            String webglFingerprint,
            String audioFingerprint
    ) {
    }

    /**
     * 终端硬件指纹信息。
     */
    public record DeviceFingerprint(
            @NotBlank(message = "platform 不能为空")
            String platform,
            @NotBlank(message = "architecture 不能为空")
            String architecture,
            @NotNull(message = "触控点数量不能为空")
            Integer touchPoints,
            @NotNull(message = "内存容量不能为空")
            Integer deviceMemory,
            @NotNull(message = "cpu 核心数不能为空")
            Integer cpuCores,
            @NotBlank(message = "screenResolution 不能为空")
            String screenResolution,
            @NotBlank(message = "colorDepth 不能为空")
            String colorDepth
    ) {
    }

    /**
     * 网络层指纹信息。
     */
    public record NetworkFingerprint(
            @NotBlank(message = "ipAddress 不能为空")
            String ipAddress,
            String ipv6Address,
            @NotBlank(message = "connectionType 不能为空")
            String connectionType,
            @NotNull(message = "downlinkMbps 不能为空")
            Double downlinkMbps,
            @NotNull(message = "rtt 不能为空")
            Double rtt,
            String isp,
            Map<String, Object> extra
    ) {
    }

    /**
     * IP 地理位置信息。
     */
    public record GeoLocation(
            String country,
            String region,
            String city,
            Double latitude,
            Double longitude,
            String timezone,
            Map<String, Object> extra
    ) {
    }

    /**
     * TLS 证书指纹信息。
     */
    public record CertificateFingerprint(
            @Size(min = 1, message = "证书指纹列表不能为空")
            List<String> fingerprints,
            List<String> pinningHashes
    ) {
    }
}
