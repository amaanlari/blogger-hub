package com.lari.bloggerhub.controller.interactions;

import com.lari.bloggerhub.dto.request.interactions.CommentRequestDto;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.service.interactions.CommentsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interactions/comments")
public class CommentsController {

  private static final Logger log = LoggerFactory.getLogger(CommentsController.class);
  private final CommentsService commentsService;

  public CommentsController(CommentsService commentsService) {
    this.commentsService = commentsService;
  }

  /*
   * Add comments
   * Remove comments
   * reply comments
   * like comments
   * Get comments
   * */
  @PostMapping
  public ResponseEntity<Response> addComment(
      @RequestBody CommentRequestDto commentRequestDto, Authentication authentication) {
    log.info("Received request to add a comment");
    log.info("CommentDto: {}", commentRequestDto);
    return commentsService.addComment(
        commentRequestDto.getContent(), commentRequestDto.getPostId(), null, authentication);
  }

  @DeleteMapping
  public ResponseEntity<Response> removeComment(
      @RequestParam("comment_id") String commentId, Authentication authentication) {
    return commentsService.removeComment(commentId, authentication);
  }

  @PostMapping("/reply")
  public ResponseEntity<Response> replyComment(
      @RequestBody CommentRequestDto commentRequestDto, Authentication authentication) {
    return commentsService.addComment(
        commentRequestDto.getContent(),
        commentRequestDto.getPostId(),
        commentRequestDto.getParentId(),
        authentication);
  }

  @GetMapping
  public ResponseEntity<Response> getComments(@RequestParam("post_id") String postId) {
    return commentsService.getCommentsByPostId(postId);
  }
}
