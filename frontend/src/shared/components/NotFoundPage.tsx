import { Link } from 'react-router-dom';
import { Button } from '@/shared/components/ui/button';

export default function NotFoundPage() {
  return (
    <div className="container flex flex-col items-center gap-4 py-24 text-center">
      <p className="font-serif text-4xl font-bold">Nothing here</p>
      <p className="max-w-sm text-sm text-muted-foreground">
        That page does not exist, or it may have been deleted.
      </p>
      <Button asChild>
        <Link to="/">Back to home</Link>
      </Button>
    </div>
  );
}
