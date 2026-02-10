package com.lari.bloggerhub.document;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Represents a blocked user relationship in the Blogger Hub application.
 *
 * <p>When a user blocks another user, interactions are prevented including: - Viewing each other's
 * posts - Commenting on each other's posts - Following each other - Sending notifications
 */
@Document("blocked_users")
@CompoundIndex(def = "{'blocker_id': 1, 'blocked_id': 1}", unique = true)
public class BlockedUser {

  @Id private String id;

  @Field("blocker_id")
  @Indexed
  private String blockerId; // User who initiated the block

  @Field("blocked_id")
  @Indexed
  private String blockedId; // User who is blocked

  @Field("reason")
  private String reason; // Optional reason for blocking

  @CreatedDate
  @Field("blocked_at")
  private Instant blockedAt;

  public BlockedUser() {
    // Default constructor
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getBlockerId() {
    return blockerId;
  }

  public void setBlockerId(String blockerId) {
    this.blockerId = blockerId;
  }

  public String getBlockedId() {
    return blockedId;
  }

  public void setBlockedId(String blockedId) {
    this.blockedId = blockedId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Instant getBlockedAt() {
    return blockedAt;
  }

  public void setBlockedAt(Instant blockedAt) {
    this.blockedAt = blockedAt;
  }

  @Override
  public String toString() {
    return "BlockedUser{"
        + "id='"
        + id
        + '\''
        + ", blockerId='"
        + blockerId
        + '\''
        + ", blockedId='"
        + blockedId
        + '\''
        + ", blockedAt="
        + blockedAt
        + '}';
  }
}
