package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.document.RefreshToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

@DataMongoTest
class RefreshTokenRepositoryTest {

  @Autowired RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void setup() {
    refreshTokenRepository.deleteAll();
  }

  @Test
  void save_whenTokenIsValid_savesToken() {
    String userId = "testUserId";
    BlogUser blogUser = new BlogUser();
    blogUser.setId(userId);

    RefreshToken savedRefreshToken = refreshTokenRepository.save(new RefreshToken("token", blogUser));

    Assertions.assertNotNull(savedRefreshToken.getId());
  }

  @Test
  void deleteByOwnerId_whenUserExists_deletesToken() {
    String userId = "existingUserId";
    BlogUser blogUser = new BlogUser();
    blogUser.setId(userId);
    refreshTokenRepository.save(new RefreshToken("token", blogUser));

    Assertions.assertTrue(refreshTokenRepository.findByOwner_Id((userId)).isPresent());
    refreshTokenRepository.deleteByOwner_Id(userId);

    Assertions.assertTrue(refreshTokenRepository.findByOwner_Id((userId)).isEmpty());
  }

  @Test
  void deleteByOwnerId_whenUserDoesNotExist_doesNothing() {
    String userId = "nonexistentUserId";

    Assertions.assertTrue(refreshTokenRepository.findByOwner_Id((userId)).isEmpty());
    refreshTokenRepository.deleteByOwner_Id(userId);

    Assertions.assertTrue(refreshTokenRepository.findByOwner_Id((userId)).isEmpty());
  }
}
