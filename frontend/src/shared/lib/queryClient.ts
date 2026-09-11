import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '@/shared/lib/api-response';

/**
 * Retrying is almost always wrong against this API.
 *
 * A 500 here usually is not a transient server fault — it is how the backend reports a wrong
 * password, a duplicate signup, a dead refresh token or a denied role, because those exceptions
 * are not registered in `GlobalExceptionHandler`. Retrying those just repeats a decided outcome,
 * and on login it triples the rate at which a user gets locked into confusing behaviour.
 *
 * So: only a genuine network failure is worth a second attempt.
 */
function retryOnlyNetworkFailures(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.kind === 'network') return failureCount < 2;
  return false;
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      retry: retryOnlyNetworkFailures,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
});
