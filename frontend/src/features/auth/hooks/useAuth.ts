import { useEffect } from 'react';
import { useAuthStore } from '@/features/auth/store/authStore';
import { authApi, readUserIdFromToken } from '@/features/auth/api/authApi';
import type { Role } from '@/shared/types/models';

export function useAuth() {
  const currentUser = useAuthStore((s) => s.currentUser);
  const accessToken = useAuthStore((s) => s.accessToken);
  const isBootstrapped = useAuthStore((s) => s.isBootstrapped);

  return {
    currentUser,
    isAuthenticated: Boolean(accessToken),
    isBootstrapped,
    hasRole: (role: Role) => Boolean(currentUser?.roles?.includes(role)),
  };
}

/**
 * Restores the signed-in user on a cold start.
 *
 * Tokens survive a reload in localStorage, but the user object behind them may not still be valid —
 * so rather than trusting what was persisted, this refetches it. That doubles as a liveness check:
 * if the stored access token has expired, the HTTP client silently refreshes it here, and if the
 * refresh token is dead too the session is cleared before any page renders as signed-in.
 *
 * The access token carries no username or roles (only `sub`, `iat`, `exp`), so this lookup is the
 * only way to know whether the viewer is a premium user.
 */
export function useBootstrapAuth() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const isBootstrapped = useAuthStore((s) => s.isBootstrapped);
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser);
  const setBootstrapped = useAuthStore((s) => s.setBootstrapped);
  const clear = useAuthStore((s) => s.clear);

  useEffect(() => {
    if (isBootstrapped) return;

    if (!accessToken) {
      setBootstrapped(true);
      return;
    }

    const userId = readUserIdFromToken(accessToken);
    if (!userId) {
      clear();
      setBootstrapped(true);
      return;
    }

    let cancelled = false;
    authApi
      .getCurrentUser(userId)
      .then((user) => {
        if (!cancelled) setCurrentUser(user);
      })
      .catch(() => {
        if (!cancelled) clear();
      })
      .finally(() => {
        if (!cancelled) setBootstrapped(true);
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, isBootstrapped, setCurrentUser, setBootstrapped, clear]);
}
