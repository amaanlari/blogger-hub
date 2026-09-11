package com.lari.bloggerhub.service.media;

import com.cloudinary.Cloudinary;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.response.DataResponse;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * General-purpose image uploads for post banners and inline markdown images.
 *
 * <p>Separate from {@code BlogUserService.uploadProfilePicture} rather than reusing it, because that
 * method is built to be destructive: it pins every upload to the fixed public ID {@code
 * profile-pic-image/{userId}} with {@code overwrite: true}, so a user uploading a second image would
 * silently replace their own avatar — and every earlier post image along with it. Post images need
 * the opposite guarantee, so each upload here gets a fresh UUID public ID and never overwrites.
 */
@Service
public class MediaService {

  private static final Logger log = LoggerFactory.getLogger(MediaService.class);

  /**
   * Cloudinary will happily store a PDF or a video and hand back a URL that an {@code <img>} tag
   * cannot render. Checking here keeps a bad upload a 400 the editor can explain, rather than a
   * broken image the author only notices after publishing.
   */
  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/avif");

  private final Cloudinary cloudinary;

  public MediaService(Cloudinary cloudinary) {
    this.cloudinary = cloudinary;
  }

  /**
   * Uploads an image to Cloudinary under the caller's own folder and returns just the fields a
   * client needs.
   *
   * <p>Note this returns a curated map, not Cloudinary's raw upload result the way the profile
   * picture endpoint does — that response leaks provider internals (signature, etag, API version)
   * into the public API surface and ties the frontend's types to a third party's payload.
   *
   * @param file the image to upload
   * @param authentication the current user, used to namespace the asset
   * @return a response entity containing {@code secure_url}, {@code public_id}, and dimensions
   */
  public ResponseEntity<Response> uploadImage(MultipartFile file, Authentication authentication) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(
              new ErrorResponse(
                  false, HttpStatus.BAD_REQUEST.value(), "No file was uploaded.", null));
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
      return ResponseEntity.badRequest()
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.BAD_REQUEST.value(),
                  "Unsupported file type. Allowed types: JPEG, PNG, GIF, WebP, AVIF.",
                  contentType));
    }

    BlogUser currentUser = (BlogUser) authentication.getPrincipal();
    String publicId =
        String.format("blogger_hub/%s/posts/%s", currentUser.getId(), UUID.randomUUID());

    try {
      Map<Object, Object> params = Map.of("public_id", publicId, "overwrite", false);
      Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

      Map<String, Object> data = new HashMap<>();
      data.put("secure_url", uploadResult.get("secure_url"));
      data.put("public_id", uploadResult.get("public_id"));
      data.put("width", uploadResult.get("width"));
      data.put("height", uploadResult.get("height"));

      return ResponseEntity.ok(
          new DataResponse(true, HttpStatus.OK.value(), "Image uploaded successfully.", data));
    } catch (Exception e) {
      log.error("Failed to upload image for user {}", currentUser.getId(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.INTERNAL_SERVER_ERROR.value(),
                  "Failed to upload image.",
                  e.getMessage()));
    }
  }
}
