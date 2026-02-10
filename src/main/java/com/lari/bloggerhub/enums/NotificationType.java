package com.lari.bloggerhub.enums;

/**
 * Enum representing the different types of notifications in the Blogger Hub application.
 *
 * <p>Each notification type corresponds to a specific user interaction or event that triggers a
 * notification.
 */
public enum NotificationType {
  /** Notification when a user starts following you */
  NEW_FOLLOWER,

  /** Notification when someone likes your blog post */
  POST_LIKED,

  /** Notification when someone comments on your blog post */
  POST_COMMENTED,

  /** Notification when someone replies to your comment */
  COMMENT_REPLIED,

  /** Notification when someone likes your comment */
  COMMENT_LIKED,

  /** Notification when someone mentions you in a post */
  MENTION_IN_POST,

  /** Notification when someone mentions you in a comment */
  MENTION_IN_COMMENT
}
