# 设备指纹识别平台

该项目实现了 **Web 端设备指纹采集与识别** 方案：

- 客户端（`web-sdk/`）封装 [FingerprintJS](https://github.com/fingerprintjs/fingerprintjs)，采集浏览器、网络、地理位置及证书等多维度特征，并将数据上报至后端。
- 后端（`server/`）基于 Java 17 + Spring Boot 3.2 + ElasticSearch，构建指纹文档索引，通过加权相似度算法（默认阈值 0.95）判定是否为同一设备。

## 项目结构

```
workspace/
├── server/         # Spring Boot 后端服务
│   ├── pom.xml
│   └── src/
└── web-sdk/        # 浏览器端 SDK 封装
    ├── package.json
    └── src/
```

## 后端能力概览

- REST API：`POST /api/v1/fingerprints`，接收 FingerprintJS 衍生数据并返回匹配结果。
- ElasticSearch 文档结构包含浏览器、硬件、网络、IP 地理位置、TLS 证书及自定义元数据。
- `SimilarityScorer` 根据 visitorId、浏览器、设备、网络、地理位置、证书指纹分配权重，默认阈值 0.95 可通过配置覆盖。
- Springdoc OpenAPI 自动生成接口文档，Actuator 暴露健康检查。
- 全局异常处理与 Bean Validation 保证输入合法性。

## 快速启动（后端）

> 需准备 Maven 3.9+、JDK 17+，ElasticSearch 8.x （或兼容版本）。

```bash
cd server
mvn spring-boot:run
```

常用配置（`application.yml`）：

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
fingerprint:
  similarity:
    threshold: 0.95   # 判定同一设备的最低相似度
```

ElasticSearch 使用 `device_fingerprints` 索引，可按需调整分片、副本或 mapping。

### API 请求示例

```http
POST /api/v1/fingerprints HTTP/1.1
Content-Type: application/json

{
  "visitorId": "visitor-123",
  "browser": {
    "userAgent": "Mozilla/5.0",
    "language": "zh-CN",
    "timezone": "Asia/Shanghai",
    "plugins": ["PluginA"],
    "canvasFingerprint": "canvas",
    "webglFingerprint": "webgl",
    "audioFingerprint": "audio"
  },
  "device": {
    "platform": "macOS",
    "architecture": "x86_64",
    "touchPoints": 5,
    "deviceMemory": 16384,
    "cpuCores": 8,
    "screenResolution": "2560x1600",
    "colorDepth": "24"
  },
  "network": {
    "ipAddress": "1.1.1.1",
    "connectionType": "wifi",
    "downlinkMbps": 120,
    "rtt": 20
  },
  "geoLocation": {
    "country": "CN",
    "city": "Shanghai"
  },
  "certificate": {
    "fingerprints": ["cert-1"],
    "pinningHashes": ["pin-1"]
  },
  "collectedAt": "2025-11-09T08:00:00Z",
  "metadata": {
    "sessionId": "sess-1"
  }
}
```

响应：

```json
{
  "deviceId": "device-existing",
  "matched": true,
  "similarityScore": 0.97,
  "matchedDeviceId": "device-existing",
  "matchedAt": "2025-11-09T08:00:00Z",
  "indicators": {
    "observationCount": 6,
    "ipHistorySize": 4
  }
}
```

## Web SDK 使用

详见 `web-sdk/README.md`，示例：

```ts
import { FingerprintClient } from '@fingerprint-solution/web-sdk';

const client = new FingerprintClient({
  backendEndpoint: 'https://api.example.com/api/v1/fingerprints',
  ipResolver: async () => {
    const data = await fetch('https://ipapi.co/json').then((res) => res.json());
    return {
      ip: data.ip,
      isp: data.org,
      geo: {
        country: data.country,
        region: data.region,
        city: data.city,
        latitude: Number(data.latitude),
        longitude: Number(data.longitude),
        timezone: data.timezone,
      },
    };
  },
});

const response = await client.collect({ sessionId: 'sess-1' });
```

## 配置与扩展

- `fingerprint.similarity.*`：权重与阈值，合计应为 1（默认：visitor 0.35、browser & device 各 0.2、network 0.1、geo 0.1、certificate 0.05）。
- `fingerprint.elasticsearch.*`：ES 客户端连接超时、连接池上限等参数。
- 可通过实现 `certificateProvider` 获取 TLS 指纹（例如与 WebSocket 握手配合）。

## 测试

后端包含核心服务、控制层与相似度计算的单元测试：

```bash
cd server
mvn test
```

> 当前运行环境缺少 Maven，可在本地安装后执行上述命令。

## 后续规划

- Android / iOS SDK：复用相似度权重模型，构建端侧封装。
- 指纹策略中心：引入黑名单、风险评分及告警流水。
- 历史轨迹审计：结合时间窗口统计、可视化分析。
