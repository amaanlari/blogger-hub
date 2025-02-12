package com.lari.bloggerhub.service.bloguser;

import com.lari.bloggerhub.constant.Constant;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.service.RedisService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);
  private final JavaMailSender mailSender;
  private final RedisService redisService;
  private final OtpService otpService;
  private final BlogUserRepository blogUserRepository;

  @Value("${otp.ttl}")
  private long otpTtl;

  public EmailService(
      JavaMailSender mailSender,
      RedisService redisService,
      OtpService otpService,
      BlogUserRepository blogUserRepository) {
    this.mailSender = mailSender;
    this.redisService = redisService;
    this.otpService = otpService;
    this.blogUserRepository = blogUserRepository;
  }

  // Send OTP to the user's email address
  @Async
  public void sendVerificationEmail(String email) {
    // Generate OTP

    String otp = otpService.generateAndStoreOtp(email, otpTtl);
    redisService.set(email, otp, otpTtl);

    try {
      String sender = Constant.MAIL_SENDER;
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);
      helper.setFrom(sender);
      helper.setTo(email);
      helper.setSubject("OTP for Blogger Hub account verification");

      // Create the HTML content
      String htmlContent = Constant.EMAIL_CONTENT_OTP.formatted(otp);

      helper.setText(htmlContent, true);
      mailSender.send(message);
      log.info("OTP verification email sent");
    } catch (Exception e) {
      log.error("Failed to send OTP to {}", email, e);
    }
  }

  public boolean verifyEmail(String email, String otp) {
    if (otpService.verifyOtp(email, otp)) {
      BlogUser user =
          blogUserRepository
              .findByEmail(email)
              .orElseThrow(() -> new UsernameNotFoundException("User not found by email"));
      user.setVerified(true);
      blogUserRepository.save(user);
      return true;
    }
    return false;
  }

  public boolean verifyEmail(BlogUser currentUser, String newEmail, String otp) {
    if (otpService.verifyOtp(newEmail, otp)) {
      currentUser.setEmail(newEmail);
      currentUser.setVerified(true);
      blogUserRepository.save(currentUser);
      return true;
    }
    return false;
  }
}
