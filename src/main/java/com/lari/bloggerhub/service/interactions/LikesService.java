package com.lari.bloggerhub.service.interactions;

import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.document.Likes;
import com.lari.bloggerhub.dto.response.BlogPostResponseDto;
import com.lari.bloggerhub.dto.response.BlogUserResponseDto;
import com.lari.bloggerhub.repository.BlogPostRepository;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.repository.LikesRepository;
import com.lari.bloggerhub.response.DataResponse;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.response.SuccessResponse;
import com.mongodb.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
public class LikesService {

  private static final Logger log = LoggerFactory.getLogger(LikesService.class);
  private final LikesRepository likesRepository;
  private final BlogPostRepository blogPostRepository;
  private final BlogUserRepository blogUserRepository;

  public LikesService(
      LikesRepository likesRepository,
      BlogPostRepository blogPostRepository,
      BlogUserRepository blogUserRepository) {

    this.likesRepository = likesRepository;
    this.blogPostRepository = blogPostRepository;
    this.blogUserRepository = blogUserRepository;
  }

  public ResponseEntity<Response> addLike(String blogPostId, Authentication authentication)
      throws DuplicateKeyException {

    String userId = ((BlogUser) authentication.getPrincipal()).getId();
    Likes like = new Likes();
    like.setUserId(userId);
    like.setPostId(blogPostId);

    return ResponseEntity.ok(
        new DataResponse(
            true, HttpStatus.OK.value(), "Like added successfully", likesRepository.save(like)));
  }

  public ResponseEntity<Response> removeLike(String blogPostId, Authentication authentication) {
    String userId = ((BlogUser) authentication.getPrincipal()).getId();
    Likes like = likesRepository.findByPostId(blogPostId);
    assert Objects.equals(like.getUserId(), userId);
    likesRepository.delete(like);
    return ResponseEntity.ok()
        .body(new SuccessResponse(true, HttpStatus.OK.value(), "Like removed successfully"));
  }

  public ResponseEntity<Response> getPostsLikedByUser(Authentication authentication) {
    String userId = ((BlogUser) authentication.getPrincipal()).getId();
    List<BlogPostResponseDto> likeBlogPosts =
        blogPostRepository
            .findAllById(
                likesRepository.getLikesByUserId(userId).stream().map(Likes::getPostId).toList())
            .stream()
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
        new DataResponse(
            true,
            HttpStatus.OK.value(),
            String.format("Liked posts by %s", authentication.getName()),
            likeBlogPosts));
  }

  public ResponseEntity<Response> getUsersWhoLikedPost(
      String blogPostId, Authentication authentication) {
    log.info("Authenticated user: {}", authentication.getName());

    if (!Objects.equals(
        blogPostRepository.getByBlogPostId(blogPostId).getCreatedBy().getId(),
        ((BlogUser) authentication.getPrincipal()).getId()))
      throw new BadCredentialsException("Invalid user");

    List<BlogUserResponseDto> likedUsersList =
        blogUserRepository
            .findAllById(
                likesRepository.getLikesByPostId(blogPostId).stream()
                    .map(Likes::getUserId)
                    .toList())
            .stream()
            .map(
                user -> {
                  BlogUserResponseDto userResponseDto = new BlogUserResponseDto();
                  BeanUtils.copyProperties(user, userResponseDto);
                  return userResponseDto;
                })
            .toList();
    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "Liked users list", likedUsersList));
  }
}
