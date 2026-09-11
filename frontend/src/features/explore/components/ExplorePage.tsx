import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';
import { Input } from '@/shared/components/ui/input';
import { PostFeed } from '@/features/blogs/components/PostFeed';
import { useDebounce } from '@/shared/hooks/useDebounce';

export default function ExplorePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [input, setInput] = useState(searchParams.get('q') ?? '');
  const query = useDebounce(input, 500);

  // Keep the URL in step with the debounced term so a search is shareable and survives a reload,
  // without pushing a history entry per keystroke.
  useEffect(() => {
    setSearchParams(query ? { q: query } : {}, { replace: true });
  }, [query, setSearchParams]);

  return (
    <div className="container max-w-3xl py-10">
      <h1 className="font-serif text-3xl font-bold tracking-tight">Explore</h1>

      <div className="relative mt-6">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder="Search by title or description"
          aria-label="Search posts"
          className="h-11 pl-10"
        />
      </div>

      <PostFeed
        query={query}
        emptyTitle={query ? `No posts match “${query}”` : 'Nothing to read yet'}
        emptyDescription={
          query ? 'Try a different word or clear the search.' : undefined
        }
      />
    </div>
  );
}
