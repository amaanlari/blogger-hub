package com.lari.bloggerhub.service.bloguser;

import com.lari.bloggerhub.constant.Constant;
import com.lari.bloggerhub.service.RedisService;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@SuppressWarnings("LoggingSimilarMessage")
@Service
public class OtpService {

  private static final Logger log = LoggerFactory.getLogger(OtpService.class);
  private final RedisService redisService;

  public OtpService(RedisService redisService) {
    this.redisService = redisService;
  }

  public String generateAndStoreOtp(String email, long otpTtl) {
    String otpKey = getOtpKey(email);
    String otpValue = RandomStringUtils.randomNumeric(6);
    log.debug("otpKey: {}, otpValue: {}", otpKey, otpValue);
    redisService.set(otpKey, otpValue, otpTtl);
    log.debug("Stored generated OTP: {}", otpValue);
    return otpValue;
  }

  public boolean verifyOtp(String email, String otp) {
    log.debug("Received OTP: {}", otp);
    String otpKey = getOtpKey(email);
    String otpValue = redisService.get(otpKey);
    log.debug("otpKey: {}, otpValue: {}", otpKey, otpValue);
    if (otpValue == null) {
      return false;
    }
    return otp.equals(otpValue);
  }

  public String getOtpKey(String email) {
    return String.format(Constant.OTP_KEY_PREFIX, email);
  }
}
