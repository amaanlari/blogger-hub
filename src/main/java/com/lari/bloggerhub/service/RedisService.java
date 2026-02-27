package com.lari.bloggerhub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

  private static final Logger log = LoggerFactory.getLogger(RedisService.class);
  private final RedisTemplate<String, String> redisTemplate;

  public RedisService(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void set(String key, String value, long ttl) {
    redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.MINUTES);
    if (log.isDebugEnabled()) {
      log.debug("stored value: {}", redisTemplate.opsForValue().get(key));
    }
  }

  public String get(String key) {
    return redisTemplate.opsForValue().get(key);
  }
}
