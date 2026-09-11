import { useMutation, useQueryClient } from '@tanstack/react-query';
import { interactionsApi } from '@/features/blogs/api/blogsApi';
import { blogKeys } from '@/features/blogs/hooks/useBlogs';

/**
 * Likes and unlikes a post.
 *
 * Deliberately not optimistic. Liking twice creates a duplicate row rather than being rejected —
 * there is no unique index and no "already liked" check — and unliking removes *a* like on the
 * post rather than necessarily the caller's own. Showing a state the server may not actually agree
 * with would make both of those worse, so the button waits for the round trip and then refetches
 * the truth.
 */
export function useToggleLike(postId: string) {
  const queryClient = useQueryClient();

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: blogKeys.likedPosts });

  const like = useMutation({
    mutationFn: () => interactionsApi.like(postId),
    onSuccess: invalidate,
  });

  const unlike = useMutation({
    mutationFn: () => interactionsApi.unlike(postId),
    onSuccess: invalidate,
  });

  return {
    like,
    unlike,
    isPending: like.isPending || unlike.isPending,
  };
}

export function useAddComment(postId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (content: string) =>
      interactionsApi.addComment({ post_id: postId, content }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: blogKeys.comments(postId) }),
  });
}

export function useReplyToComment(postId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ parentId, content }: { parentId: string; content: string }) =>
      interactionsApi.replyToComment({ post_id: postId, parent_id: parentId, content }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: blogKeys.comments(postId) }),
  });
}

export function useDeleteComment(postId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    // Deleting a top-level comment cascades to its direct replies server-side, so refetching is
    // the only way to get an accurate tree back.
    mutationFn: (commentId: string) => interactionsApi.deleteComment(commentId),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: blogKeys.comments(postId) }),
  });
}
