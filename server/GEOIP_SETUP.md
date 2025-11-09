# GeoIP 配置指南

## 概述

本项目集成了 MaxMind GeoIP2 数据库来查询 IP 地址的地理位置信息。系统会自动从 HTTP 请求头中提取真实的客户端 IP 地址，并使用 GeoIP2 数据库进行查询。

## 功能特性

1. **自动获取真实IP地址**：支持从多个请求头中提取真实IP（X-Forwarded-For、X-Real-IP等）
2. **GeoIP查询**：使用 MaxMind GeoIP2 City 数据库查询 IP 的地理位置信息
3. **私有IP过滤**：自动过滤本地IP和私有IP地址
4. **缓存优化**：使用内存缓存提高查询性能
5. **可选配置**：可通过配置开关启用/禁用 GeoIP 功能

## 数据库文件获取

### 免费版本（GeoLite2）

1. 访问 MaxMind 官网注册账号：https://www.maxmind.com/en/geolite2/signup
2. 登录后，访问 https://www.maxmind.com/en/accounts/current/geoip/downloads
3. 下载 **GeoLite2-City** 数据库文件（MMDB 格式）
4. 将下载的 `GeoLite2-City.mmdb` 文件放置到以下位置之一：
   - `server/src/main/resources/GeoLite2-City.mmdb`
   - 自定义路径（需在配置文件中指定）

### 商业版本（GeoIP2）

如果需要更高精度的数据，可以购买商业版本：
- 访问：https://www.maxmind.com/en/geoip2-databases

## 配置说明

### application.yml 配置

```yaml
fingerprint:
  geoip:
    # 是否启用 GeoIP 查询功能
    enabled: true
    # 数据库文件路径（支持 classpath: 或绝对路径）
    database-path: classpath:GeoLite2-City.mmdb
    # 缓存大小（条目数）
    cache-size: 4096
```

### 环境变量配置

也可以通过环境变量覆盖配置：

```bash
# 启用/禁用 GeoIP
export FINGERPRINT_GEOIP_ENABLED=true

# 数据库文件路径
export FINGERPRINT_GEOIP_DB_PATH=/path/to/GeoLite2-City.mmdb

# 缓存大小
export FINGERPRINT_GEOIP_CACHE_SIZE=4096
```

## 查询的地理位置信息

系统会查询并存储以下地理位置信息：

- **国家**：国家代码和名称
- **地区/省份**：一级行政区划
- **城市**：城市名称
- **坐标**：纬度和经度
- **邮政编码**：邮政编码
- **时区**：时区信息
- **大洲**：大洲代码和名称
- **ISP信息**：互联网服务提供商
- **ASN信息**：自治系统编号和组织

## IP 地址提取

系统按以下优先级从 HTTP 请求头中提取真实 IP：

1. `X-Forwarded-For`
2. `X-Real-IP`
3. `Proxy-Client-IP`
4. `WL-Proxy-Client-IP`
5. `HTTP_X_FORWARDED_FOR`
6. `HTTP_X_FORWARDED`
7. `HTTP_X_CLUSTER_CLIENT_IP`
8. `HTTP_CLIENT_IP`
9. `HTTP_FORWARDED_FOR`
10. `HTTP_FORWARDED`
11. `HTTP_VIA`
12. `REMOTE_ADDR`
13. `request.getRemoteAddr()`

对于 `X-Forwarded-For` 包含多个 IP 的情况（格式：`client, proxy1, proxy2`），系统会自动提取第一个有效的客户端IP。

## 私有IP过滤

以下IP地址不会进行 GeoIP 查询：

- **本地回环**：127.0.0.1, localhost, ::1
- **私有IPv4**：
  - 10.0.0.0/8
  - 172.16.0.0/12
  - 192.168.0.0/16
- **私有IPv6**：fc00::/7, fe80::/10

## 性能优化

1. **内存缓存**：使用 Caffeine 缓存，默认缓存 10,000 条记录，24小时过期
2. **GeoIP2 内置缓存**：DatabaseReader 内置 CHM 缓存，默认 4,096 条目
3. **异步查询**：查询失败不会影响主流程，只记录日志

## 禁用 GeoIP 功能

如果不需要 GeoIP 功能，可以通过以下方式禁用：

### 方式 1：配置文件

```yaml
fingerprint:
  geoip:
    enabled: false
```

### 方式 2：环境变量

```bash
export FINGERPRINT_GEOIP_ENABLED=false
```

禁用后，系统仍会提取真实 IP 地址并存储到 `network.ipAddress` 字段，但不会进行地理位置查询。

## 数据库更新

MaxMind 每周二更新 GeoLite2 数据库。建议定期更新数据库文件以获取最新的地理位置数据：

### 手动更新

1. 下载最新的 GeoLite2-City.mmdb 文件
2. 替换现有文件
3. 重启应用

### 自动更新（推荐）

可以使用 MaxMind 提供的 `geoipupdate` 工具实现自动更新：

```bash
# 安装 geoipupdate
# Ubuntu/Debian
sudo add-apt-repository ppa:maxmind/ppa
sudo apt update
sudo apt install geoipupdate

# 配置 /etc/GeoIP.conf
AccountID YOUR_ACCOUNT_ID
LicenseKey YOUR_LICENSE_KEY
EditionIDs GeoLite2-City

# 定时任务（每周更新）
0 2 * * 2 /usr/bin/geoipupdate
```

## 故障排查

### 问题：数据库文件未找到

**错误信息**：
```
GeoIP database file not found: classpath:GeoLite2-City.mmdb
```

**解决方案**：
1. 确认数据库文件已下载并放置在正确位置
2. 检查配置文件中的路径是否正确
3. 如果使用 classpath:，确保文件在 resources 目录下

### 问题：查询返回空结果

**可能原因**：
1. IP 地址是私有IP或本地IP（已被过滤）
2. IP 地址不在 GeoIP2 数据库中
3. 数据库文件已损坏或过期

**解决方案**：
1. 查看日志确认 IP 地址是否被过滤
2. 使用公网 IP 进行测试
3. 更新数据库文件

### 问题：性能问题

**优化建议**：
1. 增大 GeoIP 缓存大小：`fingerprint.geoip.cache-size=8192`
2. 增大应用缓存大小和过期时间：`spring.cache.caffeine.spec=maximumSize=20000,expireAfterWrite=48h`
3. 考虑使用商业版 GeoIP2 数据库（体积更小，查询更快）

## 示例

### 查询结果示例

```json
{
  "geoLocation": {
    "country": "China",
    "region": "Beijing",
    "city": "Beijing",
    "latitude": 39.9042,
    "longitude": 116.4074,
    "timezone": "Asia/Shanghai",
    "extra": {
      "countryCode": "CN",
      "continentCode": "AS",
      "continentName": "Asia",
      "isp": "China Telecom",
      "organization": "China Telecom Beijing",
      "asn": 4134,
      "asOrganization": "Chinanet"
    }
  }
}
```

## 许可证

- **GeoLite2**: 免费使用，需遵守 [Creative Commons Attribution-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-sa/4.0/)
- **GeoIP2**: 商业许可，需购买
- **GeoIP2 Java API**: Apache License 2.0

## 参考链接

- MaxMind GeoIP2 官网：https://www.maxmind.com/
- GeoLite2 免费数据库：https://dev.maxmind.com/geoip/geolite2-free-geolocation-data
- GeoIP2 Java API：https://github.com/maxmind/GeoIP2-java
- GeoIP Update 工具：https://github.com/maxmind/geoipupdate
