package com.lari.bloggerhub.controller.notification;

import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing notifications.
 *
 * <p>Provides endpoints for fetching, reading, and deleting notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /**
   * Gets notifications for the authenticated user.
   *
   * @param authentication the authentication object
   * @param page page number (default: 0)
   * @param size page size (default: 20)
   * @param unreadOnly fetch only unread notifications (default: false)
   * @return response with notifications
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @GetMapping
  public ResponseEntity<Response> getNotifications(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "false") boolean unreadOnly) {

    log.info("Fetching notifications: page={}, size={}, unreadOnly={}", page, size, unreadOnly);
    return notificationService.getNotifications(authentication, page, size, unreadOnly);
  }

  /**
   * Gets the count of unread notifications.
   *
   * @param authentication the authentication object
   * @return response with unread count
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @GetMapping("/unread-count")
  public ResponseEntity<Response> getUnreadCount(Authentication authentication) {
    log.info("Fetching unread notification count");
    return notificationService.getUnreadCount(authentication);
  }

  /**
   * Marks a notification as read.
   *
   * @param notificationId the notification ID
   * @param authentication the authentication object
   * @return response indicating success or failure
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @PatchMapping("/{notificationId}/read")
  public ResponseEntity<Response> markAsRead(
      @PathVariable String notificationId, Authentication authentication) {

    log.info("Marking notification as read: {}", notificationId);
    return notificationService.markAsRead(notificationId, authentication);
  }

  /**
   * Marks all notifications as read.
   *
   * @param authentication the authentication object
   * @return response indicating success
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @PatchMapping("/read-all")
  public ResponseEntity<Response> markAllAsRead(Authentication authentication) {
    log.info("Marking all notifications as read");
    return notificationService.markAllAsRead(authentication);
  }

  /**
   * Deletes a notification.
   *
   * @param notificationId the notification ID
   * @param authentication the authentication object
   * @return response indicating success or failure
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Response> deleteNotification(
      @PathVariable String notificationId, Authentication authentication) {

    log.info("Deleting notification: {}", notificationId);
    return notificationService.deleteNotification(notificationId, authentication);
  }
}
