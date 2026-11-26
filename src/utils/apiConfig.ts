export const DEFAULT_NATIVE_API_BASE_URL =
  'https://ais-pre-kva3g2btcikpfqxb6q42ye-1009424168425.asia-east1.run.app';

export interface ResolveApiUrlOptions {
  isNative?: boolean;
  configuredBaseUrl?: string;
  envBaseUrl?: string;
  defaultNativeBaseUrl?: string;
}

export function normalizeBaseUrl(url: string): string {
  return url.trim().replace(/\/+$/, '');
}

export function resolveApiUrl(
  path: string,
  options: ResolveApiUrlOptions = {}
): string {
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  const isNative = options.isNative ?? false;
  const configured = normalizeBaseUrl(options.configuredBaseUrl ?? '');
  if (configured) {
    return `${configured}${cleanPath}`;
  }

  const envUrl = normalizeBaseUrl(options.envBaseUrl ?? '');
  if (envUrl) {
    return `${envUrl}${cleanPath}`;
  }

  if (isNative) {
    const fallback = normalizeBaseUrl(
      options.defaultNativeBaseUrl ?? DEFAULT_NATIVE_API_BASE_URL
    );
    return `${fallback}${cleanPath}`;
  }

  return cleanPath;
}
