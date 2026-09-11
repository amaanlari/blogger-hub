import { api } from '@/shared/lib/http';
import type { NotificationsPage } from '@/shared/types/models';

export const notificationsApi = {
  /**
   * Note `unreadOnly` in camelCase. Query parameters bind by Java parameter name and are untouched
   * by Jackson's snake_case strategy, which only applies to JSON bodies — so `unread_only` here
   * would be silently ignored and return every notification.
   */
  list: (params: { page?: number; size?: number; unreadOnly?: boolean } = {}) =>
    api.get<NotificationsPage>('/notifications', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        unreadOnly: params.unreadOnly ?? false,
      },
    }),

  unreadCount: () => api.get<{ count: number }>('/notifications/unread-count'),

  markRead: (notificationId: string) =>
    api.patch<void>(`/notifications/${notificationId}/read`),

  markAllRead: () => api.patch<void>('/notifications/read-all'),

  remove: (notificationId: string) => api.delete<void>(`/notifications/${notificationId}`),
};
