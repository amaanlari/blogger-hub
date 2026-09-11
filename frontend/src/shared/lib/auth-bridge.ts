/**
 * A one-way hatch letting the HTTP client read and refresh tokens without importing the auth store.
 *
 * The client needs the current access token on every request and needs to publish a new one after a
 * refresh, but `shared/` importing from `features/auth/` would invert the dependency direction and
 * create an import cycle (the auth feature's API module imports the client). The auth store
 * registers itself here once at startup instead.
 */

export interface AuthTokens {
  accessToken: string | null;
  refreshToken: string | null;
}

interface AuthBridge {
  getTokens: () => AuthTokens;
  /** Called after a successful silent refresh, with the newly issued access token. */
  onAccessTokenRefreshed: (accessToken: string) => void;
  /** Called when the refresh token itself is dead and the user must log in again. */
  onSessionExpired: () => void;
}

const noop: AuthBridge = {
  getTokens: () => ({ accessToken: null, refreshToken: null }),
  onAccessTokenRefreshed: () => {},
  onSessionExpired: () => {},
};

let bridge: AuthBridge = noop;

export function registerAuthBridge(next: AuthBridge): void {
  bridge = next;
}

export function getTokens(): AuthTokens {
  return bridge.getTokens();
}

export function publishRefreshedAccessToken(accessToken: string): void {
  bridge.onAccessTokenRefreshed(accessToken);
}

export function publishSessionExpired(): void {
  bridge.onSessionExpired();
}
