import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/features/auth/api/authApi';
import { useAuthStore } from '@/features/auth/store/authStore';

/**
 * Signs the user out.
 *
 * The server call only deletes the refresh-token row — this API has no way to revoke an access
 * token, so the already-issued one stays valid until it expires on its own. Clearing local state is
 * therefore the part that actually ends the session, and it happens whether or not the server call
 * succeeds: a failed logout must never strand someone in a half-signed-in state.
 */
export function useLogout() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return async function logout() {
    const { refreshToken, clear } = useAuthStore.getState();

    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
      // Already-invalid token, or the server is unreachable. Either way, sign out locally.
    } finally {
      clear();
      queryClient.clear();
      navigate('/', { replace: true });
    }
  };
}
