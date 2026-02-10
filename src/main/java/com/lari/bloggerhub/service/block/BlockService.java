package com.lari.bloggerhub.service.block;

import com.lari.bloggerhub.constant.Constant;
import com.lari.bloggerhub.document.BlockedUser;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.repository.BlockedUserRepository;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.response.DataResponse;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.response.SuccessResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user blocking functionality.
 *
 * <p>Handles blocking and unblocking users, and checking block status.
 */
@Service
public class BlockService {

  private static final Logger log = LoggerFactory.getLogger(BlockService.class);

  private final BlockedUserRepository blockedUserRepository;
  private final BlogUserRepository blogUserRepository;

  public BlockService(
      BlockedUserRepository blockedUserRepository, BlogUserRepository blogUserRepository) {
    this.blockedUserRepository = blockedUserRepository;
    this.blogUserRepository = blogUserRepository;
  }

  /**
   * Blocks a user.
   *
   * @param userIdToBlock the ID of the user to block
   * @param reason optional reason for blocking
   * @param authentication the authentication object
   * @return response indicating success or failure
   */
  @Transactional
  public ResponseEntity<Response> blockUser(
      String userIdToBlock, String reason, Authentication authentication) {

    BlogUser currentUser = (BlogUser) authentication.getPrincipal();

    // Cannot block yourself
    if (currentUser.getId().equals(userIdToBlock)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(
              new ErrorResponse(
                  false, HttpStatus.BAD_REQUEST.value(), "Cannot block yourself", null));
    }

    // Check if user to block exists
    BlogUser userToBlock = blogUserRepository.findById(userIdToBlock).orElse(null);
    if (userToBlock == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(
              new ErrorResponse(false, HttpStatus.NOT_FOUND.value(), "User not found", null));
    }

    // Check if already blocked
    if (blockedUserRepository.existsByBlockerIdAndBlockedId(
        currentUser.getId(), userIdToBlock)) {
      return ResponseEntity.ok(
          new SuccessResponse(
              true, HttpStatus.OK.value(), "User is already blocked"));
    }

    // Create block relationship
    BlockedUser blockedUser = new BlockedUser();
    blockedUser.setBlockerId(currentUser.getId());
    blockedUser.setBlockedId(userIdToBlock);
    blockedUser.setReason(reason);
    blockedUserRepository.save(blockedUser);

    log.info("User {} blocked user {}", currentUser.getId(), userIdToBlock);

    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "User blocked successfully"));
  }

  /**
   * Unblocks a user.
   *
   * @param userIdToUnblock the ID of the user to unblock
   * @param authentication the authentication object
   * @return response indicating success or failure
   */
  @Transactional
  public ResponseEntity<Response> unblockUser(
      String userIdToUnblock, Authentication authentication) {

    BlogUser currentUser = (BlogUser) authentication.getPrincipal();

    // Check if block exists
    if (!blockedUserRepository.existsByBlockerIdAndBlockedId(
        currentUser.getId(), userIdToUnblock)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(
              new ErrorResponse(
                  false, HttpStatus.NOT_FOUND.value(), "Block relationship not found", null));
    }

    // Delete block relationship
    blockedUserRepository.deleteByBlockerIdAndBlockedId(currentUser.getId(), userIdToUnblock);

    log.info("User {} unblocked user {}", currentUser.getId(), userIdToUnblock);

    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "User unblocked successfully"));
  }

  /**
   * Gets the list of blocked users for the current user.
   *
   * @param authentication the authentication object
   * @param page page number
   * @param size page size
   * @return response with blocked users list
   */
  public ResponseEntity<Response> getBlockedUsers(
      Authentication authentication, int page, int size) {

    BlogUser currentUser = (BlogUser) authentication.getPrincipal();
    Pageable pageable = PageRequest.of(page, size);

    Page<BlockedUser> blockedUsersPage =
        blockedUserRepository.findByBlockerId(currentUser.getId(), pageable);

    // Get full user details for blocked users
    List<Map<String, Object>> blockedUsersDetails =
        blockedUsersPage.getContent().stream()
            .map(
                blockedUser -> {
                  BlogUser user =
                      blogUserRepository.findById(blockedUser.getBlockedId()).orElse(null);
                  Map<String, Object> details = new HashMap<>();
                  details.put("blockId", blockedUser.getId());
                  details.put("blockedAt", blockedUser.getBlockedAt());
                  details.put("reason", blockedUser.getReason());
                  if (user != null) {
                    details.put("userId", user.getId());
                    details.put("username", user.getUsername());
                    details.put("profilePicture", user.getProfilePicture());
                    details.put("bio", user.getBio());
                  }
                  return details;
                })
            .toList();

    Map<String, Object> responseData = new HashMap<>();
    responseData.put("blockedUsers", blockedUsersDetails);
    responseData.put(
        "pagination",
        Map.of(
            "page", page,
            "size", size,
            "totalElements", blockedUsersPage.getTotalElements(),
            "totalPages", blockedUsersPage.getTotalPages()));

    return ResponseEntity.ok(
        new DataResponse(
            true, HttpStatus.OK.value(), "Blocked users fetched successfully", responseData));
  }

  /**
   * Checks if a user is blocked.
   *
   * @param userId the ID of the user to check
   * @param authentication the authentication object
   * @return response with block status
   */
  public ResponseEntity<Response> isUserBlocked(String userId, Authentication authentication) {

    BlogUser currentUser = (BlogUser) authentication.getPrincipal();

    boolean isBlocked =
        blockedUserRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), userId);

    return ResponseEntity.ok(
        new DataResponse(
            true,
            HttpStatus.OK.value(),
            "Block status fetched successfully",
            Map.of("isBlocked", isBlocked)));
  }

  /**
   * Checks if two users have blocked each other (either direction).
   *
   * @param userId1 first user ID
   * @param userId2 second user ID
   * @return true if either user has blocked the other
   */
  public boolean areUsersBlocked(String userId1, String userId2) {
    return blockedUserRepository.existsByBlockerIdAndBlockedId(userId1, userId2)
        || blockedUserRepository.existsByBlockerIdAndBlockedId(userId2, userId1);
  }

  /**
   * Checks if a specific user is blocked by the current user.
   *
   * @param currentUserId the current user's ID
   * @param targetUserId the target user's ID
   * @return true if the current user has blocked the target user
   */
  public boolean hasUserBlocked(String currentUserId, String targetUserId) {
    return blockedUserRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId);
  }
}
