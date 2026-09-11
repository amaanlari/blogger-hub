import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { registerAuthBridge } from '@/shared/lib/auth-bridge';
import type { BlogUser } from '@/shared/types/models';

interface AuthState {
  currentUser: BlogUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  /** True once tokens have been rehydrated and the stored session checked against the server. */
  isBootstrapped: boolean;

  setSession: (tokens: { accessToken: string; refreshToken: string }) => void;
  setCurrentUser: (user: BlogUser | null) => void;
  setAccessToken: (accessToken: string) => void;
  setBootstrapped: (value: boolean) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      currentUser: null,
      accessToken: null,
      refreshToken: null,
      isBootstrapped: false,

      setSession: ({ accessToken, refreshToken }) => set({ accessToken, refreshToken }),
      setCurrentUser: (currentUser) => set({ currentUser }),
      setAccessToken: (accessToken) => set({ accessToken }),
      setBootstrapped: (isBootstrapped) => set({ isBootstrapped }),
      clear: () =>
        set({ currentUser: null, accessToken: null, refreshToken: null }),
    }),
    {
      name: 'blogger-hub-auth',
      // `isBootstrapped` is per-page-load state, not session state — persisting it would make a
      // reload skip the check that the restored token still works.
      partialize: ({ currentUser, accessToken, refreshToken }) => ({
        currentUser,
        accessToken,
        refreshToken,
      }),
    },
  ),
);

export function isAuthenticated(state: AuthState): boolean {
  return Boolean(state.accessToken);
}

export function hasRole(user: BlogUser | null, role: BlogUser['roles'][number]): boolean {
  return Boolean(user?.roles?.includes(role));
}

/**
 * Wires the store into the HTTP client. Called once at module load, before any request can fire.
 *
 * `onSessionExpired` clears tokens but deliberately does not navigate — routing is the router's
 * job, and `ProtectedRoute` will redirect on the next render anyway. Doing a hard
 * `window.location` here would throw away any unsaved work in the editor.
 */
registerAuthBridge({
  getTokens: () => {
    const { accessToken, refreshToken } = useAuthStore.getState();
    return { accessToken, refreshToken };
  },
  onAccessTokenRefreshed: (accessToken) => {
    useAuthStore.getState().setAccessToken(accessToken);
  },
  onSessionExpired: () => {
    useAuthStore.getState().clear();
  },
});
