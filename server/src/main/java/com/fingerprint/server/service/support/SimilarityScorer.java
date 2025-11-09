package com.fingerprint.server.service.support;

import com.fingerprint.server.config.SimilarityProperties;
import com.fingerprint.server.model.DeviceFingerprintDocument;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 负责计算设备指纹之间的相似度。
 */
@Component
@RequiredArgsConstructor
public class SimilarityScorer {

    private final SimilarityProperties similarityProperties;

    /**
     * 计算两个指纹文档的相似度得分。
     *
     * @param incoming 新上报的指纹
     * @param existing 历史指纹
     * @return 0~1 之间的相似度
     */
    public double calculate(DeviceFingerprintDocument incoming, DeviceFingerprintDocument existing) {
        if (incoming == null || existing == null) {
            return 0d;
        }
        double score = 0d;
        score += similarityProperties.getVisitorWeight() * visitorScore(incoming.getVisitorId(), existing.getVisitorId());
        score += similarityProperties.getBrowserWeight() * browserScore(incoming.getBrowser(), existing.getBrowser());
        score += similarityProperties.getDeviceWeight() * deviceScore(incoming.getDevice(), existing.getDevice());
        score += similarityProperties.getNetworkWeight() * networkScore(incoming.getNetwork(), existing.getNetwork());
        score += similarityProperties.getGeoWeight() * geoScore(incoming.getGeoLocation(), existing.getGeoLocation());
        score += similarityProperties.getCertificateWeight() * certificateScore(incoming.getCertificate(), existing.getCertificate());

        return Math.min(1d, Math.max(0d, score));
    }

    private double visitorScore(String incoming, String existing) {
        return StringUtils.equals(incoming, existing) ? 1d : 0d;
    }

    private double browserScore(DeviceFingerprintDocument.BrowserFingerprint incoming,
                                DeviceFingerprintDocument.BrowserFingerprint existing) {
        if (incoming == null || existing == null) {
            return 0d;
        }
        double userAgentScore = normalizedStringScore(incoming.getUserAgent(), existing.getUserAgent());
        double languageScore = normalizedStringScore(incoming.getLanguage(), existing.getLanguage());
        double timezoneScore = normalizedStringScore(incoming.getTimezone(), existing.getTimezone());
        double pluginScore = listJaccard(incoming.getPlugins(), existing.getPlugins());
        double canvasScore = normalizedStringScore(incoming.getCanvasFingerprint(), existing.getCanvasFingerprint());
        double webglScore = normalizedStringScore(incoming.getWebglFingerprint(), existing.getWebglFingerprint());
        double audioScore = normalizedStringScore(incoming.getAudioFingerprint(), existing.getAudioFingerprint());

        return average(userAgentScore, languageScore, timezoneScore, pluginScore, canvasScore, webglScore, audioScore);
    }

    private double deviceScore(DeviceFingerprintDocument.DeviceFingerprint incoming,
                                DeviceFingerprintDocument.DeviceFingerprint existing) {
        if (incoming == null || existing == null) {
            return 0d;
        }
        double platformScore = normalizedStringScore(incoming.getPlatform(), existing.getPlatform());
        double archScore = normalizedStringScore(incoming.getArchitecture(), existing.getArchitecture());
        double touchScore = normalizedNumericScore(incoming.getTouchPoints(), existing.getTouchPoints());
        double memoryScore = normalizedNumericScore(incoming.getDeviceMemory(), existing.getDeviceMemory());
        double cpuScore = normalizedNumericScore(incoming.getCpuCores(), existing.getCpuCores());
        double resolutionScore = normalizedStringScore(incoming.getScreenResolution(), existing.getScreenResolution());
        double colorDepthScore = normalizedStringScore(incoming.getColorDepth(), existing.getColorDepth());

        return average(platformScore, archScore, touchScore, memoryScore, cpuScore, resolutionScore, colorDepthScore);
    }

