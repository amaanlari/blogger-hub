package com.lari.bloggerhub.controller.media;

import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.service.media.MediaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Image uploads for post banners and inline markdown images. */
@RestController
@RequestMapping("/api/media")
public class MediaController {

  private final MediaService mediaService;

  public MediaController(MediaService mediaService) {
    this.mediaService = mediaService;
  }

  /**
   * Uploads one image and returns its hosted URL.
   *
   * <p>{@code multipart/form-data}, part name {@code file}. Requires a bearer token — the caller's
   * ID namespaces the stored asset. Note the explicit {@code name = "file"}: unlike the profile
   * picture endpoint, this does not rely on the compiler retaining parameter names.
   */
  @PostMapping("/upload")
  public ResponseEntity<Response> uploadImage(
      @RequestParam(name = "file") MultipartFile file, Authentication authentication) {
    return mediaService.uploadImage(file, authentication);
  }
}
