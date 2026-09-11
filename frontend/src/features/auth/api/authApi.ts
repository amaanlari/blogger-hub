import { api } from '@/shared/lib/http';
import type { BlogUser, TokenResponse } from '@/shared/types/models';

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  bio?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export const authApi = {
  signup: (body: SignupRequest) => api.post<TokenResponse>('/auth/signup', body),

  login: (body: LoginRequest) => api.post<TokenResponse>('/auth/login', body),

  /** Verifies the emailed OTP. The account is usable before this; only login is gated on it. */
  verifyOtp: (body: { email: string; otp: string }) =>
    api.post<void>('/auth/verify-otp', body),

  /**
   * Invalidates one refresh token server-side.
   *
   * The access token is *not* revoked — nothing in this API can revoke one — so the client must
   * also drop its own copy for logout to mean anything.
   */
  logout: (refreshToken: string) =>
    api.post<void>('/auth/logout', { refresh_token: refreshToken }),

  logoutAll: (refreshToken: string) =>
    api.post<void>('/auth/logout-all', { refresh_token: refreshToken }),

  /** Self-only; the ID must match the token's subject or the server answers 401. */
  getCurrentUser: (userId: string) => api.get<BlogUser>(`/users/id/${userId}`),
};

/**
 * Reads the user ID out of an access token without verifying it.
 *
 * Needed because the access token carries only `sub`, `iat` and `exp` — no roles, no username — so
 * the client cannot tell a premium user from a free one without a follow-up lookup, and needs the
 * subject to make that call. Verification is the server's job; this is only used to decide which
 * URL to fetch.
 */
export function readUserIdFromToken(accessToken: string): string | null {
  try {
    const payload = accessToken.split('.')[1];
    if (!payload) return null;
    const normalised = payload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = JSON.parse(atob(normalised)) as { sub?: string };
    return decoded.sub ?? null;
  } catch {
    return null;
  }
}
