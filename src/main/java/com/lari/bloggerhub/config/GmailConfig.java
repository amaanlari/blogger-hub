package com.lari.bloggerhub.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.apache.v2.GoogleApacheHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@Configuration
public class GmailConfig {

  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final List<String> SCOPES =
      List.of(GmailScopes.GMAIL_SEND, GmailScopes.GMAIL_COMPOSE);
  private static final Logger log = LoggerFactory.getLogger(GmailConfig.class);

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${gmail.credentials-file-path}")
  private String credentialsFilePath;

  @Value("${gmail.tokens-directory-path}")
  private String tokensDirectoryPath;

  @Value("${gmail.redirect-port}")
  private int redirectPort;

  @Bean
  public Gmail gmailService() throws Exception {
    ApacheHttpTransport httpTransport = GoogleApacheHttpTransport.newTrustedTransport();

    InputStream in = new FileSystemResource(credentialsFilePath).getInputStream();

    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDirectoryPath)))
            .setAccessType("offline")
            .build();

    Credential credential =
        new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver.Builder().setPort(redirectPort).build())
            .authorize("user");

    return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(applicationName)
        .build();
  }
}
