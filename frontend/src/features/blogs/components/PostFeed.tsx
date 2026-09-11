import { Button } from '@/shared/components/ui/button';
import { Skeleton } from '@/shared/components/ui/skeleton';
import { BlogCard } from '@/features/blogs/components/BlogCard';
import { ErrorState } from '@/shared/components/ErrorState';
import { EmptyState } from '@/shared/components/EmptyState';
import { useFeed } from '@/features/blogs/hooks/useBlogs';

interface PostFeedProps {
  query?: string;
  emptyTitle?: string;
  emptyDescription?: string;
}

export function PostFeed({ query = '', emptyTitle, emptyDescription }: PostFeedProps) {
  const feed = useFeed(query);

  if (feed.isPending) return <PostFeedSkeleton />;
  if (feed.isError) return <ErrorState error={feed.error} onRetry={() => feed.refetch()} />;

  const posts = feed.data.pages.flatMap((page) => page.posts);

  if (posts.length === 0) {
    return (
      <EmptyState
        title={emptyTitle ?? 'Nothing to read yet'}
        description={emptyDescription ?? 'Posts will show up here once someone publishes one.'}
      />
    );
  }

  return (
    <div>
      {posts.map((post) => (
        <BlogCard key={post.blog_post_id} post={post} />
      ))}

      {feed.hasNextPage && (
        <div className="flex justify-center py-8">
          <Button
            variant="outline"
            onClick={() => feed.fetchNextPage()}
            disabled={feed.isFetchingNextPage}
          >
            {feed.isFetchingNextPage ? 'Loading…' : 'Load more'}
          </Button>
        </div>
      )}
    </div>
  );
}

function PostFeedSkeleton() {
  return (
    <div className="space-y-8 py-8">
      {Array.from({ length: 4 }).map((_, index) => (
        <div key={index} className="flex gap-6">
          <div className="flex-1 space-y-3">
            <Skeleton className="h-3 w-32" />
            <Skeleton className="h-6 w-3/4" />
            <Skeleton className="h-4 w-full" />
          </div>
          <Skeleton className="hidden h-28 w-28 shrink-0 rounded-md sm:block md:h-32 md:w-48" />
        </div>
      ))}
    </div>
  );
}
