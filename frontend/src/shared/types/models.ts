import type { PaginationMeta } from '@/shared/types/api';

/**
 * Domain types, named for the exact keys on the wire.
 *
 * Jackson's `SNAKE_CASE` strategy applies to bean properties, so DTO fields are snake_case — but
 * boolean getters lose their prefix in the process: `isPremium()` serialises as `premium`, not
 * `is_premium`, and `isRead()` as `read`. Responses assembled from `Map.of(...)` skip the strategy
 * entirely and stay camelCase. All three conventions appear below, and all three are correct.
 */

export type Role = 'FREE_USER' | 'PREMIUM_USER' | 'ADMIN_USER';

/** Full user projection. Note `email` is present even on the public username lookup. */
export interface BlogUser {
  id: string;
  username: string;
  email: string;
  bio: string | null;
  profile_picture: string;
  roles: Role[];
}

/** The email-free projection returned by the batch lookup used for comment authors. */
export interface PublicUser {
  id: string;
  username: string;
  bio: string | null;
  profile_picture: string;
  roles: Role[];
}

export interface TokenResponse {
  user_id: string;
  access_token: string;
  refresh_token: string;
}

/** Feed/search projection. Carries no `content` — see the backend DTO for why. */
export interface BlogPostSummary {
  blog_post_id: string;
  title: string;
  description: string;
  banner_image_url: string | null;
  premium: boolean;
  created_by: string | null;
  created_at: string;
  updated_at: string;
}

/**
 * The per-author listing from `GET /api/blogposts/{username}`. Same entity as
 * {@link BlogPostDetail}, but the author here is flattened to a bare username string.
 */
export interface BlogPostListItem extends BlogPostSummary {
  content: string;
  updated_by: string | null;
}

export interface BlogUserRef {
  id: string;
  username: string;
}

/**
 * `GET /api/blogposts/post/{id}` returns the raw Mongo document rather than a DTO, so the author
 * fields come back as objects here and as plain strings everywhere else. Kept as a separate type on
 * purpose: sharing one type across both endpoints is the bug this models away.
 */
export interface BlogPostDetail {
  blog_post_id: string;
  title: string;
  description: string;
  banner_image_url: string | null;
  content: string;
  premium: boolean;
  created_by: BlogUserRef | null;
  created_at: string;
  updated_by: BlogUserRef | null;
  updated_at: string;
}

/** Raw `Comments` document. Carries only `user_id` — resolve authors via the batch user lookup. */
export interface Comment {
  id: string;
  post_id: string;
  user_id: string;
  parent_id: string | null;
  content: string;
  created_at: string;
}

/** A comment with its replies attached, built client-side from the flat list. */
export interface CommentNode extends Comment {
  replies: CommentNode[];
}

export type NotificationType =
  | 'NEW_FOLLOWER'
  | 'POST_LIKED'
  | 'POST_COMMENTED'
  | 'COMMENT_REPLIED'
  | 'COMMENT_LIKED'
  | 'MENTION_IN_POST'
  | 'MENTION_IN_COMMENT';

export interface AppNotification {
  id: string;
  type: NotificationType;
  actor: { id: string; username: string; profile_picture: string | null };
  target: { id: string; type: 'post' | 'comment' | 'user'; title: string | null };
  /** Pre-built server-side; render this rather than composing your own sentence. */
  message: string;
  preview: string | null;
  read: boolean;
  created_at: string;
  read_at: string | null;
}

/** Mixed casing is real here: snake_case DTOs beside camelCase map keys. */
export interface NotificationsPage {
  notifications: AppNotification[];
  unreadCount: number;
  pagination: PaginationMeta;
}

export interface BlogPostsPage {
  posts: BlogPostSummary[];
  pagination: PaginationMeta;
}

export interface UploadedImage {
  secure_url: string;
  public_id: string;
  width: number;
  height: number;
}
