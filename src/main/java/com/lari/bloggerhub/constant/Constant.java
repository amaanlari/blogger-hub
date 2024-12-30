package com.lari.bloggerhub.constant;

/** This class contains constant values used throughout the Blogger Hub application. */
public class Constant {

  /** Private constructor to prevent instantiation of this class. */
  private Constant() {}

  // API Endpoints
  // BlogUserController
  public static final String DEFAULT_PROFILE_IMAGE_URL =
      "https://res.cloudinary.com/lp-blogger-hub-dev/image/upload/v1734661609/blogger_hub/default-profile-pic.jpg";

  // Message Constants
  public static final String NOT_AUTHORIZED_TO_ACCESS_PROFILE =
      "You are not authorized to access this user's profile";
  public static final String LOGGED_IN_USER_ID_DOES_NOT_MATCH_REQUESTED_USER_ID =
      "Logged in user's id does not match the requested user's id";

  // Redis Keys
  public static final String OTP_KEY_PREFIX = "OTP_%s";
}
