import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Avatar, AvatarFallback, AvatarImage } from '@/shared/components/ui/avatar';
import { Button } from '@/shared/components/ui/button';
import { Skeleton } from '@/shared/components/ui/skeleton';
import { ErrorState } from '@/shared/components/ErrorState';
import { EmptyState } from '@/shared/components/EmptyState';
import { BlogCard } from '@/features/blogs/components/BlogCard';
import { profileApi } from '@/features/profile/api/profileApi';
import { usePostsByUsername } from '@/features/blogs/hooks/useBlogs';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { initials } from '@/shared/utils/formatting';

export default function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { currentUser, isAuthenticated } = useAuth();

  const profile = useQuery({
    queryKey: ['profile', username],
    queryFn: () => profileApi.getByUsername(username!),
    enabled: Boolean(username),
  });

  const posts = usePostsByUsername(username);
  const isOwnProfile = currentUser?.username === username;

  if (profile.isPending) {
    return (
      <div className="container max-w-3xl space-y-4 py-12">
        <Skeleton className="h-20 w-20 rounded-full" />
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-4 w-full max-w-md" />
      </div>
    );
  }

  if (profile.isError) {
    return (
      <div className="container max-w-3xl">
        <ErrorState error={profile.error} onRetry={() => profile.refetch()} />
      </div>
    );
  }

  return (
    <div className="container max-w-3xl py-12">
      <header className="flex flex-col gap-5 sm:flex-row sm:items-start">
        <Avatar className="h-20 w-20">
          <AvatarImage src={profile.data.profile_picture} alt="" />
          <AvatarFallback className="text-xl">{initials(profile.data.username)}</AvatarFallback>
        </Avatar>

        <div className="min-w-0 flex-1">
          <h1 className="font-serif text-3xl font-bold tracking-tight">
            {profile.data.username}
          </h1>
          {profile.data.bio && (
            <p className="mt-2 max-w-lg text-sm leading-relaxed text-muted-foreground">
              {profile.data.bio}
            </p>
          )}
        </div>

        {isOwnProfile && (
          <Button asChild variant="outline" size="sm">
            <Link to="/me">Edit profile</Link>
          </Button>
        )}
      </header>

      <section className="mt-12 border-t pt-2">
        {/* Listing an author's posts requires a token — unlike the public profile lookup above —
            so signed-out visitors get an invitation rather than a failed request. */}
        {!isAuthenticated ? (
          <EmptyState
            title="Sign in to see these posts"
            description={`${profile.data.username}'s posts are visible to signed-in readers.`}
            action={
              <Button asChild size="sm">
                <Link to="/login">Sign in</Link>
              </Button>
            }
          />
        ) : posts.isPending ? (
          <div className="space-y-6 py-8">
            {Array.from({ length: 3 }).map((_, index) => (
              <Skeleton key={index} className="h-20 w-full" />
            ))}
          </div>
        ) : posts.isError ? (
          <ErrorState error={posts.error} onRetry={() => posts.refetch()} />
        ) : posts.data.length === 0 ? (
          <EmptyState
            title={isOwnProfile ? 'You have not published anything yet' : 'No posts yet'}
            action={
              isOwnProfile ? (
                <Button asChild size="sm">
                  <Link to="/posts/new">Write your first post</Link>
                </Button>
              ) : undefined
            }
          />
        ) : (
          <div>
            {[...posts.data]
              .sort((a, b) => b.created_at.localeCompare(a.created_at))
              .map((post) => (
                <BlogCard key={post.blog_post_id} post={post} />
              ))}
          </div>
        )}
      </section>
    </div>
  );
}
