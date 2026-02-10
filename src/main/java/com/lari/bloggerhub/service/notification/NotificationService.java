package com.lari.bloggerhub.service.notification;

import com.lari.bloggerhub.constant.Constant;
import com.lari.bloggerhub.document.BlogPost;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.document.Comments;
import com.lari.bloggerhub.document.Notification;
import com.lari.bloggerhub.dto.response.NotificationResponseDto;
import com.lari.bloggerhub.enums.NotificationType;
import com.lari.bloggerhub.repository.BlogPostRepository;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.repository.BlockedUserRepository;
import com.lari.bloggerhub.repository.CommentsRepository;
import com.lari.bloggerhub.repository.NotificationRepository;
import com.lari.bloggerhub.response.DataResponse;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.response.SuccessResponse;
import com.lari.bloggerhub.service.bloguser.EmailService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing notifications.
 *
 * <p>Handles creation, retrieval, and management of user notifications.
 */
@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository notificationRepository;
  private final BlogUserRepository blogUserRepository;
  private final BlogPostRepository blogPostRepository;
  private final CommentsRepository commentsRepository;
  private final BlockedUserRepository blockedUserRepository;
  private final EmailService emailService;

  public NotificationService(
      NotificationRepository notificationRepository,
      BlogUserRepository blogUserRepository,
      BlogPostRepository blogPostRepository,
      CommentsRepository commentsRepository,
      BlockedUserRepository blockedUserRepository,
      EmailService emailService) {
    this.notificationRepository = notificationRepository;
    this.blogUserRepository = blogUserRepository;
    this.blogPostRepository = blogPostRepository;
    this.commentsRepository = commentsRepository;
    this.blockedUserRepository = blockedUserRepository;
    this.emailService = emailService;
  }

  /**
   * Creates a notification in the database.
   *
   * <p>This method is called by the Kafka consumer when processing notification events.
   *
   * @param userId recipient user ID
   * @param type notification type
   * @param actorId actor user ID
   * @param targetId target entity ID
   * @param targetType target entity type
   * @param content optional content
   */
  public void createNotification(
      String userId,
      NotificationType type,
      String actorId,
      String targetId,
      String targetType,
      String content) {

    log.info(
        "Creating notification: userId={}, type={}, actorId={}, targetId={}",
        userId,
        type,
        actorId,
        targetId);

    // Check if users are blocked
    if (blockedUserRepository.existsByBlockerIdAndBlockedId(userId, actorId)
        || blockedUserRepository.existsByBlockerIdAndBlockedId(actorId, userId)) {
      log.info(
          "Skipping notification: users have blocked each other (userId={}, actorId={})",
          userId,
          actorId);
      return;
    }

    // Don't notify if actor is recipient
    if (userId.equals(actorId)) {
      log.debug("Skipping notification: actor and recipient are the same");
      return;
    }

    // Get actor and recipient details
    BlogUser actor = blogUserRepository.findById(actorId).orElse(null);
    if (actor == null) {
      log.error("Actor user not found: {}", actorId);
      return;
    }

    BlogUser recipient = blogUserRepository.findById(userId).orElse(null);
    if (recipient == null) {
      log.error("Recipient user not found: {}", userId);
      return;
    }

    // Create notification
    Notification notification = new Notification();
    notification.setUserId(userId);
    notification.setType(type);
    notification.setActorId(actorId);
    notification.setActorUsername(actor.getUsername());
    notification.setActorProfilePicture(actor.getProfilePicture());
    notification.setTargetId(targetId);
    notification.setTargetType(targetType);

    // Set context-specific fields
    setNotificationDetails(notification, targetType, targetId, type, actor.getUsername(), content);

    notificationRepository.save(notification);
    log.info("Notification created successfully: {}", notification.getId());

    // Send email notification if user has email notifications enabled
    if (recipient.isEmailNotificationsEnabled()) {
      String targetTitle = notification.getTargetTitle() != null ? notification.getTargetTitle() : "";
      String emailContent = content != null ? content : (notification.getContent() != null ? notification.getContent() : "");

      emailService.sendNotificationEmail(
          recipient.getEmail(),
          recipient.getUsername(),
          type,
          actor.getUsername(),
          targetTitle,
          emailContent
      );
      log.info("Email notification sent to {} for notification type {}", recipient.getEmail(), type);
    } else {
      log.debug("Email notifications disabled for user {}", userId);
    }
  }

  /**
   * Sets notification details based on target type and notification type.
   *
   * @param notification the notification object to update
   * @param targetType type of target ("post", "comment", "user")
   * @param targetId ID of the target
   * @param type notification type
   * @param actorUsername username of the actor
   * @param content optional content
   */
  private void setNotificationDetails(
      Notification notification,
      String targetType,
      String targetId,
      NotificationType type,
      String actorUsername,
      String content) {

    switch (targetType) {
      case "post":
        BlogPost post = blogPostRepository.findById(targetId).orElse(null);
        if (post != null) {
          notification.setTargetTitle(post.getTitle());
          notification.setMessage(
              buildNotificationMessage(type, actorUsername, post.getTitle(), null));
        }
        break;

      case "comment":
        Comments comment = commentsRepository.findById(targetId).orElse(null);
        if (comment != null) {
          notification.setContent(truncateContent(comment.getContent(), 100));
          notification.setMessage(
              buildNotificationMessage(type, actorUsername, null, content));
        }
        break;

      case "user":
        notification.setMessage(buildNotificationMessage(type, actorUsername, null, null));
        break;
    }

    if (content != null) {
      notification.setContent(truncateContent(content, 100));
    }
  }

  /**
   * Builds a human-readable notification message.
   *
   * @param type notification type
   * @param actorUsername actor's username
   * @param postTitle optional post-title
   * @param commentContent optional comment content
   * @return formatted message
   */
  private String buildNotificationMessage(
      NotificationType type, String actorUsername, String postTitle, String commentContent) {
    return switch (type) {
      case NEW_FOLLOWER -> actorUsername + " started following you";
      case POST_LIKED -> actorUsername + " liked your post"
              + (postTitle != null ? " \"" + postTitle + "\"" : "");
      case POST_COMMENTED -> actorUsername + " commented on your post"
              + (postTitle != null ? " \"" + postTitle + "\"" : "");
      case COMMENT_REPLIED -> actorUsername + " replied to your comment";
      case COMMENT_LIKED -> actorUsername + " liked your comment";
      case MENTION_IN_POST -> actorUsername + " mentioned you in a post";
      case MENTION_IN_COMMENT -> actorUsername + " mentioned you in a comment";
    };
  }

  /**
   * Truncates content to a maximum length.
   *
   * @param content the content to truncate
   * @param maxLength maximum length
   * @return truncated content
   */
  private String truncateContent(String content, int maxLength) {
    if (content == null || content.length() <= maxLength) {
      return content;
    }
    return content.substring(0, maxLength) + "...";
  }

  /**
   * Gets paginated notifications for the authenticated user.
   *
   * @param authentication the authentication object
   * @param page page number
   * @param size page size
   * @param unreadOnly whether to fetch only unread notifications
   * @return response with notifications
   */
  public ResponseEntity<Response> getNotifications(
      Authentication authentication, int page, int size, boolean unreadOnly) {

    BlogUser currentUser = (BlogUser) authentication.getPrincipal();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Page<Notification> notificationPage;
    if (unreadOnly) {
      notificationPage =
          notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
              currentUser.getId(), pageable);
    } else {
      notificationPage =
          notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
    }

    List<NotificationResponseDto> notifications =
        notificationPage.getContent().stream().map(this::convertToDto).toList();

    long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());

    Map<String, Object> responseData = new HashMap<>();
    responseData.put("notifications", notifications);
    responseData.put("unreadCount", unreadCount);
    responseData.put(
        "pagination",
        Map.of(
            "page", page,
            "size", size,
            "totalElements", notificationPage.getTotalElements(),
            "totalPages", notificationPage.getTotalPages()));

    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "Notifications fetched successfully", responseData));
  }

  /**
   * Gets the count of unread notifications.
   *
   * @param authentication the authentication object
   * @return response with unread count
   */
  public ResponseEntity<Response> getUnreadCount(Authentication authentication) {
    BlogUser currentUser = (BlogUser) authentication.getPrincipal();
    long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());

    return ResponseEntity.ok(
        new DataResponse(
            true, HttpStatus.OK.value(), "Unread count fetched successfully", Map.of("count", unreadCount)));
  }

  /**
   * Marks a notification as read.
   *
   * @param notificationId the notification ID
   * @param authentication the authentication object
   * @return response indicating success or failure
   */
  public ResponseEntity<Response> markAsRead(String notificationId, Authentication authentication) {
    BlogUser currentUser = (BlogUser) authentication.getPrincipal();

    Notification notification = notificationRepository.findById(notificationId).orElse(null);
    if (notification == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(
              new ErrorResponse(
                  false, HttpStatus.NOT_FOUND.value(), "Notification not found", null));
    }

    if (!notification.getUserId().equals(currentUser.getId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.FORBIDDEN.value(),
                  Constant.NOT_AUTHORIZED_TO_ACCESS_PROFILE,
                  null));
    }

    notification.setRead(true);
    notification.setReadAt(Instant.now());
    notificationRepository.save(notification);

    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "Notification marked as read"));
  }

  /**
   * Marks all notifications as read for the current user.
   *
   * @param authentication the authentication object
   * @return response indicating success
   */
  @Transactional
  public ResponseEntity<Response> markAllAsRead(Authentication authentication) {
    BlogUser currentUser = (BlogUser) authentication.getPrincipal();

    List<Notification> unreadNotifications =
        notificationRepository.findByUserIdAndIsReadFalse(currentUser.getId());

    Instant now = Instant.now();
    unreadNotifications.forEach(
        notification -> {
          notification.setRead(true);
          notification.setReadAt(now);
        });

    notificationRepository.saveAll(unreadNotifications);

    return ResponseEntity.ok(
        new SuccessResponse(
            true,
            HttpStatus.OK.value(),
            unreadNotifications.size() + " notifications marked as read"));
  }

  /**
   * Deletes a notification.
   *
   * @param notificationId the notification ID
   * @param authentication the authentication object
   * @return response indicating success or failure
   */
  public ResponseEntity<Response> deleteNotification(
      String notificationId, Authentication authentication) {
    BlogUser currentUser = (BlogUser) authentication.getPrincipal();

    Notification notification = notificationRepository.findById(notificationId).orElse(null);
    if (notification == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(
              new ErrorResponse(
                  false, HttpStatus.NOT_FOUND.value(), "Notification not found", null));
    }

    if (!notification.getUserId().equals(currentUser.getId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.FORBIDDEN.value(),
                  Constant.NOT_AUTHORIZED_TO_ACCESS_PROFILE,
                  null));
    }

    notificationRepository.delete(notification);

    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "Notification deleted successfully"));
  }

  /**
   * Converts a Notification entity to a NotificationResponseDto.
   *
   * @param notification the notification entity
   * @return the notification response DTO
   */
  private NotificationResponseDto convertToDto(Notification notification) {
    NotificationResponseDto dto = new NotificationResponseDto();
    dto.setId(notification.getId());
    dto.setType(notification.getType());
    dto.setMessage(notification.getMessage());
    dto.setPreview(notification.getContent());
    dto.setRead(notification.isRead());
    dto.setCreatedAt(notification.getCreatedAt());
    dto.setReadAt(notification.getReadAt());

    // Set actor info
    NotificationResponseDto.ActorInfo actor =
        new NotificationResponseDto.ActorInfo(
            notification.getActorId(),
            notification.getActorUsername(),
            notification.getActorProfilePicture());
    dto.setActor(actor);

    // Set target info
    NotificationResponseDto.TargetInfo target =
        new NotificationResponseDto.TargetInfo(
            notification.getTargetId(),
            notification.getTargetType(),
            notification.getTargetTitle());
    dto.setTarget(target);

    return dto;
  }
}
