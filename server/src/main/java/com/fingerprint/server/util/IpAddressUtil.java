package com.fingerprint.server.util;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IP地址工具类。
 */
public class IpAddressUtil {

    private static final Logger log = LoggerFactory.getLogger(IpAddressUtil.class);

    private static final String UNKNOWN = "unknown";

    /**
     * 常见的代理请求头，按优先级排序
     */
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    private IpAddressUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 从HttpServletRequest中获取真实的客户端IP地址。
     * 支持通过代理、负载均衡器等转发的情况。
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址，如果无法获取则返回null
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            log.warn("HttpServletRequest is null, cannot extract IP address");
            return null;
        }

        String ip = null;

        // 按优先级尝试从各个请求头获取IP
        for (String header : IP_HEADER_CANDIDATES) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                log.debug("Found IP address from header {}: {}", header, ip);
                break;
            }
        }

        // 如果所有header都没有，使用getRemoteAddr()
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
            log.debug("Using RemoteAddr as IP: {}", ip);
        }

        // 处理X-Forwarded-For可能包含多个IP的情况（格式：client, proxy1, proxy2）
        if (StringUtils.isNotBlank(ip) && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String candidateIp : ips) {
                String trimmedIp = candidateIp.trim();
                if (isValidIp(trimmedIp)) {
                    ip = trimmedIp;
                    log.debug("Extracted first valid IP from comma-separated list: {}", ip);
                    break;
                }
            }
        }

        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            log.warn("Could not determine client IP address from request");
            return null;
        }

        log.debug("Final extracted IP address: {}", ip);
        return ip;
    }

    /**
     * 验证IP地址是否有效。
     * 
     * @param ip IP地址字符串
     * @return 是否为有效IP
     */
    private static boolean isValidIp(String ip) {
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }

        // 基本格式验证
        ip = ip.trim();

        // IPv4格式验证
        if (isValidIpv4(ip)) {
            return true;
        }

        // IPv6格式验证（简单检查）
        if (isValidIpv6(ip)) {
            return true;
        }

        return false;
    }

    /**
     * 验证IPv4地址格式。
     */
    private static boolean isValidIpv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 验证IPv6地址格式（简单验证）。
     */
    private static boolean isValidIpv6(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // 简单检查：IPv6包含冒号
        return ip.contains(":");
    }
}
