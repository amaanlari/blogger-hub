import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { AxiosHeaders } from 'axios';
import { api, http, refreshClient } from '@/shared/lib/http';
import { ApiError } from '@/shared/lib/api-response';
import { registerAuthBridge } from '@/shared/lib/auth-bridge';

/** Builds an axios response the adapter can resolve or reject with. */
function reply(
  config: InternalAxiosRequestConfig,
  status: number,
  data: unknown,
): AxiosResponse {
  return {
    data,
    status,
    statusText: '',
    headers: new AxiosHeaders(),
    config,
  };
}

const SHAPE_C_401 = {
  timestamp: '2026-09-09T21:08:21.338230623',
  data: {
    timestamp: '2026-09-09T15:38:21.337+00:00',
    status: 401,
    error: 'Unauthorized',
    path: '/api/notifications',
  },
  error: null,
};

const OK = (payload: unknown) => ({
  timestamp: '2026-09-09T12:34:56.789',
  data: { success: true, status_code: 200, message: 'ok', data: payload },
  error: null,
});

const REFRESHED = {
  timestamp: '2026-09-09T12:34:56.789',
  data: { user_id: 'u1', access_token: 'fresh-token', refresh_token: 'r1' },
  error: null,
};

let tokens = { accessToken: 'stale-token', refreshToken: 'r1' };
let sessionExpired = false;

beforeEach(() => {
  tokens = { accessToken: 'stale-token', refreshToken: 'r1' };
  sessionExpired = false;
  registerAuthBridge({
    getTokens: () => tokens,
    onAccessTokenRefreshed: (accessToken) => {
      tokens = { ...tokens, accessToken };
    },
    onSessionExpired: () => {
      sessionExpired = true;
    },
  });
});

describe('silent refresh on an expired access token', () => {
  it('refreshes once and retries the original request with the new token', async () => {
    const seen: string[] = [];

    http.defaults.adapter = (async (config: InternalAxiosRequestConfig) => {
      const auth = config.headers.Authorization as string | undefined;
      seen.push(auth ?? 'none');
      if (auth === 'Bearer stale-token') {
        throw Object.assign(new Error('401'), {
          isAxiosError: true,
          config,
          response: reply(config, 401, SHAPE_C_401),
        });
      }
      return reply(config, 200, OK({ count: 3 }));
    }) as AxiosAdapter;

    refreshClient.defaults.adapter = (async (config: InternalAxiosRequestConfig) =>
      reply(config, 200, REFRESHED)) as AxiosAdapter;

    const result = await api.get<{ count: number }>('/notifications/unread-count');

    expect(result).toEqual({ count: 3 });
    expect(seen).toEqual(['Bearer stale-token', 'Bearer fresh-token']);
    expect(sessionExpired).toBe(false);
  });

  it('refreshes only once when several requests hit 401 together', async () => {
    const refreshCalls = vi.fn();

    http.defaults.adapter = (async (config: InternalAxiosRequestConfig) => {
      if (config.headers.Authorization === 'Bearer stale-token') {
        throw Object.assign(new Error('401'), {
          isAxiosError: true,
          config,
          response: reply(config, 401, SHAPE_C_401),
        });
      }
      return reply(config, 200, OK({ ok: true }));
    }) as AxiosAdapter;

    refreshClient.defaults.adapter = (async (config: InternalAxiosRequestConfig) => {
      refreshCalls();
      // A real refresh is not instantaneous; without the single-flight guard each of the three
      // callers below would start its own while this one is still in the air.
      await new Promise((resolve) => setTimeout(resolve, 10));
      return reply(config, 200, REFRESHED);
    }) as AxiosAdapter;

    await Promise.all([
      api.get('/notifications'),
      api.get('/blogposts'),
      api.get('/users/lookup?ids=a'),
    ]);

    expect(refreshCalls).toHaveBeenCalledTimes(1);
  });

  it('ends the session when the refresh token is dead', async () => {
    http.defaults.adapter = (async (config: InternalAxiosRequestConfig) => {
      throw Object.assign(new Error('401'), {
        isAxiosError: true,
        config,
        response: reply(config, 401, SHAPE_C_401),
      });
    }) as AxiosAdapter;

    // A dead refresh token is a 500 Shape A carrying "Invalid token" — not a 401. If the client
    // keyed its hard-logout off a 401 status it would never fire here.
    refreshClient.defaults.adapter = (async (config: InternalAxiosRequestConfig) => {
      throw Object.assign(new Error('500'), {
        isAxiosError: true,
        config,
        response: reply(config, 500, {
          timestamp: '2026-09-09T12:00:00.123',
          data: null,
          error: { status: 'INTERNAL_SERVER_ERROR', message: 'Invalid token' },
        }),
      });
    }) as AxiosAdapter;

    await expect(api.get('/notifications')).rejects.toBeInstanceOf(ApiError);
    expect(sessionExpired).toBe(true);
  });
});

describe('401s that are not token problems', () => {
  it('does not refresh on a Shape B 401 from an ownership check', async () => {
    const refreshCalls = vi.fn();

    http.defaults.adapter = (async (config: InternalAxiosRequestConfig) => {
      throw Object.assign(new Error('401'), {
        isAxiosError: true,
        config,
        response: reply(config, 401, {
          timestamp: '2026-09-09T12:00:00.123',
          data: {
            success: false,
            status_code: 401,
            message: "You are not authorized to access this user's profile",
            error: null,
          },
          error: null,
        }),
      });
    }) as AxiosAdapter;

    refreshClient.defaults.adapter = (async (config: InternalAxiosRequestConfig) => {
      refreshCalls();
      return reply(config, 200, REFRESHED);
    }) as AxiosAdapter;

    await expect(api.get('/users/id/someone-else')).rejects.toMatchObject({
      kind: 'business',
      status: 401,
    });
    expect(refreshCalls).not.toHaveBeenCalled();
    expect(sessionExpired).toBe(false);
  });

  it('surfaces a login failure as an exception error without touching the session', async () => {
    http.defaults.adapter = (async (config: InternalAxiosRequestConfig) => {
      throw Object.assign(new Error('500'), {
        isAxiosError: true,
        config,
        response: reply(config, 500, {
          timestamp: '2026-09-09T12:00:00.123',
          data: null,
          error: { status: 'INTERNAL_SERVER_ERROR', message: 'Invalid credentials' },
        }),
      });
    }) as AxiosAdapter;

    await expect(
      api.post('/auth/login', { username: 'jane', password: 'wrong' }),
    ).rejects.toMatchObject({ kind: 'exception', message: 'Invalid credentials' });
    expect(sessionExpired).toBe(false);
  });
});

describe('request headers', () => {
  it('never attaches a bearer token to an auth endpoint', async () => {
    let seenAuth: unknown = 'unset';

    http.defaults.adapter = (async (config: InternalAxiosRequestConfig) => {
      seenAuth = config.headers.Authorization;
      return reply(config, 200, OK({}));
    }) as AxiosAdapter;

    await api.post('/auth/logout', { refresh_token: 'r1' });

    expect(seenAuth).toBeUndefined();
  });
});
