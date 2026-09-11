package com.lari.bloggerhub.service.blogpost;

import com.lari.bloggerhub.constant.Constant;
import com.lari.bloggerhub.document.BlogPost;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.dto.request.BlogPostRequestDto;
import com.lari.bloggerhub.dto.response.BlogPostResponseDto;
import com.lari.bloggerhub.dto.response.BlogPostSummaryDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  /**
   * Paginated, newest-first list of every post, optionally filtered by a search term matched against
   * title and description. Backs the home feed and the explore/search page.
   *
   * <p>Returns {@link BlogPostSummaryDto}, which carries no {@code content} — see that class for why
   * that matters for the premium paywall, and why it makes this endpoint safe to serve anonymously.
   *
   * <p>The pagination envelope deliberately mirrors {@code NotificationService.getNotifications} —
   * a raw map with camelCase {@code totalElements}/{@code totalPages} keys — so the frontend parses
   * one pagination shape across the whole API rather than two.
   *
   * @param page zero-based page index
   * @param size page size
   * @param q optional search term; blank or null lists everything
   * @return response with {@code posts} and {@code pagination}
   */
  public ResponseEntity<Response> listBlogPosts(int page, int size, String q) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Page<BlogPost> postPage;
    if (q == null || q.isBlank()) {
      postPage = blogPostRepository.findAll(pageable);
    } else {
      postPage =
          blogPostRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
              q.trim(), q.trim(), pageable);
    }

    List<BlogPostSummaryDto> posts =
        postPage.getContent().stream().map(this::toSummaryDto).toList();

    Map<String, Object> responseData = new HashMap<>();
    responseData.put("posts", posts);
    responseData.put(
        "pagination",
        Map.of(
            "page", page,
            "size", size,
            "totalElements", postPage.getTotalElements(),
            "totalPages", postPage.getTotalPages()));

    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "Blog posts fetched successfully", responseData));
  }

  /**
   * Flattens a post into its summary projection, resolving the {@link BlogUserRef} author reference
   * down to a plain username string.
   *
   * <p>The null guard is not defensive padding: {@code createdBy} is a {@code @DocumentReference},
   * so a post whose author document was deleted resolves to null here, and a hard-deleted user
   * leaves exactly that behind (nothing cascades author deletion to their posts).
   */
  private BlogPostSummaryDto toSummaryDto(BlogPost blogPost) {
    BlogPostSummaryDto dto = new BlogPostSummaryDto();
    BeanUtils.copyProperties(blogPost, dto);
    dto.setCreatedBy(blogPost.getCreatedBy() != null ? blogPost.getCreatedBy().getUsername() : null);
    return dto;
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
