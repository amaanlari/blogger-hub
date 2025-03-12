package com.lari.bloggerhub.service.interactions;

import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.document.Comments;
import com.lari.bloggerhub.repository.BlogPostRepository;
import com.lari.bloggerhub.repository.CommentsRepository;
import com.lari.bloggerhub.response.DataResponse;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CommentsService {

  private static final Logger log = LoggerFactory.getLogger(CommentsService.class);
  private final CommentsRepository commentsRepository;
  private final BlogPostRepository blogPostRepository;

  public CommentsService(
      CommentsRepository commentsRepository, BlogPostRepository blogPostRepository) {
    this.commentsRepository = commentsRepository;
    this.blogPostRepository = blogPostRepository;
  }

  public ResponseEntity<Response> addComment(
      String commentContent, String postId, String parentId, Authentication authentication) {
    String userId = ((BlogUser) authentication.getPrincipal()).getId();
    if (postId == null || !blogPostRepository.existsById(postId)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ErrorResponse(false, HttpStatus.NOT_FOUND.value(), "Post not found", null));
    }
    Comments comment = new Comments();
    comment.setUserId(userId);
    comment.setPostId(postId);
    comment.setContent(commentContent);
    comment.setParentId((parentId));

    log.info("Comment: {}", comment);

    return ResponseEntity.ok(
        new DataResponse(
            true,
            HttpStatus.OK.value(),
            "Comment added successfully",
            commentsRepository.save(comment)));
  }

  public ResponseEntity<Response> removeComment(String commentId, Authentication authentication) {
    String userId = ((BlogUser) authentication.getPrincipal()).getId();
    Comments comment = commentsRepository.findById(commentId).orElse(null);
    assert comment != null;
    if (!userId.equals(comment.getUserId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              new ErrorResponse(false, HttpStatus.FORBIDDEN.value(), "Unauthorized access", null));
    }
    if (comment.getParentId() == null) commentsRepository.deleteCommentsByParentId(commentId);
    commentsRepository.deleteById(commentId);

    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "Comment removed successfully", comment));
  }

  public ResponseEntity<Response> getCommentsByPostId(String postId) {
    return ResponseEntity.ok(
        new DataResponse(
            true,
            HttpStatus.OK.value(),
            "Comments retrieved successfully",
            commentsRepository.findCommentsByPostId((postId))));
  }
}
