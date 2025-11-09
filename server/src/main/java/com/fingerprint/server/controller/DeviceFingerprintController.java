package com.fingerprint.server.controller;

import com.fingerprint.server.dto.FingerprintRequest;
import com.fingerprint.server.dto.FingerprintResponse;
import com.fingerprint.server.service.DeviceFingerprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备指纹采集控制器。
 */
@RestController
@RequestMapping("/api/v1/fingerprints")
@RequiredArgsConstructor
@Tag(name = "Device Fingerprint", description = "设备指纹采集与匹配接口")
public class DeviceFingerprintController {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintController.class);

    private final DeviceFingerprintService deviceFingerprintService;

    /**
     * 接收客户端指纹数据并进行匹配。
     */
    @PostMapping
    @Operation(summary = "采集并匹配设备指纹")
    public ResponseEntity<FingerprintResponse> collect(@Valid @RequestBody FingerprintRequest request) {
        log.info("Received fingerprint request for visitorId={}", request.visitorId());
        FingerprintResponse response = deviceFingerprintService.handleFingerprint(request);
        HttpStatus status = response.matched() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}
