# Web SDK

基于 [FingerprintJS](https://github.com/fingerprintjs/fingerprintjs) 的浏览器端封装，用于采集设备指纹并上报至后端服务。

## 安装

```bash
npm install @fingerprint-solution/web-sdk
```

或使用 pnpm：

```bash
pnpm add @fingerprint-solution/web-sdk
```

## 快速开始

```ts
import { FingerprintClient } from '@fingerprint-solution/web-sdk';

const client = new FingerprintClient({
  backendEndpoint: 'https://api.example.com/api/v1/fingerprints',
  apiKey: 'optional-api-key',
  ipResolver: async () => {
    const response = await fetch('https://ipinfo.io/json');
    const data = await response.json();
    return {
      ip: data.ip,
      isp: data.org,
      // 注意：geoLocation 已由后端基于 IP 地址自动查询，不需要从前端传入
    };
  },
});

const result = await client.collect({
  sessionId: 'session-id-from-app',
});
console.log(result);
```

## API 说明

- `backendEndpoint`：后端 Spring Boot 服务 `POST /api/v1/fingerprints` 地址。
- `timeoutMs`：上报请求超时时间，默认 5000ms。
- `headers`：自定义请求头。
- `apiKey`：可选的鉴权 key，将作为 `X-API-Key` 发送。
- `loadOptions`：传递给 `FingerprintJS.load` 的配置。
- `fetchImplementation`：自定义 `fetch` 实现（例如跨平台 polyfill）。
- `ipResolver`：异步解析公网 IP 和 ISP（地理位置由后端自动查询）。
- `certificateProvider`：提供 TLS 证书指纹信息。
- `metadataProvider`：补充元数据（如用户上下文）。
- `fingerprintAgentProvider`：自定义 FingerprintJS Agent 创建逻辑。

## 与后端字段对应

SDK 会按以下结构组织上报数据：

- `browser`：浏览器 UA、语言、时区、插件列表、Canvas/WebGL/Audio 指纹。
- `device`：平台、架构、触控点、内存、CPU 核心数、屏幕分辨率、色深。
- `network`：公网/IPv6 IP、网络类型、带宽、RTT、ISP、额外指标。
- `certificate`：TLS 证书指纹与 Pinning Hash。
- `metadata`：包含 FingerprintJS 信心分值及自定义元数据。

**注意**：`geoLocation` 字段已由后端服务基于 IP 地址自动查询（使用 GeoIP 数据库），无需从前端传入。

## 开发构建

```bash
npm install
npm run build
```

输出产物位于 `dist/` 目录，可直接供前端应用引用。
