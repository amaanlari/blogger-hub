import { AlertCircle } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { getErrorMessage } from '@/shared/utils/errorHandler';

interface ErrorStateProps {
  error: unknown;
  onRetry?: () => void;
  className?: string;
}

export function ErrorState({ error, onRetry, className }: ErrorStateProps) {
  return (
    <div className={`flex flex-col items-center gap-3 py-12 text-center ${className ?? ''}`}>
      <AlertCircle className="h-6 w-6 text-muted-foreground" />
      <p className="max-w-sm text-sm text-muted-foreground">{getErrorMessage(error)}</p>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}
