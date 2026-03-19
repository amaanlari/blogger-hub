package com.lari.bloggerhub.config;

import com.google.api.client.googleapis.apache.v2.GoogleApacheHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.lari.bloggerhub.constant.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.io.InputStream;
import java.util.List;

@Configuration
public class GmailConfig {

  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final List<String> SCOPES =
      List.of(GmailScopes.GMAIL_SEND, GmailScopes.GMAIL_COMPOSE);
  private static final Logger log = LoggerFactory.getLogger(GmailConfig.class);

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${gmail.clientId}")
  private String clientId;

  @Value("${gmail.clientSecret}")
  private String clientSecret;

  @Value("${gmail.refreshToken}")
  private String refreshToken;

  @Bean
  public Gmail gmailService() throws Exception {
    ApacheHttpTransport httpTransport = GoogleApacheHttpTransport.newTrustedTransport();

    // Build the credential using the refresh token
    GoogleCredentials credentials = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();

    HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

    return new Gmail.Builder(httpTransport, JSON_FACTORY, requestInitializer)
        .setApplicationName(applicationName)
        .build();
  }
}
