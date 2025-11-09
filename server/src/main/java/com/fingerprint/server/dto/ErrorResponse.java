package com.fingerprint.server.dto;

import java.time.Instant;
import java.util.List;

/**
 * 统一错误响应结构。
 */
public record ErrorResponse(
        Instant timestamp,
        String path,
        String error,
        String message,
        List<String> details
) {
}
