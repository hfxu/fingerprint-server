import FingerprintJS from '@fingerprintjs/fingerprintjs';

/**
 * IP 信息解析结果。
 */
export interface IpResolution {
  ip: string;
  ipv6?: string;
  isp?: string;
  geo?: {
    country?: string;
    region?: string;
    city?: string;
    latitude?: number;
    longitude?: number;
    timezone?: string;
    extra?: Record<string, unknown>;
  };
  extra?: Record<string, unknown>;
}

/**
 * 证书指纹信息。
 */
export interface CertificateFingerprint {
  fingerprints: string[];
  pinningHashes?: string[];
}

/**
 * 后端响应结构。
 */
export interface FingerprintServerResponse {
  deviceId: string;
  matched: boolean;
  similarityScore: number;
  matchedDeviceId: string | null;
  matchedAt: string;
  indicators: Record<string, unknown>;
}

/**
 * Web SDK 初始化参数。
 */
export interface FingerprintClientOptions {
  backendEndpoint: string;
  timeoutMs?: number;
  headers?: Record<string, string>;
  apiKey?: string;
  loadOptions?: Parameters<typeof FingerprintJS.load>[0];
  fetchImplementation?: typeof fetch;
  ipResolver?: () => Promise<IpResolution | undefined>;
  certificateProvider?: () => Promise<CertificateFingerprint | undefined>;
  metadataProvider?: () => Promise<Record<string, unknown>>;
  fingerprintAgentProvider?: () => Promise<Awaited<ReturnType<typeof FingerprintJS.load>>>;
}

/**
 * FingerprintJS 采集结果类型。
 */
type FingerprintGetResult = Awaited<ReturnType<Awaited<ReturnType<typeof FingerprintJS.load>>['get']>>;

/**
 * 发送给服务端的指纹载荷。
 */
interface FingerprintServerPayload {
  visitorId: string;
  browser: {
    userAgent: string;
    language: string;
    timezone: string;
    plugins: string[];
    canvasFingerprint?: string;
    webglFingerprint?: string;
    audioFingerprint?: string;
  };
  device: {
    platform: string;
    architecture: string;
    touchPoints: number;
    deviceMemory: number;
    cpuCores: number;
    screenResolution: string;
    colorDepth: string;
  };
  network: {
    ipAddress: string;
    ipv6Address?: string;
    connectionType: string;
    downlinkMbps: number;
    rtt: number;
    isp?: string;
    extra?: Record<string, unknown>;
  };
  // geoLocation 已由后端自动查询，不再从前端传入
  certificate?: CertificateFingerprint;
  collectedAt: string;
  metadata?: Record<string, unknown>;
}

/**
 * Web SDK 主类，封装 FingerprintJS 并上报至后端。
 */
export class FingerprintClient {
  private readonly options: Required<Pick<FingerprintClientOptions, 'backendEndpoint'>> & FingerprintClientOptions;
  private readonly fpPromise: Promise<Awaited<ReturnType<typeof FingerprintJS.load>>>;
  private readonly fetchImplementation: typeof fetch;

  public constructor(options: FingerprintClientOptions) {
    if (!options.backendEndpoint) {
      throw new Error('backendEndpoint is required');
    }
    this.options = {
      timeoutMs: 5000,
      headers: {},
      ...options,
    } as FingerprintClient['options'];
    this.fetchImplementation = options.fetchImplementation ?? globalThis.fetch.bind(globalThis);
    this.fpPromise = options.fingerprintAgentProvider?.() ?? FingerprintJS.load(options.loadOptions);
  }

