package com.fingerprint.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IP地理位置信息DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoLocationInfo {

    /**
     * 国家代码 (例如: CN, US)
     */
    private String countryCode;

    /**
     * 国家名称
     */
    private String countryName;

    /**
     * 省份/州
     */
    private String region;

    /**
     * 城市
     */
    private String city;

    /**
     * 邮政编码
     */
    private String postalCode;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 大洲代码
     */
    private String continentCode;

    /**
     * 大洲名称
     */
    private String continentName;

    /**
     * ISP信息
     */
    private String isp;

    /**
     * 组织信息
     */
    private String organization;

    /**
     * ASN (Autonomous System Number)
     */
    private Integer asn;

    /**
     * AS组织
     */
    private String asOrganization;
}
