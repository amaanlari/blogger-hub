package com.lari.bloggerhub.controller.block;

import com.lari.bloggerhub.dto.request.BlockUserRequestDto;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.service.block.BlockService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing user blocking.
 *
 * <p>Provides endpoints for blocking, unblocking, and managing blocked users.
 */
@RestController
@RequestMapping("/api/block")
public class BlockController {

  private static final Logger log = LoggerFactory.getLogger(BlockController.class);
  private final BlockService blockService;

  public BlockController(BlockService blockService) {
    this.blockService = blockService;
  }

  /**
   * Blocks a user.
   *
   * @param request the block user request containing user ID and optional reason
   * @param authentication the authentication object
   * @return response indicating success or failure
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @PostMapping
  public ResponseEntity<Response> blockUser(
      @Valid @RequestBody BlockUserRequestDto request, Authentication authentication) {

    log.info("Blocking user: userId={}", request.getUserId());
    return blockService.blockUser(request.getUserId(), request.getReason(), authentication);
  }

  /**
   * Unblocks a user.
   *
   * @param userId the ID of the user to unblock
   * @param authentication the authentication object
   * @return response indicating success or failure
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @DeleteMapping("/{userId}")
  public ResponseEntity<Response> unblockUser(
      @PathVariable String userId, Authentication authentication) {

    log.info("Unblocking user: userId={}", userId);
    return blockService.unblockUser(userId, authentication);
  }

  /**
   * Gets the list of blocked users.
   *
   * @param authentication the authentication object
   * @param page page number (default: 0)
   * @param size page size (default: 20)
   * @return response with blocked users list
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @GetMapping
  public ResponseEntity<Response> getBlockedUsers(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    log.info("Fetching blocked users: page={}, size={}", page, size);
    return blockService.getBlockedUsers(authentication, page, size);
  }

  /**
   * Checks if a user is blocked.
   *
   * @param userId the ID of the user to check
   * @param authentication the authentication object
   * @return response with block status
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @GetMapping("/check/{userId}")
  public ResponseEntity<Response> isUserBlocked(
      @PathVariable String userId, Authentication authentication) {

    log.info("Checking if user is blocked: userId={}", userId);
    return blockService.isUserBlocked(userId, authentication);
  }
}
