import type { PropsWithChildren } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { FullPageSpinner } from '@/shared/components/FullPageSpinner';

export function ProtectedRoute({ children }: PropsWithChildren) {
  const { isAuthenticated, isBootstrapped } = useAuth();
  const location = useLocation();

  // Without this gate a reload on a protected route would bounce to /login for a frame before the
  // persisted token finished rehydrating, losing the user's place.
  if (!isBootstrapped) return <FullPageSpinner />;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}
