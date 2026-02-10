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

  public static final String EMAIL_CONTENT_NEW_FOLLOWER =
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
            .content {
              font-size: 16px;
              color: #333;
              margin: 20px 0;
            }
            .highlight {
              font-weight: bold;
              color: #ff5722;
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
            <div class="header">New Follower!</div>
            <div class="content">
                <p>Hello <span class="highlight">%s</span>,</p>
                <p><span class="highlight">%s</span> started following you on Blogger Hub!</p>
                <p>Check out their profile and see what they're writing about.</p>
            </div>
            <div class="footer">Thank you, <br> The Blogger Hub Team</div>
        </div>
    </body>
    </html>
    """;

  public static final String EMAIL_CONTENT_POST_LIKED =
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
            .content {
              font-size: 16px;
              color: #333;
              margin: 20px 0;
            }
            .highlight {
              font-weight: bold;
              color: #ff5722;
            }
            .post-title {
              font-style: italic;
              color: #555;
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
            <div class="header">Someone liked your post!</div>
            <div class="content">
                <p>Hello <span class="highlight">%s</span>,</p>
                <p><span class="highlight">%s</span> liked your post <span class="post-title">"%s"</span>.</p>
                <p>Keep creating great content!</p>
            </div>
            <div class="footer">Thank you, <br> The Blogger Hub Team</div>
        </div>
    </body>
    </html>
    """;

  public static final String EMAIL_CONTENT_POST_COMMENTED =
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
            .content {
              font-size: 16px;
              color: #333;
              margin: 20px 0;
            }
            .highlight {
              font-weight: bold;
              color: #ff5722;
            }
            .post-title {
              font-style: italic;
              color: #555;
            }
            .comment {
              background-color: #f5f5f5;
              padding: 10px;
              border-left: 3px solid #ff5722;
              margin: 10px 0;
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
            <div class="header">New Comment on Your Post!</div>
            <div class="content">
                <p>Hello <span class="highlight">%s</span>,</p>
                <p><span class="highlight">%s</span> commented on your post <span class="post-title">"%s"</span>:</p>
                <div class="comment">%s</div>
                <p>Reply to keep the conversation going!</p>
            </div>
            <div class="footer">Thank you, <br> The Blogger Hub Team</div>
        </div>
    </body>
    </html>
    """;

  public static final String EMAIL_CONTENT_COMMENT_REPLIED =
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
            .content {
              font-size: 16px;
              color: #333;
              margin: 20px 0;
            }
            .highlight {
              font-weight: bold;
              color: #ff5722;
            }
            .reply {
              background-color: #f5f5f5;
              padding: 10px;
              border-left: 3px solid #ff5722;
              margin: 10px 0;
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
            <div class="header">New Reply to Your Comment!</div>
            <div class="content">
                <p>Hello <span class="highlight">%s</span>,</p>
                <p><span class="highlight">%s</span> replied to your comment:</p>
                <div class="reply">%s</div>
                <p>Continue the conversation!</p>
            </div>
            <div class="footer">Thank you, <br> The Blogger Hub Team</div>
        </div>
    </body>
    </html>
    """;

  public static final String EMAIL_CONTENT_COMMENT_LIKED =
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
            .content {
              font-size: 16px;
              color: #333;
              margin: 20px 0;
            }
            .highlight {
              font-weight: bold;
              color: #ff5722;
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
            <div class="header">Someone liked your comment!</div>
            <div class="content">
                <p>Hello <span class="highlight">%s</span>,</p>
                <p><span class="highlight">%s</span> liked your comment.</p>
                <p>Your insights are appreciated!</p>
            </div>
            <div class="footer">Thank you, <br> The Blogger Hub Team</div>
        </div>
    </body>
    </html>
    """;

  public static final String EMAIL_CONTENT_MENTION =
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
            .content {
              font-size: 16px;
              color: #333;
              margin: 20px 0;
            }
            .highlight {
              font-weight: bold;
              color: #ff5722;
            }
            .mention {
              background-color: #f5f5f5;
              padding: 10px;
              border-left: 3px solid #ff5722;
              margin: 10px 0;
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
            <div class="header">You were mentioned!</div>
            <div class="content">
                <p>Hello <span class="highlight">%s</span>,</p>
                <p><span class="highlight">%s</span> mentioned you in a %s:</p>
                <div class="mention">%s</div>
                <p>Check it out and join the discussion!</p>
            </div>
            <div class="footer">Thank you, <br> The Blogger Hub Team</div>
        </div>
    </body>
    </html>
    """;
}
