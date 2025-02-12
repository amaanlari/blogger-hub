package com.lari.bloggerhub.service.blogpost;

import com.lari.bloggerhub.constant.Constant;
import com.lari.bloggerhub.document.BlogPost;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.dto.request.BlogPostRequestDto;
import com.lari.bloggerhub.dto.response.BlogPostResponseDto;
import com.lari.bloggerhub.dto.response.BlogUserRef;
import com.lari.bloggerhub.enums.Role;
import com.lari.bloggerhub.repository.BlogPostRepository;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.response.DataResponse;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.response.SuccessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class BlogPostService {

  private static final Logger log = LoggerFactory.getLogger(BlogPostService.class);

  private final BlogPostRepository blogPostRepository;
  private final BlogUserRepository blogUserRepository;

  public BlogPostService(
      BlogPostRepository blogPostRepository, BlogUserRepository blogUserRepository) {
    this.blogPostRepository = blogPostRepository;
    this.blogUserRepository = blogUserRepository;
  }

  public ResponseEntity<Response> createBlogPost(
      BlogPostRequestDto blogPostRequestDto, Authentication authentication) {
    authentication.getPrincipal();
    BlogUser currentUser = (BlogUser) authentication.getPrincipal();
    BlogPost blogPost = new BlogPost();
    BeanUtils.copyProperties(blogPostRequestDto, blogPost);
    BlogUserRef currentUserRef = new BlogUserRef();
    BeanUtils.copyProperties(currentUser, currentUserRef);
    log.info("Current user ref: {}", currentUserRef);
    blogPost.setCreatedBy(currentUserRef);
    blogPost.setUpdatedBy(currentUserRef);
    blogPostRepository.save(blogPost);
    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "Blog post created successfully"));
  }

  public ResponseEntity<Response> getBlogPostById(String id, Authentication authentication) {
    BlogPost blogPost = blogPostRepository.findById(id).orElse(null);
    if (blogPost == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(
              new ErrorResponse(
                  false, HttpStatus.NOT_FOUND.value(), Constant.POST_NOT_FOUND, null));
    }
    if (blogPost.isPremium()
        && authentication.getPrincipal() instanceof BlogUser user
        && !user.getRoles().contains(Role.PREMIUM_USER)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              new ErrorResponse(false, HttpStatus.FORBIDDEN.value(), Constant.PREMIUM_POST, null));
    }
    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "Blog post found successfully.", blogPost));
  }

  public ResponseEntity<Response> getAllBlogPostsByUserId(String userId) {
    List<BlogPostResponseDto> blogPosts =
        blogPostRepository.findAllByCreatedBy_Id(userId).stream()
            .map(
                blogPost -> {
                  BlogPostResponseDto blogPostResponseDto = new BlogPostResponseDto();
                  BeanUtils.copyProperties(blogPost, blogPostResponseDto);
                  return blogPostResponseDto;
                })
            .toList();
    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "Blog posts found successfully.", blogPosts));
  }

  public ResponseEntity<Response> getAllBlogPostsByUsername(String username) {
    BlogUser blogUser =
        blogUserRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    BlogUserRef currentUserRef = new BlogUserRef();
    BeanUtils.copyProperties(blogUser, currentUserRef);

    List<BlogPostResponseDto> blogPosts =
        blogPostRepository.findAllByCreatedBy(currentUserRef).stream()
            .map(
                blogPost -> {
                  BlogPostResponseDto blogPostResponseDto = new BlogPostResponseDto();
                  BeanUtils.copyProperties(blogPost, blogPostResponseDto);
                  blogPostResponseDto.setCreatedBy(blogPost.getCreatedBy().getUsername());
                  blogPostResponseDto.setUpdatedBy(blogPost.getUpdatedBy().getUsername());
                  return blogPostResponseDto;
                })
            .toList();
    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "Blog posts found successfully.", blogPosts));
  }

  public ResponseEntity<Response> updateBlogPost(
      String id, BlogPostRequestDto updatedBlogPost, Authentication authentication) {
    BlogPost blogPost = blogPostRepository.findById(id).orElse(null);
    if (blogPost == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(
              new ErrorResponse(
                  false, HttpStatus.NOT_FOUND.value(), Constant.POST_NOT_FOUND, null));
    }
    BlogUser currentUser = (BlogUser) authentication.getPrincipal();
    if (!blogPost.getCreatedBy().getId().equals(currentUser.getId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.FORBIDDEN.value(),
                  Constant.LOGGED_IN_USER_ID_NOT_MATCH_REQUESTED_USER_ID,
                  null));
    }
    BeanUtils.copyProperties(updatedBlogPost, blogPost);
    log.info("Updating blog post: {}", blogPost);
    log.info("Blog post payload: {}", updatedBlogPost);
    blogPost.setUpdatedAt(Instant.now());
    blogPostRepository.save(blogPost);
    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "Blog post updated successfully"));
  }

  public ResponseEntity<Response> deleteBlogPost(String id, Authentication authentication) {
    BlogPost blogPost = blogPostRepository.findById(id).orElse(null);
    if (blogPost == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(
              new ErrorResponse(
                  false, HttpStatus.NOT_FOUND.value(), Constant.POST_NOT_FOUND, null));
    }
    BlogUser currentUser = (BlogUser) authentication.getPrincipal();
    if (!blogPost.getCreatedBy().getId().equals(currentUser.getId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.FORBIDDEN.value(),
                  Constant.LOGGED_IN_USER_ID_NOT_MATCH_REQUESTED_USER_ID,
                  null));
    }
    blogPostRepository.deleteById(id);
    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "Blog post deleted successfully"));
  }

  public ResponseEntity<Response> updateBlogPostPremiumStatus(String id, boolean isPremium) {
    BlogPost blogPost = blogPostRepository.findById(id).orElse(null);
    if (blogPost == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(
              new ErrorResponse(false, HttpStatus.NOT_FOUND.value(), "Blog post not found", null));
    }
    blogPost.setPremium(isPremium);
    blogPost.setUpdatedAt(Instant.now());
    blogPostRepository.save(blogPost);
    return ResponseEntity.ok(
        new SuccessResponse(
            true, HttpStatus.OK.value(), "Blog post premium status updated successfully"));
  }
}
