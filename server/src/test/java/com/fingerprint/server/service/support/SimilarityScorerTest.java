package com.fingerprint.server.service.support;

import com.fingerprint.server.config.SimilarityProperties;
import com.fingerprint.server.model.DeviceFingerprintDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 相似度计算逻辑单元测试。
 */
class SimilarityScorerTest {

    private SimilarityScorer similarityScorer;

    @BeforeEach
    void setUp() {
        SimilarityProperties properties = new SimilarityProperties();
        similarityScorer = new SimilarityScorer(properties);
    }

    @Test
    void shouldReturnOneForIdenticalFingerprint() {
        DeviceFingerprintDocument base = buildDocument("visitor-1", "1.1.1.1");
        double score = similarityScorer.calculate(base, base);
        assertEquals(1.0d, score, 1e-6, "完全一致的指纹相似度应为 1");
    }

    @Test
    void shouldDecreaseScoreWhenNetworkDiffers() {
        DeviceFingerprintDocument incoming = buildDocument("visitor-1", "1.1.1.1");
        DeviceFingerprintDocument existing = buildDocument("visitor-1", "8.8.8.8");
        double score = similarityScorer.calculate(incoming, existing);
        assertTrue(score < 1.0d && score > 0.5d, "网络差异应降低相似度但仍大于阈值的一半");
    }

    private DeviceFingerprintDocument buildDocument(String visitorId, String ipAddress) {
        return DeviceFingerprintDocument.builder()
                .id("test")
                .visitorId(visitorId)
                .browser(DeviceFingerprintDocument.BrowserFingerprint.builder()
                        .userAgent("UA")
                        .language("zh-CN")
                        .timezone("Asia/Shanghai")
                        .plugins(List.of("PluginA", "PluginB"))
                        .canvasFingerprint("canvas")
                        .webglFingerprint("webgl")
                        .audioFingerprint("audio")
                        .build())
                .device(DeviceFingerprintDocument.DeviceFingerprint.builder()
                        .platform("macOS")
                        .architecture("x86_64")
                        .touchPoints(5)
                        .deviceMemory(16)
                        .cpuCores(8)
                        .screenResolution("2560x1600")
                        .colorDepth("24")
                        .build())
                .network(DeviceFingerprintDocument.NetworkFingerprint.builder()
                        .ipAddress(ipAddress)
                        .ipv6Address(null)
                        .connectionType("wifi")
                        .downlinkMbps(120d)
                        .rtt(20d)
                        .isp("ISP")
                        .build())
                .geoLocation(DeviceFingerprintDocument.GeoLocation.builder()
                        .country("CN")
                        .region("SH")
                        .city("Shanghai")
                        .latitude(31.2304)
                        .longitude(121.4737)
                        .timezone("Asia/Shanghai")
                        .build())
                .certificate(DeviceFingerprintDocument.CertificateFingerprint.builder()
                        .fingerprints(List.of("cert1"))
                        .pinningHashes(List.of("pin1"))
                        .build())
                .build();
    }
}
