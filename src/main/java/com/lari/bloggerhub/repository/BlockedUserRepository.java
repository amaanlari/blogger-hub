package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.BlockedUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing BlockedUser entities.
 *
 * <p>Provides methods for querying and managing user blocking relationships.
 */
@Repository
public interface BlockedUserRepository extends MongoRepository<BlockedUser, String> {

  /**
   * Check if a user has blocked another user.
   *
   * @param blockerId the ID of the user who might have blocked
   * @param blockedId the ID of the potentially blocked user
   * @return true if the block relationship exists
   */
  boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

  /**
   * Find a specific block relationship.
   *
   * @param blockerId the ID of the blocker
   * @param blockedId the ID of the blocked user
   * @return Optional containing the BlockedUser if found
   */
  Optional<BlockedUser> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

  /**
   * Find all users blocked by a specific user.
   *
   * @param blockerId the ID of the blocker
   * @param pageable pagination information
   * @return page of blocked users
   */
  Page<BlockedUser> findByBlockerId(String blockerId, Pageable pageable);

  /**
   * Find all users blocked by a specific user (list).
   *
   * @param blockerId the ID of the blocker
   * @return list of blocked users
   */
  List<BlockedUser> findByBlockerId(String blockerId);

  /**
   * Count how many users a specific user has blocked.
   *
   * @param blockerId the ID of the blocker
   * @return count of blocked users
   */
  long countByBlockerId(String blockerId);

  /**
   * Delete a specific block relationship.
   *
   * @param blockerId the ID of the blocker
   * @param blockedId the ID of the blocked user
   */
  void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);
}