    private double networkScore(DeviceFingerprintDocument.NetworkFingerprint incoming,
                                 DeviceFingerprintDocument.NetworkFingerprint existing) {
        if (incoming == null || existing == null) {
            return 0d;
        }
        double ipv4Score = normalizedStringScore(incoming.getIpAddress(), existing.getIpAddress());
        double ipv6Score = normalizedStringScore(incoming.getIpv6Address(), existing.getIpv6Address());
        double typeScore = normalizedStringScore(incoming.getConnectionType(), existing.getConnectionType());
        double downlinkScore = normalizedDoubleRange(incoming.getDownlinkMbps(), existing.getDownlinkMbps(), 5d);
        double rttScore = normalizedDoubleRange(incoming.getRtt(), existing.getRtt(), 50d);
        double ispScore = normalizedStringScore(incoming.getIsp(), existing.getIsp());

        return average(ipv4Score, ipv6Score, typeScore, downlinkScore, rttScore, ispScore);
    }

    private double geoScore(DeviceFingerprintDocument.GeoLocation incoming,
                            DeviceFingerprintDocument.GeoLocation existing) {
        if (incoming == null || existing == null) {
            return 0d;
        }
        double countryScore = normalizedStringScore(incoming.getCountry(), existing.getCountry());
        double regionScore = normalizedStringScore(incoming.getRegion(), existing.getRegion());
        double cityScore = normalizedStringScore(incoming.getCity(), existing.getCity());
        double timezoneScore = normalizedStringScore(incoming.getTimezone(), existing.getTimezone());
        Double distanceKm = geoDistance(incoming, existing);
        double distanceScore = distanceKm == null ? 0d : switch (distanceKm.intValue()) {
            case 0 -> 1d;
            default -> distanceKm < 20 ? 0.9d : distanceKm < 50 ? 0.7d : distanceKm < 200 ? 0.4d : 0.1d;
        };

        return average(countryScore, regionScore, cityScore, timezoneScore, distanceScore);
    }

    private double certificateScore(DeviceFingerprintDocument.CertificateFingerprint incoming,
                                    DeviceFingerprintDocument.CertificateFingerprint existing) {
        if (incoming == null || existing == null) {
            return 0d;
        }
        double fingerprintScore = listJaccard(incoming.getFingerprints(), existing.getFingerprints());
        double pinningScore = listJaccard(incoming.getPinningHashes(), existing.getPinningHashes());
        return average(fingerprintScore, pinningScore);
    }

    private double normalizedStringScore(String a, String b) {
        if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) {
            return 0d;
        }
        return StringUtils.equalsIgnoreCase(a, b) ? 1d : 0d;
    }

    private double normalizedNumericScore(Integer a, Integer b) {
        if (a == null || b == null) {
            return 0d;
        }
        return Objects.equals(a, b) ? 1d : 0d;
    }

    private double normalizedDoubleRange(Double a, Double b, double tolerance) {
        if (a == null || b == null) {
            return 0d;
        }
        double diff = Math.abs(a - b);
        if (diff < 0.0001d) {
            return 1d;
        }
        return diff <= tolerance ? Math.max(0d, 1d - diff / tolerance) : 0d;
    }

    private double listJaccard(List<String> a, List<String> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0d;
        }
        Set<String> union = new HashSet<>();
        Set<String> intersection = new HashSet<>();
        for (String value : a) {
            if (value != null) {
                union.add(value);
            }
        }
        for (String value : b) {
            if (value != null) {
                if (union.contains(value)) {
                    intersection.add(value);
                }
                union.add(value);
            }
        }
        if (union.isEmpty()) {
            return 0d;
        }
        return (double) intersection.size() / (double) union.size();
    }

    private double average(double... values) {
        double sum = 0d;
        int count = 0;
        for (double value : values) {
            if (!Double.isNaN(value)) {
                sum += value;
                count++;
            }
        }
        return count == 0 ? 0d : sum / count;
    }

    private Double geoDistance(DeviceFingerprintDocument.GeoLocation incoming,
                               DeviceFingerprintDocument.GeoLocation existing) {
        Double lat1 = incoming.getLatitude();
        Double lon1 = incoming.getLongitude();
        Double lat2 = existing.getLatitude();
        Double lon2 = existing.getLongitude();
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }
        double R = 6371.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
