import { api } from '@/shared/lib/http';
import type {
  BlogPostDetail,
  BlogPostListItem,
  BlogPostsPage,
  Comment,
  PublicUser,
  UploadedImage,
} from '@/shared/types/models';

export interface BlogPostInput {
  title: string;
  description: string;
  banner_image_url: string;
  content: string;
}

export const blogsApi = {
  /** Paginated feed with optional search. Public — works signed out. */
  list: (params: { page?: number; size?: number; q?: string } = {}) =>
    api.get<BlogPostsPage>('/blogposts', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 10,
        ...(params.q ? { q: params.q } : {}),
      },
    }),

  /** All posts by one author. Requires auth, unlike the similar-looking /users/{username}. */
  listByUsername: (username: string) =>
    api
      .get<BlogPostListItem[] | null>(`/blogposts/${encodeURIComponent(username)}`)
      .then(toArray),

  /** Returns the raw document, so its author fields are objects rather than strings. */
  getById: (id: string) => api.get<BlogPostDetail>(`/blogposts/post/${id}`),

  /**
   * Creates a post. The response is a bare success message with no ID in it, so the caller has to
   * re-list the author's posts to find out what was just created.
   */
  create: (body: BlogPostInput) => api.post<void>('/blogposts', body),

  update: (id: string, body: BlogPostInput) => api.put<void>(`/blogposts/${id}`, body),

  remove: (id: string) => api.delete<void>(`/blogposts/${id}`),

  setPremium: (id: string, isPremium: boolean) =>
    api.patch<void>(`/blogposts/${id}/premium`, undefined, {
      // Snake_case here is not an accident: this parameter is bound with an explicit
      // @RequestParam(name = "is_premium"), unlike `unreadOnly` on the notifications endpoint.
      params: { is_premium: isPremium },
    }),

  uploadImage: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    // No explicit Content-Type: the browser must set it so the multipart boundary is included.
    return api.post<UploadedImage>('/media/upload', form, {
      headers: { 'Content-Type': undefined },
    });
  },
};

/**
 * Coerces a list payload that may legitimately arrive as `null`.
 *
 * Verified against a running server: `GET /api/interactions/comments` on a post with no comments
 * answers `data: null`, not `[]` — its repository method is declared as returning a bare `Object`,
 * so an empty result serialises as null. Any list endpoint here can do the same, and a raw `.map()`
 * on the result would throw inside a query and surface as a generic error state.
 */
function toArray<T>(value: T[] | null | undefined): T[] {
  return Array.isArray(value) ? value : [];
}

export const interactionsApi = {
  /** Flat and unpaginated; threading is reconstructed client-side from `parent_id`. */
  listComments: (postId: string) =>
    api
      .get<Comment[] | null>('/interactions/comments', { params: { post_id: postId } })
      .then(toArray),

  addComment: (body: { post_id: string; content: string }) =>
    api.post<Comment>('/interactions/comments', body),

  replyToComment: (body: { post_id: string; parent_id: string; content: string }) =>
    api.post<Comment>('/interactions/comments/reply', body),

  deleteComment: (commentId: string) =>
    api.delete<Comment>('/interactions/comments', { params: { comment_id: commentId } }),

  like: (blogPostId: string) =>
    api.post<{ id: string }>('/interactions/likes', { blog_post_id: blogPostId }),

  /** DELETE with a JSON body — axios needs it passed as `data`, not as a second argument. */
  unlike: (blogPostId: string) =>
    api.delete<void>('/interactions/likes', { data: { blog_post_id: blogPostId } }),

  /** Every post the signed-in user has liked. Unpaginated. */
  likedPosts: () =>
    api.get<BlogPostListItem[] | null>('/interactions/likes/user/posts').then(toArray),
};

export const usersApi = {
  lookup: (ids: string[]) =>
    ids.length === 0
      ? Promise.resolve([] as PublicUser[])
      : api
          .get<PublicUser[] | null>('/users/lookup', { params: { ids: ids.join(',') } })
          .then(toArray),
};
