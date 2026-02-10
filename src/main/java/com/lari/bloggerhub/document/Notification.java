package com.lari.bloggerhub.document;

import com.lari.bloggerhub.enums.NotificationType;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Represents a notification entity in the Blogger Hub application.
 *
 * <p>Notifications are triggered by various user interactions such as new followers, likes,
 * comments, and mentions. This entity stores notification details for users to view and manage.
 */
@Document("notifications")
@CompoundIndex(def = "{'user_id': 1, 'is_read': 1, 'created_at': -1}")
public class Notification {

  @Id private String id;

  @Field("user_id")
  @Indexed
  private String userId; // Recipient of the notification

  @Field("type")
  @Indexed
  private NotificationType type;

  @Field("actor_id")
  private String actorId; // User who triggered the notification

  @Field("actor_username")
  private String actorUsername;

  @Field("actor_profile_picture")
  private String actorProfilePicture;

  @Field("target_id")
  private String targetId; // Post/Comment/User ID related to the notification

  @Field("target_type")
  private String targetType; // "post", "comment", "user"

  @Field("target_title")
  private String targetTitle; // Post title or comment preview for context

  @Field("content")
  private String content; // Preview text (e.g., comment content)

  @Field("message")
  private String message; // Formatted notification message

  @Field("is_read")
  @Indexed
  private boolean isRead = false;

  @CreatedDate
  @Field("created_at")
  private Instant createdAt;

  @Field("read_at")
  private Instant readAt;

  public Notification() {
    // Default constructor
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public String getActorUsername() {
    return actorUsername;
  }

  public void setActorUsername(String actorUsername) {
    this.actorUsername = actorUsername;
  }

  public String getActorProfilePicture() {
    return actorProfilePicture;
  }

  public void setActorProfilePicture(String actorProfilePicture) {
    this.actorProfilePicture = actorProfilePicture;
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

  public String getTargetTitle() {
    return targetTitle;
  }

  public void setTargetTitle(String targetTitle) {
    this.targetTitle = targetTitle;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isRead() {
    return isRead;
  }

  public void setRead(boolean read) {
    isRead = read;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public void setReadAt(Instant readAt) {
    this.readAt = readAt;
  }

  @Override
  public String toString() {
    return "Notification{"
        + "id='"
        + id
        + '\''
        + ", userId='"
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
        + ", isRead="
        + isRead
        + ", createdAt="
        + createdAt
        + '}';
  }
}
