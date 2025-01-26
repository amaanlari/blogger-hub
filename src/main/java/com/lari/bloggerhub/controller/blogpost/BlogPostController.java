package com.lari.bloggerhub.controller.blogpost;

import com.lari.bloggerhub.dto.request.BlogPostRequestDto;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.service.blogpost.BlogPostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BlogPostController {

  private final BlogPostService blogPostService;

  public BlogPostController(BlogPostService blogPostService) {
    this.blogPostService = blogPostService;
  }

  @GetMapping("/api/user/{username}/blogposts")
  public ResponseEntity<Response> getBlogPostsByUsername(@PathVariable String username) {
    return blogPostService.getAllBlogPostsByUsername(username);
  }

  @GetMapping("/api/blogposts/{id}")
  public ResponseEntity<Response> getBlogPostById(@PathVariable String id, Authentication authentication) {
    return blogPostService.getBlogPostById(id, authentication);
  }

  @PostMapping("/api/blogposts")
  public ResponseEntity<Response> createBlogPost(
      @RequestBody BlogPostRequestDto blogPost, Authentication authentication) {
    return blogPostService.createBlogPost(blogPost, authentication);
  }

  @PutMapping("/api/blogposts/{id}")
  public ResponseEntity<Response> updateBlogPost(
      @PathVariable String id, BlogPostRequestDto updatedBlogPost, Authentication authentication) {
    return blogPostService.updateBlogPost(id, updatedBlogPost, authentication);
  }

  @DeleteMapping("/api/blogposts/{id}")
  public ResponseEntity<Response> deleteBlogPost(
      @PathVariable String id, Authentication authentication) {
    return blogPostService.deleteBlogPost(id, authentication);
  }

  @PatchMapping("/{id}/premium")
  public ResponseEntity<Response> updateBlogPostPremiumStatus(
      @PathVariable String id, @RequestParam boolean isPremium) {
    return blogPostService.updateBlogPostPremiumStatus(id, isPremium);
  }
}
