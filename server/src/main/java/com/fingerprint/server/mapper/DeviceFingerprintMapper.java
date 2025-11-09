package com.fingerprint.server.mapper;

import com.fingerprint.server.dto.FingerprintRequest;
import com.fingerprint.server.model.DeviceFingerprintDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * FingerprintRequest 与 ES 文档的转换器。
 */
@Component
public class DeviceFingerprintMapper {

    /**
     * 将采集请求转换为 ElasticSearch 文档。
     *
     * @param request 采集请求
     * @return ES 文档
     */
    public DeviceFingerprintDocument toDocument(FingerprintRequest request) {
        DeviceFingerprintDocument.BrowserFingerprint browser = mapBrowser(request.browser());
        DeviceFingerprintDocument.DeviceFingerprint device = mapDevice(request.device());
        DeviceFingerprintDocument.NetworkFingerprint network = mapNetwork(request.network());
        // geoLocation 将在服务层通过IP地址自动查询，不从前端接收
        DeviceFingerprintDocument.CertificateFingerprint certificate = mapCertificate(request.certificate());

        List<String> ipHistory = new ArrayList<>();
        if (network != null && network.getIpAddress() != null) {
            ipHistory.add(network.getIpAddress());
        }

        return DeviceFingerprintDocument.builder()
                .visitorId(request.visitorId())
                .browser(browser)
                .device(device)
                .network(network)
                // geoLocation 在服务层通过GeoIP服务自动填充
                .certificate(certificate)
                .metadata(request.metadata() == null ? null : new java.util.HashMap<>(request.metadata()))
                .observationCount(1)
                .ipHistory(ipHistory)
                .createdAt(request.collectedAt())
                .updatedAt(request.collectedAt())
                .build();
    }

    private DeviceFingerprintDocument.BrowserFingerprint mapBrowser(FingerprintRequest.BrowserFingerprint browser) {
        if (browser == null) {
            return null;
        }
        return DeviceFingerprintDocument.BrowserFingerprint.builder()
                .userAgent(browser.userAgent())
                .language(browser.language())
                .timezone(browser.timezone())
                .plugins(browser.plugins())
                .canvasFingerprint(browser.canvasFingerprint())
                .webglFingerprint(browser.webglFingerprint())
                .audioFingerprint(browser.audioFingerprint())
                .build();
    }

    private DeviceFingerprintDocument.DeviceFingerprint mapDevice(FingerprintRequest.DeviceFingerprint device) {
        if (device == null) {
            return null;
        }
        return DeviceFingerprintDocument.DeviceFingerprint.builder()
                .platform(device.platform())
                .architecture(device.architecture())
                .touchPoints(device.touchPoints())
                .deviceMemory(device.deviceMemory())
                .cpuCores(device.cpuCores())
                .screenResolution(device.screenResolution())
                .colorDepth(device.colorDepth())
                .build();
    }

    private DeviceFingerprintDocument.NetworkFingerprint mapNetwork(FingerprintRequest.NetworkFingerprint network) {
        if (network == null) {
            return null;
        }
        return DeviceFingerprintDocument.NetworkFingerprint.builder()
                .ipAddress(network.ipAddress())
                .ipv6Address(network.ipv6Address())
                .connectionType(network.connectionType())
                .downlinkMbps(network.downlinkMbps())
                .rtt(network.rtt())
                .isp(network.isp())
                .extra(network.extra())
                .build();
    }

    private DeviceFingerprintDocument.CertificateFingerprint mapCertificate(FingerprintRequest.CertificateFingerprint certificate) {
        if (certificate == null || (certificate.fingerprints() == null && certificate.pinningHashes() == null)) {
            return null;
        }
        return DeviceFingerprintDocument.CertificateFingerprint.builder()
                .fingerprints(certificate.fingerprints())
                .pinningHashes(certificate.pinningHashes())
                .build();
    }
}
