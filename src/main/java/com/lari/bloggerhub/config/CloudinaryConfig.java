package com.lari.bloggerhub.config;

import com.cloudinary.Cloudinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);
    @Value("${cloudinary.cloud.name}")
    private String cloudName;

    @Value("${cloudinary.api.key}")
    private String apiKey;

    @Value("${cloudinary.api.secret}")
    private String apiSecret;


    @Bean
    public Cloudinary cloudinary() {
        String cloudinaryUrl = String.format("cloudinary://%s:%s@%s", apiKey, apiSecret, cloudName);
        log.info("Initializing Cloudinary with URL: {}", cloudinaryUrl);
        return new Cloudinary(cloudinaryUrl);
    }
}
