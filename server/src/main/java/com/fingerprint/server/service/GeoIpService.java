package com.fingerprint.server.service;

import com.fingerprint.server.dto.GeoLocationInfo;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * GeoIP查询服务。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnBean(DatabaseReader.class)
public class GeoIpService {

    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);

    private final DatabaseReader databaseReader;

    /**
     * 根据IP地址查询地理位置信息。
     *
     * @param ipAddress IP地址
     * @return 地理位置信息，如果查询失败则返回Optional.empty()
     */
    @Cacheable(value = "geoIpCache", key = "#ipAddress", unless = "#result.isEmpty()")
    public Optional<GeoLocationInfo> lookup(String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            log.debug("IP address is blank, skipping lookup");
            return Optional.empty();
        }

        // 过滤本地IP和私有IP
        if (isLocalOrPrivateIp(ipAddress)) {
            log.debug("IP address {} is local or private, skipping lookup", ipAddress);
            return Optional.empty();
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            CityResponse response = databaseReader.city(inetAddress);

            GeoLocationInfo geoInfo = buildGeoLocationInfo(response);
            log.debug("GeoIP lookup successful for IP: {}, country: {}, city: {}", 
                    ipAddress, geoInfo.getCountryCode(), geoInfo.getCity());
            
            return Optional.of(geoInfo);

        } catch (AddressNotFoundException e) {
            log.debug("IP address {} not found in GeoIP database", ipAddress);
            return Optional.empty();
        } catch (UnknownHostException e) {
            log.warn("Invalid IP address format: {}", ipAddress);
            return Optional.empty();
        } catch (GeoIp2Exception e) {
            log.error("GeoIP lookup error for IP: {}", ipAddress, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error during GeoIP lookup for IP: {}", ipAddress, e);
            return Optional.empty();
        }
    }

    /**
     * 从CityResponse构建GeoLocationInfo对象。
     */
    private GeoLocationInfo buildGeoLocationInfo(CityResponse response) {
        Country country = response.getCountry();
        Subdivision subdivision = response.getMostSpecificSubdivision();
        City city = response.getCity();
        Postal postal = response.getPostal();
        Location location = response.getLocation();
        Continent continent = response.getContinent();
        Traits traits = response.getTraits();

        return GeoLocationInfo.builder()
                .countryCode(country.getIsoCode())
                .countryName(country.getName())
                .region(subdivision.getName())
                .city(city.getName())
                .postalCode(postal.getCode())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .timezone(location.getTimeZone())
                .continentCode(continent.getCode())
                .continentName(continent.getName())
                .isp(traits.getIsp())
                .organization(traits.getOrganization())
                .asn(traits.getAutonomousSystemNumber())
                .asOrganization(traits.getAutonomousSystemOrganization())
                .build();
    }

    /**
     * 判断是否为本地或私有IP地址。
     */
    private boolean isLocalOrPrivateIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return true;
        }

        // 移除可能的端口号
        if (ip.contains(":") && !ip.contains("::")) {
            ip = ip.substring(0, ip.indexOf(":"));
        }

        // 本地回环地址
        if (ip.startsWith("127.") || ip.equals("localhost") || ip.equals("::1")) {
            return true;
        }

        // 私有IP地址段
        // 10.0.0.0 - 10.255.255.255
        if (ip.startsWith("10.")) {
            return true;
        }

        // 172.16.0.0 - 172.31.255.255
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int secondOctet = Integer.parseInt(parts[1]);
                    if (secondOctet >= 16 && secondOctet <= 31) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }

        // 192.168.0.0 - 192.168.255.255
        if (ip.startsWith("192.168.")) {
            return true;
        }

        // IPv6 私有地址
        if (ip.startsWith("fc") || ip.startsWith("fd") || ip.startsWith("fe80")) {
            return true;
        }

        return false;
    }
}
