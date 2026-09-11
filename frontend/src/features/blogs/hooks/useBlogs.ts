import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { blogsApi, usersApi } from '@/features/blogs/api/blogsApi';
import { hasNextPage } from '@/shared/types/api';
import type { CommentNode, PublicUser } from '@/shared/types/models';
import { interactionsApi } from '@/features/blogs/api/blogsApi';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { ApiError } from '@/shared/lib/api-response';

const PAGE_SIZE = 10;

export const blogKeys = {
  all: ['blogs'] as const,
  feed: (q: string) => ['blogs', 'feed', q] as const,
  byUser: (username: string) => ['blogs', 'by-user', username] as const,
  detail: (id: string) => ['blogs', 'detail', id] as const,
  comments: (postId: string) => ['blogs', 'comments', postId] as const,
  likedPosts: ['blogs', 'liked-posts'] as const,
};

export function useFeed(q = '') {
  return useInfiniteQuery({
    queryKey: blogKeys.feed(q),
    initialPageParam: 0,
    queryFn: ({ pageParam }) => blogsApi.list({ page: pageParam, size: PAGE_SIZE, q }),
    // The API exposes no `hasNext`, `last` or cursor — only totals — so the next page is derived.
    getNextPageParam: (lastPage) =>
      hasNextPage(lastPage.pagination) ? lastPage.pagination.page + 1 : undefined,
  });
}

export function usePostsByUsername(username: string | undefined) {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: blogKeys.byUser(username ?? ''),
    queryFn: () => blogsApi.listByUsername(username!),
    // This endpoint is not whitelisted, so signed-out visitors would just get a 401. The profile
    // page falls back to the public feed for them.
    enabled: Boolean(username) && isAuthenticated,
  });
}

/**
 * A single post, with the two failure modes that are really UI states rather than errors surfaced
 * as flags rather than left for the caller to sniff out of an error message.
 *
 * - `isPremiumLocked` — the post is premium and the viewer is not a `PREMIUM_USER`. The server
 *   answers 403 with **no post data at all**, not even a title, so there is nothing to render
 *   behind a paywall except what the feed already cached.
 * - `isSignInRequired` — the viewer has no valid token. Reading a single post requires auth even
 *   though the feed does not, so this is reachable by following a shared link while signed out.
 */
export function useBlogDetail(id: string | undefined) {
  const query = useQuery({
    queryKey: blogKeys.detail(id ?? ''),
    queryFn: () => blogsApi.getById(id!),
    enabled: Boolean(id),
  });

  const error = query.error;
  const isApiError = error instanceof ApiError;

  return {
    ...query,
    // Matched on the message because a @PreAuthorize denial and a hand-built 403 are
    // indistinguishable by status in this API — see the error-shape notes in docs/FRONTEND.md.
    isPremiumLocked: isApiError && error.message.includes('premium'),
    isSignInRequired: isApiError && error.kind === 'unauthenticated',
  };
}

/**
 * Comments, with their authors resolved and threading rebuilt.
 *
 * The comments endpoint returns a flat array carrying only `user_id` — no name, no avatar — so the
 * distinct IDs are resolved in a single batch lookup and joined here. Authors that come back
 * missing (deleted accounts) are left out of the map and render as a placeholder rather than
 * blanking the thread.
 */
export function useComments(postId: string | undefined) {
  return useQuery({
    queryKey: blogKeys.comments(postId ?? ''),
    enabled: Boolean(postId),
    queryFn: async () => {
      const comments = await interactionsApi.listComments(postId!);
      const authorIds = [...new Set(comments.map((comment) => comment.user_id))];
      const authors = await usersApi.lookup(authorIds);

      const authorsById = new Map<string, PublicUser>(
        authors.map((author) => [author.id, author]),
      );

      const nodesById = new Map<string, CommentNode>(
        comments.map((comment) => [comment.id, { ...comment, replies: [] }]),
      );

      const roots: CommentNode[] = [];
      for (const node of nodesById.values()) {
        const parent = node.parent_id ? nodesById.get(node.parent_id) : undefined;
        if (parent) parent.replies.push(node);
        else roots.push(node);
      }

      const byOldestFirst = (a: CommentNode, b: CommentNode) =>
        a.created_at.localeCompare(b.created_at);
      roots.sort(byOldestFirst);
      for (const node of nodesById.values()) node.replies.sort(byOldestFirst);

      return { roots, authorsById };
    },
  });
}

/**
 * The set of post IDs the current user has liked.
 *
 * There is no "have I liked this post" endpoint, and no unique index preventing a duplicate like —
 * a second like on the same post just creates a second row. So the client has to know the answer
 * before it can render the button correctly.
 */
export function useLikedPostIds() {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: blogKeys.likedPosts,
    queryFn: async () => {
      const posts = await interactionsApi.likedPosts();
      return new Set(posts.map((post) => post.blog_post_id));
    },
    enabled: isAuthenticated,
  });
}
