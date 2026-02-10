package com.lari.bloggerhub.dto.event;

import com.lari.bloggerhub.enums.NotificationType;
import java.io.Serializable;

/**
 * Event DTO for sending notification events through Kafka.
 *
 * <p>This class is serialized and sent to Kafka topics when a notification-triggering event
 * occurs.
 */
public class NotificationEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private String userId; // Recipient
  private NotificationType type;
  private String actorId; // User who triggered the notification
  private String targetId; // Post/Comment/User ID
  private String targetType; // "post", "comment", "user"
  private String content; // Optional content (e.g., comment text)

  public NotificationEvent() {
    // Default constructor for serialization
  }

  public NotificationEvent(
      String userId,
      NotificationType type,
      String actorId,
      String targetId,
      String targetType,
      String content) {
    this.userId = userId;
    this.type = type;
    this.actorId = actorId;
    this.targetId = targetId;
    this.targetType = targetType;
    this.content = content;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public NotificationType getType() {
    return type;
  }

  public void setType(NotificationType type) {
    this.type = type;
  }

  public String getActorId() {
    return actorId;
  }

  public void setActorId(String actorId) {
    this.actorId = actorId;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "NotificationEvent{"
        + "userId='"
        + userId
        + '\''
        + ", type="
        + type
        + ", actorId='"
        + actorId
        + '\''
        + ", targetId='"
        + targetId
        + '\''
        + ", targetType='"
        + targetType
        + '\''
        + '}';
  }
}
