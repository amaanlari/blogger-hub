package com.lari.bloggerhub.dto.response;

import com.lari.bloggerhub.enums.NotificationType;
import java.time.Instant;

/**
 * Response DTO for notification data.
 *
 * <p>This DTO is returned when fetching notifications via the API.
 */
public class NotificationResponseDto {

  private String id;
  private NotificationType type;
  private ActorInfo actor;
  private TargetInfo target;
  private String message;
  private String preview;
  private boolean isRead;
  private Instant createdAt;
  private Instant readAt;

  public NotificationResponseDto() {
    // Default constructor
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public NotificationType getType() {
    return type;
  }

  public void setType(NotificationType type) {
    this.type = type;
  }

  public ActorInfo getActor() {
    return actor;
  }

  public void setActor(ActorInfo actor) {
    this.actor = actor;
  }

  public TargetInfo getTarget() {
    return target;
  }

  public void setTarget(TargetInfo target) {
    this.target = target;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getPreview() {
    return preview;
  }

  public void setPreview(String preview) {
    this.preview = preview;
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

  /** Nested class for actor information */
  public static class ActorInfo {
    private String id;
    private String username;
    private String profilePicture;

    public ActorInfo() {}

    public ActorInfo(String id, String username, String profilePicture) {
      this.id = id;
      this.username = username;
      this.profilePicture = profilePicture;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getProfilePicture() {
      return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
      this.profilePicture = profilePicture;
    }
  }

  /** Nested class for target information */
  public static class TargetInfo {
    private String id;
    private String type; // "post", "comment", "user"
    private String title;

    public TargetInfo() {}

    public TargetInfo(String id, String type, String title) {
      this.id = id;
      this.type = type;
      this.title = title;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }
  }
}
