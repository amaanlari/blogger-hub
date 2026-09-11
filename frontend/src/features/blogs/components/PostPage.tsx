import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { BookOpen, Heart, Loader2, Pencil, Sparkles, Trash2 } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Badge } from '@/shared/components/ui/badge';
import { Avatar, AvatarFallback } from '@/shared/components/ui/avatar';
import { Skeleton } from '@/shared/components/ui/skeleton';
import { BlogContent } from '@/features/blogs/components/BlogContent';
import { Comments } from '@/features/blogs/components/Comments';
import { ErrorState } from '@/shared/components/ErrorState';
import { useBlogDetail, useLikedPostIds, blogKeys } from '@/features/blogs/hooks/useBlogs';
import { useToggleLike } from '@/features/blogs/hooks/useInteractions';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { blogsApi } from '@/features/blogs/api/blogsApi';
import { formatDate, initials, readingTime } from '@/shared/utils/formatting';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { cn } from '@/shared/lib/utils';

export default function PostPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { currentUser, isAuthenticated } = useAuth();

  const {
    data: post,
    isPending,
    isError,
    error,
    refetch,
    isPremiumLocked,
    isSignInRequired,
  } = useBlogDetail(id);
  const { data: likedPostIds } = useLikedPostIds();
  const { like, unlike, isPending: likePending } = useToggleLike(id ?? '');

  const remove = useMutation({
    mutationFn: () => blogsApi.remove(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: blogKeys.all });
      navigate('/', { replace: true });
    },
  });

  if (isPending) return <PostSkeleton />;

  // A premium post returns 403 with no content at all — not even a title — so the paywall has
  // nothing of the post to show. Handled before the generic error state so it reads as an
  // invitation rather than a failure.
  if (isPremiumLocked) return <Paywall />;

  // The feed is public but reading a single post is not, so a signed-out visitor arrives here
  // legitimately — via a shared link or a click from the home page — and must be asked to sign in
  // rather than shown a failure. Whitelisting the detail endpoint instead would not work: with an
  // anonymous principal the server's premium check does not match its `instanceof BlogUser` guard,
  // so it would hand out premium posts in full to anyone.
  if (isSignInRequired) return <SignInToRead />;

  if (isError) {
    return (
      <div className="container max-w-2xl">
        <ErrorState error={error} onRetry={() => refetch()} />
      </div>
    );
  }

  const authorName = post.created_by?.username ?? null;
  const isAuthor = Boolean(currentUser && post.created_by?.id === currentUser.id);
  const isLiked = likedPostIds?.has(post.blog_post_id) ?? false;

  return (
    <article className="container max-w-2xl py-10 sm:py-14">
      <header>
        {post.premium && (
          <Badge variant="secondary" className="mb-4 gap-1">
            <Sparkles className="h-3 w-3" />
            Premium
          </Badge>
        )}

        <h1 className="font-serif text-3xl font-bold leading-[1.15] tracking-tight sm:text-5xl">
          {post.title || 'Untitled'}
        </h1>

        {post.description && (
          <p className="mt-4 text-lg leading-relaxed text-muted-foreground">
            {post.description}
          </p>
        )}

        <div className="mt-8 flex flex-wrap items-center gap-3 border-y py-4">
          <Avatar className="h-10 w-10">
            <AvatarFallback>{initials(authorName)}</AvatarFallback>
          </Avatar>

          <div className="min-w-0 flex-1 text-sm">
            {authorName ? (
              <Link to={`/u/${authorName}`} className="font-medium hover:underline">
                {authorName}
              </Link>
            ) : (
              <span className="font-medium text-muted-foreground">Unknown author</span>
            )}
            <p className="text-xs text-muted-foreground">
              {formatDate(post.created_at)} · {readingTime(post.content)}
            </p>
          </div>

          {isAuthor && (
            <div className="flex items-center gap-1">
              <Button asChild variant="ghost" size="sm">
                <Link to={`/posts/${post.blog_post_id}/edit`}>
                  <Pencil className="mr-1.5 h-3.5 w-3.5" />
                  Edit
                </Link>
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="text-muted-foreground hover:text-destructive"
                disabled={remove.isPending}
                onClick={() => remove.mutate()}
              >
                <Trash2 className="mr-1.5 h-3.5 w-3.5" />
                Delete
              </Button>
            </div>
          )}
        </div>
      </header>

      {post.banner_image_url && (
        <img
          src={post.banner_image_url}
          alt=""
          className="mt-8 w-full rounded-lg"
          onError={(event) => {
            event.currentTarget.style.display = 'none';
          }}
        />
      )}

      <div className="mt-10">
        <BlogContent content={post.content} />
      </div>

      {isAuthenticated && (
        <div className="mt-12 flex items-center gap-3 border-t pt-6">
          <Button
            variant="outline"
            size="sm"
            className={cn('gap-2', isLiked && 'border-destructive/40 text-destructive')}
            disabled={likePending}
            onClick={() => (isLiked ? unlike.mutate() : like.mutate())}
          >
            {likePending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Heart className={cn('h-4 w-4', isLiked && 'fill-current')} />
            )}
            {isLiked ? 'Liked' : 'Like'}
          </Button>
        </div>
      )}

      <div className="mt-12">
        <Comments postId={post.blog_post_id} />
      </div>
    </article>
  );
}

function SignInToRead() {
  const location = useLocation();

  return (
    <div className="container max-w-xl py-24 text-center">
      <BookOpen className="mx-auto h-8 w-8 text-muted-foreground" />
      <h1 className="mt-4 font-serif text-3xl font-bold tracking-tight">
        Sign in to read this post
      </h1>
      <p className="mx-auto mt-3 max-w-sm text-sm text-muted-foreground">
        Browsing is open to everyone, but full posts are for signed-in readers.
      </p>
      <div className="mt-8 flex justify-center gap-2">
        <Button asChild>
          {/* Carries the current path so signing in returns the reader to the post they wanted. */}
          <Link to="/login" state={{ from: location.pathname }}>
            Sign in
          </Link>
        </Button>
        <Button asChild variant="outline">
          <Link to="/signup">Create an account</Link>
        </Button>
      </div>
    </div>
  );
}

function Paywall() {
  return (
    <div className="container max-w-xl py-24 text-center">
      <Sparkles className="mx-auto h-8 w-8 text-muted-foreground" />
      <h1 className="mt-4 font-serif text-3xl font-bold tracking-tight">
        This story is for premium members
      </h1>
      <p className="mx-auto mt-3 max-w-sm text-sm text-muted-foreground">
        Upgrade your account to read it in full.
      </p>
      <Button asChild variant="outline" className="mt-8">
        <Link to="/explore">Find something else to read</Link>
      </Button>
    </div>
  );
}

function PostSkeleton() {
  return (
    <div className="container max-w-2xl space-y-6 py-10 sm:py-14">
      <Skeleton className="h-12 w-3/4" />
      <Skeleton className="h-6 w-full" />
      <Skeleton className="h-14 w-full" />
      <div className="space-y-3 pt-6">
        {Array.from({ length: 8 }).map((_, index) => (
          <Skeleton key={index} className="h-4 w-full" />
        ))}
      </div>
    </div>
  );
}