  /**
   * 采集指纹并推送到后端。
   */
  public async collect(metadata?: Record<string, unknown>): Promise<FingerprintServerResponse> {
    const agent = await this.fpPromise;
    const result = await agent.get();
    const ipResolution = await this.options.ipResolver?.();
    if (!ipResolution?.ip) {
      throw new Error('ipResolver must provide a valid public IP address');
    }
    const certificate = await this.options.certificateProvider?.();
    const dynamicMetadata = await this.options.metadataProvider?.();
    const payload: FingerprintServerPayload = {
      visitorId: result.visitorId,
      browser: this.buildBrowserFingerprint(result),
      device: this.buildDeviceFingerprint(),
      network: this.buildNetworkFingerprint(ipResolution),
      // geoLocation 已由后端自动查询，不再从前端传入
      certificate,
      collectedAt: new Date().toISOString(),
      metadata: {
        confidenceScore: result.confidence.score,
        ...(dynamicMetadata ?? {}),
        ...(metadata ?? {}),
      },
    };

    if (!payload.certificate) {
      delete payload.certificate;
    }

    return this.sendToServer(payload);
  }

  private async sendToServer(payload: FingerprintServerPayload): Promise<FingerprintServerResponse> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.options.timeoutMs);
    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        ...this.options.headers,
      };
      if (this.options.apiKey) {
        headers['X-API-Key'] = this.options.apiKey;
      }
      const response = await this.fetchImplementation(this.options.backendEndpoint, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
        signal: controller.signal,
      });
      if (!response.ok) {
        throw new Error(`Backend rejected fingerprint payload with status ${response.status}`);
      }
      return (await response.json()) as FingerprintServerResponse;
    } finally {
      clearTimeout(timeout);
    }
  }

  private buildBrowserFingerprint(result: FingerprintGetResult): FingerprintServerPayload['browser'] {
    const components = result.components as Record<string, { value: unknown }>;
    const canvasFingerprint = this.safeComponentValue(components, 'canvasFingerprint');
    const webglFingerprint = this.safeComponentValue(components, 'webGlBasics') ?? this.safeComponentValue(components, 'webgl');
    const audioFingerprint = this.safeComponentValue(components, 'audioFingerprint');
    return {
      userAgent: navigator.userAgent,
      language: navigator.language,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone ?? 'unknown',
      plugins: this.resolvePlugins(),
      canvasFingerprint: canvasFingerprint ?? undefined,
      webglFingerprint: webglFingerprint ?? undefined,
      audioFingerprint: audioFingerprint ?? undefined,
    };
  }

  private buildDeviceFingerprint(): FingerprintServerPayload['device'] {
    const screen = globalThis.screen;
    return {
      platform: navigator.platform,
      architecture: (navigator as unknown as { cpuClass?: string }).cpuClass ?? 'unknown',
      touchPoints: navigator.maxTouchPoints ?? 0,
      deviceMemory: Math.round(((navigator as unknown as { deviceMemory?: number }).deviceMemory ?? 0) * 1024) || 0,
      cpuCores: navigator.hardwareConcurrency ?? 0,
      screenResolution: screen ? `${screen.width}x${screen.height}` : 'unknown',
      colorDepth: screen ? `${screen.colorDepth}` : 'unknown',
    };
  }

  private buildNetworkFingerprint(ipResolution?: IpResolution): FingerprintServerPayload['network'] {
    const connection = (navigator as unknown as { connection?: NetworkInformation }).connection;
    return {
      ipAddress: ipResolution?.ip ?? '',
      ipv6Address: ipResolution?.ipv6,
      connectionType: connection?.effectiveType ?? 'unknown',
      downlinkMbps: connection?.downlink ?? 0,
      rtt: connection?.rtt ?? 0,
      isp: ipResolution?.isp,
      extra: {
        saveData: connection?.saveData,
        ...ipResolution?.extra,
      },
    };
  }

  private resolvePlugins(): string[] {
    if (!navigator.plugins) {
      return [];
    }
    return Array.from(navigator.plugins).map((plugin) => plugin.name);
  }

  private safeComponentValue(components: Record<string, { value: unknown }>, key: string): string | undefined {
    const component = components[key];
    if (!component) {
      return undefined;
    }
    const { value } = component;
    if (typeof value === 'string') {
      return value;
    }
    if (value && typeof value === 'object' && 'checksum' in (value as Record<string, unknown>)) {
      const checksum = (value as Record<string, unknown>).checksum;
      return typeof checksum === 'string' ? checksum : undefined;
    }
    return undefined;
  }
}
