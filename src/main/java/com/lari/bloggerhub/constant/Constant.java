package com.lari.bloggerhub.constant;

import org.springframework.beans.factory.annotation.Value;

/** This class contains constant values used throughout the Blogger Hub application. */
public class Constant {

  public static final String UNAUTHORIZED = "Unauthorized";
  public static final String LOGGED_IN_USER_ID_NOT_MATCH_REQUESTED_USER_ID =
      "Logged in user's id does not match requested user's id";
  public static final String PREMIUM_POST = "This is a premium post";

  /** Private constructor to prevent instantiation of this class. */
  private Constant() {}

  // API Endpoints
  // BlogUserController
  @Value("${cloudinary.cloud.default-profile-pic}")
  public static final String DEFAULT_PROFILE_IMAGE_URL =
      "https://res.cloudinary.com/dkzjg0j2e/image/upload/v1629782927/default-profile-pic.png";

  // Message Constants
  public static final String NOT_AUTHORIZED_TO_ACCESS_PROFILE =
      "You are not authorized to access this user's profile";
  public static final String LOGGED_IN_USER_ID_DOES_NOT_MATCH_REQUESTED_USER_ID =
      "Logged in user's id does not match the requested user's id";
  public static final String POST_NOT_FOUND = "Blog post not found";

  // Redis Keys
  public static final String OTP_KEY_PREFIX = "OTP_%s";

  // Email
  @Value("${spring.mail.username}")
  public static final String MAIL_SENDER = "bloggerhub.lari.com@gmail.com";

  public static final String EMAIL_CONTENT_OTP =
      """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body {
              font-family: Arial, sans-serif;
              line-height: 1.6;
              background-color: #f9f9f9;
              padding: 20px;
            }
            .container {
              max-width: 600px;
              margin: auto;
              padding: 20px;
              border: 1px solid #ddd;
              border-radius: 8px;
              background: #fff;
              color: #000 !important;
            }
            .header {
              font-size: 20px;
              text-align: center;
              margin-bottom: 20px;
              color: #333;
            }
            .otp {
              font-size: 24px;
              color: #ff5722;
              text-align: center;
              margin: 20px 0;
              font-weight: bold;
            }
            .footer {
              font-size: 14px;
              text-align: center;
              color: #555;
              margin-top: 20px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">Your One-Time Password (OTP)</div>
            <div>
            <p>Hello,</p>
            <p>Use the OTP below to verify your email address or reset your password:</p>
            <div class="otp">%s</div>
            <p>If you didn't request this, please ignore this email.</p>
            <div>
            <div class="footer">Thank you, <br> The Blogger Hub Team</div>
        </div>
    </body>
    </html>
    """;
}
