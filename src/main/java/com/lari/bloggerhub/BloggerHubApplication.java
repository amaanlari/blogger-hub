package com.lari.bloggerhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SuppressWarnings("checkstyle:MissingJavadocType")
@EnableAsync
@SpringBootApplication
public class BloggerHubApplication {

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static void main(String[] args) {
    SpringApplication.run(BloggerHubApplication.class, args);
  }
}
