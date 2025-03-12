package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.BlogUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Optional;

@DataMongoTest
class BlogUserRepositoryTest {

  @Autowired private BlogUserRepository blogUserRepository;

  @BeforeEach
  void setUp() {
    blogUserRepository.deleteAll();
  }

  @Test
  void save_whenUserIsValid_savesUser() {
    BlogUser blogUser = createTestBlogUser();

    BlogUser savedUser = blogUserRepository.save(blogUser);

    Assertions.assertNotNull(savedUser.getId());
    Assertions.assertEquals(blogUser.getUsername(), savedUser.getUsername());
    Assertions.assertEquals(blogUser.getEmail(), savedUser.getEmail());
    Assertions.assertEquals(blogUser.getPassword(), savedUser.getPassword());
    Assertions.assertEquals(blogUser.getBio(), savedUser.getBio());
    Assertions.assertEquals(blogUser.getProfilePicture(), savedUser.getProfilePicture());
  }

  @Test
  void save_whenUserIsNull_throwsException() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> blogUserRepository.save(null));
  }

  @Test
  void findByUsername_whenUserExists_returnsUser() {
    BlogUser blogUser = createTestBlogUser();
    blogUserRepository.save(blogUser);

    Optional<BlogUser> foundUser = blogUserRepository.findByUsername("test");

    Assertions.assertTrue(foundUser.isPresent());
    Assertions.assertEquals("test", foundUser.get().getUsername());
  }

  @Test
  void findByUsername_whenUserDoesNotExist_returnsEmpty() {
    Optional<BlogUser> foundUser = blogUserRepository.findByUsername("nonexistent");

    Assertions.assertFalse(foundUser.isPresent());
  }

  @Test
  void findByEmail_whenUserExists_returnsUser() {
    BlogUser blogUser = createTestBlogUser();
    blogUserRepository.save(blogUser);

    Optional<BlogUser> foundUser = blogUserRepository.findByEmail("test@lari.com");

    Assertions.assertTrue(foundUser.isPresent());
    Assertions.assertEquals("test@lari.com", foundUser.get().getEmail());
  }

  @Test
  void findByEmail_whenUserDoesNotExist_returnsEmpty() {
    Optional<BlogUser> foundUser = blogUserRepository.findByEmail("nonexistent@lari.com");

    Assertions.assertFalse(foundUser.isPresent());
  }

  @Test
  void existsByUsername_whenUserExists_returnsTrue() {
    BlogUser blogUser = createTestBlogUser();
    blogUserRepository.save(blogUser);

    boolean exists = blogUserRepository.existsByUsername("test");

    Assertions.assertTrue(exists);
  }

  @Test
  void existsByUsername_whenUserDoesNotExist_returnsFalse() {
    boolean exists = blogUserRepository.existsByUsername("nonexistent");

    Assertions.assertFalse(exists);
  }

  @Test
  void existsByEmail_whenUserExists_returnsTrue() {
    BlogUser blogUser = createTestBlogUser();
    blogUserRepository.save(blogUser);

    boolean exists = blogUserRepository.existsByEmail("test@lari.com");

    Assertions.assertTrue(exists);
  }

  @Test
  void existsByEmail_whenUserDoesNotExist_returnsFalse() {
    boolean exists = blogUserRepository.existsByEmail("nonexistent@lari.com");

    Assertions.assertFalse(exists);
  }

  private BlogUser createTestBlogUser() {
    BlogUser blogUser = new BlogUser();
    blogUser.setUsername("test");
    blogUser.setEmail("test@lari.com");
    blogUser.setPassword("test.password");
    blogUser.setBio("Test bio");
    blogUser.setProfilePicture("test.jpg");
    return blogUser;
  }
}
