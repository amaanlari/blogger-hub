import { Link } from 'react-router-dom';
import { Button } from '@/shared/components/ui/button';
import { PostFeed } from '@/features/blogs/components/PostFeed';
import { useAuth } from '@/features/auth/hooks/useAuth';

export default function HomePage() {
  const { isAuthenticated } = useAuth();

  return (
    <>
      {!isAuthenticated && (
        <section className="border-b bg-secondary/40">
          <div className="container max-w-3xl py-16 sm:py-24">
            <h1 className="font-serif text-4xl font-bold leading-[1.1] tracking-tight sm:text-6xl">
              Stay curious.
            </h1>
            <p className="mt-5 max-w-lg text-base text-muted-foreground sm:text-lg">
              Read what matters, write what you know, and find people worth following.
            </p>
            <Button asChild size="lg" className="mt-8 rounded-full">
              <Link to="/signup">Start reading</Link>
            </Button>
          </div>
        </section>
      )}

      <div className="container max-w-3xl py-10">
        {isAuthenticated && (
          <h1 className="mb-2 font-serif text-3xl font-bold tracking-tight">Latest</h1>
        )}
        <PostFeed />
      </div>
    </>
  );
}
