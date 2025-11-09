package com.fingerprint.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fingerprint.server.dto.FingerprintRequest;
import com.fingerprint.server.dto.FingerprintResponse;
import com.fingerprint.server.service.DeviceFingerprintService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 设备指纹控制器测试。
 */
@WebMvcTest(controllers = DeviceFingerprintController.class)
class DeviceFingerprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeviceFingerprintService deviceFingerprintService;

    @Test
    void shouldReturnCreatedWhenNewDevice() throws Exception {
        FingerprintResponse response = new FingerprintResponse("device-new", false, 0d, null, Instant.now(), Map.of());
        when(deviceFingerprintService.handleFingerprint(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/fingerprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnOkWhenMatched() throws Exception {
        FingerprintResponse response = new FingerprintResponse("device-existing", true, 0.98d, "device-existing", Instant.now(), Map.of());
        when(deviceFingerprintService.handleFingerprint(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/fingerprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());
    }

    private FingerprintRequest buildRequest() {
        return new FingerprintRequest(
                "visitor-xyz",
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
                        "1.1.1.1",
                        null,
                        "wifi",
                        120d,
                        20d,
                        "ISP",
                        Map.of()
                ),
                new FingerprintRequest.GeoLocation(
                        "CN",
                        "SH",
                        "Shanghai",
                        31.2304,
                        121.4737,
                        "Asia/Shanghai",
                        Map.of()
                ),
                new FingerprintRequest.CertificateFingerprint(
                        List.of("cert-1"),
                        List.of("pin-1")
                ),
                Instant.now(),
                Map.of()
        );
    }
}
