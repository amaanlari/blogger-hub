package com.lari.bloggerhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SuppressWarnings("checkstyle:MissingJavadocType")
@EnableAsync
@SpringBootApplication
public class BloggerHubApplication {

  private static final Logger log = LoggerFactory.getLogger(BloggerHubApplication.class);

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static void main(String[] args) {
    System.out.println("MONGODB_DATABASE = [" + System.getenv("MONGODB_DATABASE") + "]");
    log.warn("MONGODB_DATABASE = [" + System.getenv("MONGODB_DATABASE") + "]");

    SpringApplication.run(BloggerHubApplication.class, args);
  }
}
