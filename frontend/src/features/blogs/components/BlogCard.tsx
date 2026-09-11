import { Link } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import { Badge } from '@/shared/components/ui/badge';
import { formatDate } from '@/shared/utils/formatting';
import type { BlogPostSummary } from '@/shared/types/models';

export function BlogCard({ post }: { post: BlogPostSummary }) {
  return (
    <article className="group border-b py-8 first:pt-0 last:border-b-0">
      <div className="flex gap-6">
        <div className="min-w-0 flex-1">
          <div className="mb-2 flex items-center gap-2 text-xs text-muted-foreground">
            {post.created_by ? (
              <Link
                to={`/u/${post.created_by}`}
                className="font-medium text-foreground hover:underline"
              >
                {post.created_by}
              </Link>
            ) : (
              <span>Unknown author</span>
            )}
            <span aria-hidden>·</span>
            <time dateTime={post.created_at}>{formatDate(post.created_at)}</time>
          </div>

          <Link to={`/posts/${post.blog_post_id}`} className="block">
            <h2 className="font-serif text-xl font-bold leading-snug tracking-tight group-hover:underline sm:text-2xl">
              {post.title || 'Untitled'}
            </h2>
            {post.description && (
              <p className="mt-2 line-clamp-2 text-sm text-muted-foreground sm:text-base">
                {post.description}
              </p>
            )}
          </Link>

          {post.premium && (
            <Badge variant="secondary" className="mt-3 gap-1">
              <Sparkles className="h-3 w-3" />
              Premium
            </Badge>
          )}
        </div>

        {post.banner_image_url && (
          <Link
            to={`/posts/${post.blog_post_id}`}
            className="hidden shrink-0 sm:block"
            tabIndex={-1}
            aria-hidden
          >
            <img
              src={post.banner_image_url}
              alt=""
              loading="lazy"
              className="h-28 w-28 rounded-md object-cover md:h-32 md:w-48"
              onError={(event) => {
                // Banner URLs are arbitrary strings the author pasted or uploaded; a dead one
                // should quietly vanish rather than leave a broken-image glyph in the feed.
                event.currentTarget.style.display = 'none';
              }}
            />
          </Link>
        )}
      </div>
    </article>
  );
}
