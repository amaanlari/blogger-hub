import axios, {
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';
import { ApiError, readError, unwrapResponse } from '@/shared/lib/api-response';
import {
  getTokens,
  publishRefreshedAccessToken,
  publishSessionExpired,
} from '@/shared/lib/auth-bridge';

/**
 * Relative by default, and that default is the one you want.
 *
 * In development Vite proxies `/api` to :8080, and in production the SPA is served from the backend
 * JAR — so the API is same-origin either way. Going cross-origin exposes two live CORS defects in
 * `SecurityConfig` (allowedOrigins `*` combined with allowCredentials, and PATCH missing from
 * allowedMethods), which is why `.env.example` documents the override as a last resort rather than
 * a normal configuration step.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/** Endpoints that take the refresh token in the body and must never carry a bearer header. */
function isAuthEndpoint(url: string | undefined): boolean {
  return Boolean(url?.startsWith('/auth/'));
}

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
}

export const http = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

/**
 * A second, deliberately un-intercepted instance used only to refresh.
 *
 * Refreshing through `http` would re-enter the 401 handler below on failure and recurse.
 *
 * Exported so tests can swap its adapter; nothing in the app should call it directly.
 */
export const refreshClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

http.interceptors.request.use((config) => {
  const { accessToken } = getTokens();
  if (accessToken && !isAuthEndpoint(config.url)) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

/**
 * Shared across every request that hits a 401 at the same moment, so a burst of parallel calls
 * triggers exactly one refresh rather than one each.
 */
let inFlightRefresh: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (inFlightRefresh) return inFlightRefresh;

  inFlightRefresh = (async () => {
    const { refreshToken } = getTokens();
    if (!refreshToken) {
      throw new ApiError({ kind: 'unauthenticated', message: 'No refresh token' });
    }

    // `/auth/access-token`, not `/auth/refresh-token`. The latter rotates: it deletes the old
    // RefreshToken row server-side, so two refreshes racing each other would each invalidate the
    // other's token and log the user out. This one issues a new access token and leaves the
    // refresh token alone, which makes concurrent calls harmless.
    const response = await refreshClient.post('/auth/access-token', {
      refresh_token: refreshToken,
    });

    // Single-wrapped: this endpoint returns a bare TokenResponseDto, one level shallower than
    // every DataResponse endpoint.
    const tokens = unwrapResponse<{ access_token: string }>(response.data);
    if (!tokens?.access_token) {
      throw new ApiError({ kind: 'unauthenticated', message: 'Refresh returned no token' });
    }

    publishRefreshedAccessToken(tokens.access_token);
    return tokens.access_token;
  })();

  try {
    return await inFlightRefresh;
  } finally {
    inFlightRefresh = null;
  }
}

http.interceptors.response.use(
  (response) => {
    // Unwrap exactly once, here. Feature code reads `response.data` and gets the real payload.
    response.data = unwrapResponse(response.data);
    return response;
  },
  async (error: unknown) => {
    // `axios.isAxiosError`, not `instanceof AxiosError`: a project can end up with more than one
    // copy of axios in its tree, and an identity check silently misclassifies errors thrown by the
    // other copy as network failures.
    if (!axios.isAxiosError(error)) {
      return Promise.reject(
        new ApiError({ kind: 'network', message: 'Unexpected error' }),
      );
    }

    if (!error.response) {
      return Promise.reject(
        new ApiError({ kind: 'network', message: 'Could not reach the server.' }),
      );
    }

    const apiError =
      readError(error.response.data) ??
      new ApiError({
        kind: 'exception',
        message: error.message || 'Request failed',
        status: error.response.status,
      });

    const config = error.config as RetryableConfig | undefined;

    // Only a Shape C body means "your token is missing or expired". A Shape B 401 is a business
    // outcome — an unverified account at login, or an ownership mismatch on a /users/{id} route —
    // and refreshing on those would loop forever without ever fixing anything.
    const shouldRefresh =
      apiError.kind === 'unauthenticated' &&
      config &&
      !config._retried &&
      !isAuthEndpoint(config.url);

    if (shouldRefresh) {
      config._retried = true;
      try {
        const accessToken = await refreshAccessToken();
        config.headers.Authorization = `Bearer ${accessToken}`;
        return http(config);
      } catch {
        // A dead refresh token comes back as a 500 Shape A carrying "Invalid token", not a 401 —
        // so this catch, not a status check, is what ends the session.
        publishSessionExpired();
        return Promise.reject(
          new ApiError({ kind: 'unauthenticated', message: 'Your session has expired.' }),
        );
      }
    }

    return Promise.reject(apiError);
  },
);

/** Thin helpers so feature code never handles an axios response object directly. */
export const api = {
  get: async <T>(url: string, config?: AxiosRequestConfig): Promise<T> =>
    (await http.get<T>(url, config)).data,

  post: async <T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> =>
    (await http.post<T>(url, body, config)).data,

  put: async <T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> =>
    (await http.put<T>(url, body, config)).data,

  patch: async <T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> =>
    (await http.patch<T>(url, body, config)).data,

  delete: async <T>(url: string, config?: AxiosRequestConfig): Promise<T> =>
    (await http.delete<T>(url, config)).data,
};
