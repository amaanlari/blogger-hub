import { Navigate, useParams } from 'react-router-dom';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { FullPageSpinner } from '@/shared/components/FullPageSpinner';

/**
 * Aliases for the route paths named in the original project brief.
 *
 * The canonical paths differ because `SpaFallbackController` on the backend only forwards deep
 * links under a fixed set of prefixes — `/u/**`, `/posts/**`, `/admin/**`, plus any single
 * extension-less segment. Renaming the real routes to match the brief would mean a post link
 * working in-app but 404ing on refresh or when shared.
 *
 * So the brief's paths resolve, as redirects, and the canonical ones stay authoritative. `/blog/**`
 * was added to the controller's prefix list so these survive a cold load too.
 */

/** `/blog/:id` → `/posts/:id` */
export function RedirectToPost() {
  const { id } = useParams<{ id: string }>();
  return <Navigate to={id ? `/posts/${id}` : '/'} replace />;
}

/**
 * `/profile` and `/my-blogs` → the signed-in user's own profile, which already lists their posts.
 *
 * Both need a username the URL does not carry, so this waits for auth to rehydrate rather than
 * bouncing a signed-in user to the login page for one frame on a cold load.
 */
export function RedirectToOwnProfile() {
  const { currentUser, isAuthenticated, isBootstrapped } = useAuth();

  if (!isBootstrapped) return <FullPageSpinner />;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: '/profile' }} />;
  if (!currentUser) return <FullPageSpinner />;

  return <Navigate to={`/u/${currentUser.username}`} replace />;
}
