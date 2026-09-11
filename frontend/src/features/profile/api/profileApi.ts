import { api } from '@/shared/lib/http';
import type { BlogUser } from '@/shared/types/models';

/**
 * Fields on `BlogUser` that are safe for a user to change about themselves.
 *
 * Keys here are **Java field names in camelCase**, not the snake_case used by every other request
 * body in this API. `PATCH /api/users/{userId}` takes a raw `Map<String, Object>` and reflects each
 * key onto the entity with `getDeclaredField`, which bypasses Jackson's naming strategy entirely —
 * `email_notifications_enabled` would simply throw `NoSuchFieldException` and surface as a 500.
 *
 * The endpoint has no allow-list of its own: it will just as happily set `roles`, `status`, or a
 * plaintext `password` that bypasses BCrypt and permanently breaks the account's login. This type
 * is the allow-list.
 */
export interface ProfilePatch {
  bio?: string;
  profilePicture?: string;
  emailNotificationsEnabled?: boolean;
}

export const profileApi = {
  getByUsername: (username: string) =>
    api.get<BlogUser>(`/users/${encodeURIComponent(username)}`),

  /**
   * Partial update.
   *
   * Always prefer this to `PUT /api/users/{userId}`, which overwrites username, email, password and
   * bio unconditionally and re-hashes the password every time — meaning a bio edit through PUT
   * would require re-sending the user's plaintext password.
   */
  update: (userId: string, patch: ProfilePatch) =>
    api.patch<void>(`/users/${userId}`, patch),

  uploadAvatar: (userId: string, file: File) => {
    const form = new FormData();
    // Part name is `profilePicture` — a @RequestParam name, so camelCase regardless of the JSON
    // naming strategy.
    form.append('profilePicture', file);
    return api.post<{ secure_url: string }>(
      `/users/${userId}/upload-profile-picture`,
      form,
      { headers: { 'Content-Type': undefined } },
    );
  },

  removeAvatar: (userId: string) =>
    api.post<void>(`/users/${userId}/remove-profile-picture`),
};

/**
 * Avatar uploads overwrite a fixed Cloudinary public ID, so the URL never changes between uploads
 * and the browser keeps serving the cached old image. A cache-busting parameter is the only way to
 * make a freshly uploaded picture actually appear.
 */
export function bustCache(url: string): string {
  if (!url) return url;
  return `${url}${url.includes('?') ? '&' : '?'}v=${Date.now()}`;
}
