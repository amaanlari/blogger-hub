import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationsApi } from '@/features/notifications/api/notificationsApi';
import { useAuth } from '@/features/auth/hooks/useAuth';

export const notificationKeys = {
  all: ['notifications'] as const,
  list: (page: number, unreadOnly: boolean) =>
    ['notifications', 'list', page, unreadOnly] as const,
  unreadCount: ['notifications', 'unread-count'] as const,
};

/**
 * Drives the badge in the header.
 *
 * Polled, because there is no WebSocket or SSE endpoint anywhere in the backend — notifications are
 * produced asynchronously through Kafka and only ever observed by asking.
 */
export function useUnreadCount() {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: notificationKeys.unreadCount,
    queryFn: async () => (await notificationsApi.unreadCount()).count,
    enabled: isAuthenticated,
    refetchInterval: 30_000,
    staleTime: 15_000,
  });
}

export function useNotifications(page: number, unreadOnly: boolean) {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: notificationKeys.list(page, unreadOnly),
    queryFn: () => notificationsApi.list({ page, unreadOnly }),
    enabled: isAuthenticated,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => notificationsApi.markRead(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}

export function useMarkAllRead() {
  const queryClient = useQueryClient();

  return useMutation({
    // The response reports how many were marked only by interpolating the number into its message
    // string, with no field to read it from — so refetch rather than trying to parse it back out.
    mutationFn: () => notificationsApi.markAllRead(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}

export function useDeleteNotification() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => notificationsApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}
