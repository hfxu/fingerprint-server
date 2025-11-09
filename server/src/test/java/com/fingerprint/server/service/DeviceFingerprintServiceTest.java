package com.fingerprint.server.service;

import com.fingerprint.server.config.SimilarityProperties;
import com.fingerprint.server.dto.FingerprintRequest;
import com.fingerprint.server.dto.FingerprintResponse;
import com.fingerprint.server.mapper.DeviceFingerprintMapper;
import com.fingerprint.server.model.DeviceFingerprintDocument;
import com.fingerprint.server.repository.DeviceFingerprintRepository;
import com.fingerprint.server.service.support.SimilarityScorer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 设备指纹服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DeviceFingerprintServiceTest {

    @Mock
    private DeviceFingerprintRepository repository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private DeviceFingerprintService deviceFingerprintService;

    private DeviceFingerprintMapper mapper;

    private SimilarityProperties properties;

    @BeforeEach
    void setUp() {
        mapper = new DeviceFingerprintMapper();
        properties = new SimilarityProperties();
        SimilarityScorer scorer = new SimilarityScorer(properties);
        deviceFingerprintService = new DeviceFingerprintService(repository, elasticsearchOperations, mapper, scorer, properties);
    }

    @Test
    void shouldCreateNewDeviceWhenNoMatchFound() {
        FingerprintRequest request = buildRequest("visitor-new", "2.2.2.2");
        when(repository.findTopByVisitorIdOrderByUpdatedAtDesc("visitor-new")).thenReturn(Optional.empty());
        when(elasticsearchOperations.search(any(), eq(DeviceFingerprintDocument.class))).thenReturn(emptySearchHits());
        when(repository.save(any(DeviceFingerprintDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FingerprintResponse response = deviceFingerprintService.handleFingerprint(request, "2.2.2.2");

        assertFalse(response.matched());
        assertEquals(0d, response.similarityScore());
        verify(repository, times(1)).save(any(DeviceFingerprintDocument.class));
    }

    @Test
    void shouldUpdateExistingDeviceWhenSimilarityIsHigh() {
        FingerprintRequest request = buildRequest("visitor-existing", "1.1.1.1");
        DeviceFingerprintDocument existing = mapper.toDocument(request);
        existing.setId("device-1");
        existing.setObservationCount(5);
        when(repository.findTopByVisitorIdOrderByUpdatedAtDesc("visitor-existing")).thenReturn(Optional.of(existing));
        when(elasticsearchOperations.search(any(), eq(DeviceFingerprintDocument.class))).thenReturn(emptySearchHits());
        when(repository.save(any(DeviceFingerprintDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FingerprintResponse response = deviceFingerprintService.handleFingerprint(request, "1.1.1.1");

        assertTrue(response.matched());
        assertEquals("device-1", response.deviceId());
        assertTrue(response.similarityScore() >= properties.getThreshold());
        verify(repository, times(1)).save(existing);
    }

    private FingerprintRequest buildRequest(String visitorId, String ip) {
        return new FingerprintRequest(
                visitorId,
                new FingerprintRequest.BrowserFingerprint(
                        "Mozilla/5.0",
                        "zh-CN",
                        "Asia/Shanghai",
                        List.of("PluginA"),
                        "canvas",
                        "webgl",
                        "audio"
                ),
                new FingerprintRequest.DeviceFingerprint(
                        "macOS",
                        "x86_64",
                        5,
                        16,
                        8,
                        "2560x1600",
                        "24"
                ),
                new FingerprintRequest.NetworkFingerprint(
                        ip,
                        null,
                        "wifi",
                        120d,
                        20d,
                        "ISP",
                        Map.of()
                ),
                // geoLocation 已由后端自动查询，不再从前端传入
                new FingerprintRequest.CertificateFingerprint(
                        List.of("cert-1"),
                        List.of("pin-1")
                ),
                Instant.now(),
                Map.of("sessionId", "sess-1")
        );
    }

    private SearchHits<DeviceFingerprintDocument> emptySearchHits() {
        @SuppressWarnings("unchecked")
        SearchHits<DeviceFingerprintDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(hits.iterator()).thenReturn(List.<SearchHit<DeviceFingerprintDocument>>of().iterator());
        return hits;
    }
}
