package com.lari.bloggerhub.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisServiceTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // This test is used to check if the RedisTemplate bean is created successfully
    @Test
    void contextLoads() {
        assertNotNull(redisTemplate);
    }

    @Test
    void save_whenKeyAndValueAreValid_savesKeyAndValue() {
        String key = "testKey";
        String value = "testValue";

        redisTemplate.opsForValue().set(key, value, 30);

        String savedValue = redisTemplate.opsForValue().get(key);

        assertNotNull(savedValue);
    }
}
