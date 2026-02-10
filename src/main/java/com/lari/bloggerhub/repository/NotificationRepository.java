package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.Notification;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Notification entities.
 *
 * <p>Provides methods for querying and managing user notifications.
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

  /**
   * Find all notifications for a specific user, ordered by creation date (newest first).
   *
   * @param userId the ID of the user
   * @param pageable pagination information
   * @return page of notifications
   */
  Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

  /**
   * Find unread notifications for a specific user.
   *
   * @param userId the ID of the user
   * @return list of unread notifications
   */
  List<Notification> findByUserIdAndIsReadFalse(String userId);

  /**
   * Find unread notifications for a specific user with pagination.
   *
   * @param userId the ID of the user
   * @param pageable pagination information
   * @return page of unread notifications
   */
  Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
      String userId, Pageable pageable);

  /**
   * Count unread notifications for a specific user.
   *
   * @param userId the ID of the user
   * @return count of unread notifications
   */
  long countByUserIdAndIsReadFalse(String userId);

  /**
   * Delete notifications older than a specific date.
   *
   * @param date the cutoff date
   */
  void deleteByCreatedAtBefore(Instant date);

  /**
   * Delete all notifications for a specific user.
   *
   * @param userId the ID of the user
   */
  void deleteByUserId(String userId);
}
