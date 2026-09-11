import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { Layout } from '@/shared/components/layout/Layout';
import { FullPageSpinner } from '@/shared/components/FullPageSpinner';
import { ProtectedRoute } from '@/features/auth/components/ProtectedRoute';
import { useBootstrapAuth } from '@/features/auth/hooks/useAuth';
import { RedirectToOwnProfile, RedirectToPost } from '@/app/aliases';

/**
 * Route paths are constrained by the backend.
 *
 * `SpaFallbackController` forwards deep links to index.html by matching a fixed set of prefixes —
 * any single extension-less segment, plus `/u/**`, `/posts/**` and `/admin/**`. A new nested route
 * outside those prefixes would 404 on refresh even though it works via client-side navigation, so
 * adding one means editing that controller too.
 */
const HomePage = lazy(() => import('@/features/home/components/HomePage'));
const ExplorePage = lazy(() => import('@/features/explore/components/ExplorePage'));
const LoginPage = lazy(() => import('@/features/auth/components/LoginPage'));
const SignupPage = lazy(() => import('@/features/auth/components/SignupPage'));
const VerifyPage = lazy(() => import('@/features/auth/components/VerifyPage'));
const PostPage = lazy(() => import('@/features/blogs/components/PostPage'));
const EditorPage = lazy(() => import('@/features/blogs/components/EditorPage'));
const ProfilePage = lazy(() => import('@/features/profile/components/ProfilePage'));
const SettingsPage = lazy(() => import('@/features/profile/components/SettingsPage'));
const NotificationsPage = lazy(
  () => import('@/features/notifications/components/NotificationsPage'),
);
const NotFoundPage = lazy(() => import('@/shared/components/NotFoundPage'));

export function App() {
  useBootstrapAuth();

  return (
    <Suspense fallback={<FullPageSpinner />}>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="explore" element={<ExplorePage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="signup" element={<SignupPage />} />
          <Route path="verify" element={<VerifyPage />} />
          <Route path="u/:username" element={<ProfilePage />} />
          <Route path="posts/:id" element={<PostPage />} />

          <Route
            path="posts/new"
            element={
              <ProtectedRoute>
                <EditorPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="posts/:id/edit"
            element={
              <ProtectedRoute>
                <EditorPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="me"
            element={
              <ProtectedRoute>
                <SettingsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="notifications"
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />

          {/* Aliases for the paths named in the project brief — see app/aliases.tsx. */}
          <Route path="blog/:id" element={<RedirectToPost />} />
          <Route path="profile" element={<RedirectToOwnProfile />} />
          <Route path="my-blogs" element={<RedirectToOwnProfile />} />

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
