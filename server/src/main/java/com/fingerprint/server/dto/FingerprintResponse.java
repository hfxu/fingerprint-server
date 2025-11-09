package com.fingerprint.server.dto;

import java.time.Instant;
import java.util.Map;

/**
 * 服务端对设备指纹采集请求的响应。
 */
public record FingerprintResponse(
        String deviceId,
        boolean matched,
        Double similarityScore,
        String matchedDeviceId,
        Instant matchedAt,
        Map<String, Object> indicators
) {
}
