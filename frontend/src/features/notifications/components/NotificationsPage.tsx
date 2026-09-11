import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, Trash2 } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '@/shared/components/ui/avatar';
import { Skeleton } from '@/shared/components/ui/skeleton';
import { ErrorState } from '@/shared/components/ErrorState';
import { EmptyState } from '@/shared/components/EmptyState';
import {
  useDeleteNotification,
  useMarkAllRead,
  useMarkNotificationRead,
  useNotifications,
} from '@/features/notifications/hooks/useNotifications';
import { hasNextPage } from '@/shared/types/api';
import { formatRelative, initials } from '@/shared/utils/formatting';
import { cn } from '@/shared/lib/utils';
import type { AppNotification } from '@/shared/types/models';

export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);

  const { data, isPending, isError, error, refetch } = useNotifications(page, unreadOnly);
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllRead();
  const remove = useDeleteNotification();

  return (
    <div className="container max-w-2xl py-12">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-serif text-3xl font-bold tracking-tight">Notifications</h1>
        <div className="flex items-center gap-2">
          <Button
            variant={unreadOnly ? 'default' : 'outline'}
            size="sm"
            onClick={() => {
              setUnreadOnly((value) => !value);
              setPage(0);
            }}
          >
            Unread only
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={markAllRead.isPending}
            onClick={() => markAllRead.mutate()}
          >
            Mark all read
          </Button>
        </div>
      </div>

      <div className="mt-8">
        {isPending ? (
          <div className="space-y-4">
            {Array.from({ length: 5 }).map((_, index) => (
              <Skeleton key={index} className="h-16 w-full" />
            ))}
          </div>
        ) : isError ? (
          <ErrorState error={error} onRetry={() => refetch()} />
        ) : data.notifications.length === 0 ? (
          <EmptyState
            title={unreadOnly ? 'Nothing unread' : 'No notifications yet'}
            description="When someone likes or replies to your posts, it will show up here."
          />
        ) : (
          <>
            <ul className="divide-y">
              {data.notifications.map((notification) => (
                <NotificationRow
                  key={notification.id}
                  notification={notification}
                  onMarkRead={() => markRead.mutate(notification.id)}
                  onDelete={() => remove.mutate(notification.id)}
                />
              ))}
            </ul>

            {(page > 0 || hasNextPage(data.pagination)) && (
              <div className="mt-8 flex items-center justify-between">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((current) => current - 1)}
                >
                  Previous
                </Button>
                <span className="text-xs text-muted-foreground">
                  Page {data.pagination.page + 1} of {Math.max(1, data.pagination.totalPages)}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={!hasNextPage(data.pagination)}
                  onClick={() => setPage((current) => current + 1)}
                >
                  Next
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

interface NotificationRowProps {
  notification: AppNotification;
  onMarkRead: () => void;
  onDelete: () => void;
}

function NotificationRow({ notification, onMarkRead, onDelete }: NotificationRowProps) {
  // `target.type` is one of post/comment/user; only a post target has somewhere to navigate to.
  const href =
    notification.target?.type === 'post' && notification.target.id
      ? `/posts/${notification.target.id}`
      : null;

  const body = (
    <div className="min-w-0 flex-1">
      {/* The message is composed server-side — rendering it verbatim keeps the wording consistent
          with the matching email. */}
      <p className={cn('text-sm', !notification.read && 'font-medium')}>
        {notification.message}
      </p>
      {notification.preview && (
        <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">
          {notification.preview}
        </p>
      )}
      <p className="mt-1 text-xs text-muted-foreground">
        {formatRelative(notification.created_at)}
      </p>
    </div>
  );

  return (
    <li className={cn('flex items-start gap-3 py-4', !notification.read && 'bg-secondary/30')}>
      <Avatar className="h-8 w-8 shrink-0">
        <AvatarImage src={notification.actor?.profile_picture ?? undefined} alt="" />
        <AvatarFallback>{initials(notification.actor?.username)}</AvatarFallback>
      </Avatar>

      {href ? (
        <Link to={href} className="min-w-0 flex-1" onClick={onMarkRead}>
          {body}
        </Link>
      ) : (
        body
      )}

      <div className="flex shrink-0 items-center gap-1">
        {!notification.read && (
          <Button variant="ghost" size="icon" aria-label="Mark as read" onClick={onMarkRead}>
            <Check className="h-4 w-4" />
          </Button>
        )}
        <Button
          variant="ghost"
          size="icon"
          aria-label="Delete notification"
          className="text-muted-foreground hover:text-destructive"
          onClick={onDelete}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    </li>
  );
}
