package com.lari.bloggerhub.controller.interactions;

import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.service.interactions.LikesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/interactions/likes")
public class LikesController {

  private static final Logger log = LoggerFactory.getLogger(LikesController.class);
  private final LikesService likesService;

  public LikesController(LikesService likesService) {
    this.likesService = likesService;
  }

  @PostMapping
  public ResponseEntity<Response> addLike(
      @RequestBody Map<String, String> blogPost, Authentication authentication) {
    log.info("Received request to add a like");
    log.info("created postId: {}", blogPost);
    return likesService.addLike(blogPost.get("blog_post_id"), authentication);
  }

  @DeleteMapping
  public ResponseEntity<Response> removeLike(
      @RequestBody Map<String, String> blogPost, Authentication authentication) {
    log.info("Received request to remove a like");
    log.info("deleted postId: {}", blogPost);
    return likesService.removeLike(blogPost.get("blog_post_id"), authentication);
  }

  /**
   * Get all the posts liked by the user
   */
  @GetMapping("/user/posts")
  public ResponseEntity<Response> getUserLikedPosts(Authentication authentication) {
    log.info("Received request to get user liked posts");
    return likesService.getPostsLikedByUser(authentication);
  }

  @GetMapping("/post/users")
  public ResponseEntity<Response> getUsersWhoLikedPost(@RequestBody Map<String, String> payload, Authentication authentication) {
    log.info("Payload: {}", payload);
    return likesService.getUsersWhoLikedPost(payload.get("blog_post_id"), authentication);
  }
}
