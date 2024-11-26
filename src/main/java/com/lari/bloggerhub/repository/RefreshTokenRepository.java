package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * This interface extends the {@link MongoRepository} interface provided by Spring Data MongoDB to
 * manage {@link RefreshToken} entities in the database.
 *
 * <p>It includes a custom method to delete a refresh token by the ID of the user it belongs to.
 */
@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

  /**
   * Deletes a refresh token by the ID of the user it belongs to.
   *
   * @param id the ID of the user
   */
  @SuppressWarnings("checkstyle:MethodName")
  void deleteByOwner_Id(String id);
}
