package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.BlogUser;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * This interface extends the {@link MongoRepository} interface provided by Spring Data MongoDB to
 * manage {@link BlogUser} entities in the database.
 *
 * <p>It includes custom methods to query the database for user entities by their username or email
 * address. The interface also provides methods to check if a user with a specific username or email
 * address already exists in the database.
 */
@Repository
public interface BlogUserRepository extends MongoRepository<BlogUser, String> {

  /**
   * Finds a BlogUser by their username.
   *
   * @param username the username of the BlogUser
   * @return an Optional containing the BlogUser if found, or empty if not found
   */
  Optional<BlogUser> findByUsername(String username);

  /**
   * Finds a BlogUser by their email address.
   *
   * @param email the email address of the BlogUser
   * @return an Optional containing the BlogUser if found, or empty if not found
   */
  Optional<BlogUser> findByEmail(String email);

  /**
   * Checks if a BlogUser with the given username exists in the database.
   *
   * @param username the username to check
   * @return true if a BlogUser with the username exists, false otherwise
   */
  boolean existsByUsername(String username);

  /**
   * Checks if a BlogUser with the given email address exists in the database.
   *
   * @param email the email address to check
   * @return true if a BlogUser with the email address exists, false otherwise
   */
  boolean existsByEmail(String email);
}
