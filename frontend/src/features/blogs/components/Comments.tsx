import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Trash2 } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Textarea } from '@/shared/components/ui/textarea';
import { Avatar, AvatarFallback, AvatarImage } from '@/shared/components/ui/avatar';
import { Skeleton } from '@/shared/components/ui/skeleton';
import { ErrorState } from '@/shared/components/ErrorState';
import { useComments } from '@/features/blogs/hooks/useBlogs';
import {
  useAddComment,
  useDeleteComment,
  useReplyToComment,
} from '@/features/blogs/hooks/useInteractions';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { formatRelative, initials } from '@/shared/utils/formatting';
import type { CommentNode, PublicUser } from '@/shared/types/models';

export function Comments({ postId }: { postId: string }) {
  const { data, isPending, isError, error, refetch } = useComments(postId);
  const { isAuthenticated } = useAuth();
  const addComment = useAddComment(postId);
  const [draft, setDraft] = useState('');

  if (isPending) {
    return (
      <div className="space-y-4 pt-4">
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-16 w-full" />
      </div>
    );
  }

  if (isError) return <ErrorState error={error} onRetry={() => refetch()} />;

  const total = countComments(data.roots);

  return (
    <section className="border-t pt-8">
      <h2 className="font-serif text-xl font-bold">
        {total === 0 ? 'Responses' : `${total} response${total === 1 ? '' : 's'}`}
      </h2>

      {isAuthenticated ? (
        <form
          className="mt-4"
          onSubmit={(event) => {
            event.preventDefault();
            const content = draft.trim();
            if (!content) return;
            addComment.mutate(content, { onSuccess: () => setDraft('') });
          }}
        >
          <Textarea
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="What did you think?"
            rows={3}
            aria-label="Write a response"
          />
          <div className="mt-2 flex justify-end">
            <Button
              type="submit"
              size="sm"
              disabled={!draft.trim() || addComment.isPending}
            >
              {addComment.isPending ? 'Posting…' : 'Respond'}
            </Button>
          </div>
        </form>
      ) : (
        <p className="mt-4 text-sm text-muted-foreground">
          <Link to="/login" className="underline underline-offset-4">
            Sign in
          </Link>{' '}
          to join the conversation.
        </p>
      )}

      <div className="mt-8 space-y-6">
        {data.roots.map((node) => (
          <CommentItem
            key={node.id}
            node={node}
            postId={postId}
            authorsById={data.authorsById}
          />
        ))}
      </div>
    </section>
  );
}

function countComments(nodes: CommentNode[]): number {
  return nodes.reduce((total, node) => total + 1 + countComments(node.replies), 0);
}

interface CommentItemProps {
  node: CommentNode;
  postId: string;
  authorsById: Map<string, PublicUser>;
  depth?: number;
}

function CommentItem({ node, postId, authorsById, depth = 0 }: CommentItemProps) {
  const { currentUser, isAuthenticated } = useAuth();
  const reply = useReplyToComment(postId);
  const remove = useDeleteComment(postId);
  const [replyDraft, setReplyDraft] = useState<string | null>(null);

  // Missing from the lookup means the account was deleted — the comment itself survives, since
  // nothing cascades.
  const author = authorsById.get(node.user_id);
  const isOwn = currentUser?.id === node.user_id;

  // Only the first level is indented. Threads here can nest arbitrarily deep via parent_id, and
  // indenting every level turns a long exchange into a sliver of text on a phone.
  const indent = depth > 0 ? 'ml-6 sm:ml-11' : '';

  return (
    <div className={indent}>
      <div className="flex gap-3">
        <Avatar className="h-8 w-8 shrink-0">
          <AvatarImage src={author?.profile_picture} alt="" />
          <AvatarFallback>{initials(author?.username)}</AvatarFallback>
        </Avatar>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 text-sm">
            {author ? (
              <Link to={`/u/${author.username}`} className="font-medium hover:underline">
                {author.username}
              </Link>
            ) : (
              <span className="font-medium text-muted-foreground">Deleted account</span>
            )}
            <span className="text-xs text-muted-foreground">
              {formatRelative(node.created_at)}
            </span>
          </div>

          <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed">{node.content}</p>

          <div className="mt-2 flex items-center gap-3">
            {isAuthenticated && (
              <button
                type="button"
                className="text-xs text-muted-foreground hover:text-foreground"
                onClick={() => setReplyDraft((draft) => (draft === null ? '' : null))}
              >
                Reply
              </button>
            )}
            {isOwn && (
              <button
                type="button"
                className="flex items-center gap-1 text-xs text-muted-foreground hover:text-destructive"
                disabled={remove.isPending}
                onClick={() => remove.mutate(node.id)}
              >
                <Trash2 className="h-3 w-3" />
                Delete
              </button>
            )}
          </div>

          {replyDraft !== null && (
            <form
              className="mt-3"
              onSubmit={(event) => {
                event.preventDefault();
                const content = replyDraft.trim();
                if (!content) return;
                reply.mutate(
                  { parentId: node.id, content },
                  { onSuccess: () => setReplyDraft(null) },
                );
              }}
            >
              <Textarea
                value={replyDraft}
                onChange={(event) => setReplyDraft(event.target.value)}
                rows={2}
                autoFocus
                aria-label={`Reply to ${author?.username ?? 'this comment'}`}
              />
              <div className="mt-2 flex justify-end gap-2">
                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  onClick={() => setReplyDraft(null)}
                >
                  Cancel
                </Button>
                <Button type="submit" size="sm" disabled={!replyDraft.trim() || reply.isPending}>
                  {reply.isPending ? 'Replying…' : 'Reply'}
                </Button>
              </div>
            </form>
          )}
        </div>
      </div>

      {node.replies.length > 0 && (
        <div className="mt-5 space-y-5">
          {node.replies.map((child) => (
            <CommentItem
              key={child.id}
              node={child}
              postId={postId}
              authorsById={authorsById}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}
