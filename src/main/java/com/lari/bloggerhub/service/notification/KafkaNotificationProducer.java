package com.lari.bloggerhub.service.notification;

import com.lari.bloggerhub.dto.event.NotificationEvent;
import com.lari.bloggerhub.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

/**
 * Service for producing notification events to Kafka.
 *
 * <p>This service sends notification events to a Kafka topic for asynchronous processing by
 * consumers.
 */
@Service
public class KafkaNotificationProducer {

  private static final Logger log = LoggerFactory.getLogger(KafkaNotificationProducer.class);

  @Value("${spring.kafka.topic.notifications}")
  private String notificationsTopic;

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public KafkaNotificationProducer(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * Sends a notification event to Kafka.
   *
   * @param userId the ID of the user who should receive the notification
   * @param type the type of notification
   * @param actorId the ID of the user who triggered the notification
   * @param targetId the ID of the target entity (post, comment, etc.)
   * @param targetType the type of the target entity
   * @param content optional content (e.g., comment text)
   */
  public void sendNotificationEvent(
      String userId,
      NotificationType type,
      String actorId,
      String targetId,
      String targetType,
      String content) {

    // Don't send notification if actor is the recipient
    if (userId.equals(actorId)) {
      log.debug("Skipping notification: actor and recipient are the same user");
      return;
    }

    NotificationEvent event =
        new NotificationEvent(userId, type, actorId, targetId, targetType, content);

    log.info("Sending notification event to Kafka: {}", event);

    CompletableFuture<SendResult<String, Object>> future =
        kafkaTemplate.send(notificationsTopic, userId, event);

    future.whenComplete(
        (result, ex) -> {
          if (ex == null) {
            log.info(
                "Notification event sent successfully: topic={}, partition={}, offset={}",
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          } else {
            log.error("Failed to send notification event to Kafka", ex);
          }
        });
  }

  /**
   * Convenience method for sending notification without content.
   *
   * @param userId the ID of the user who should receive the notification
   * @param type the type of notification
   * @param actorId the ID of the user who triggered the notification
   * @param targetId the ID of the target entity
   * @param targetType the type of the target entity
   */
  public void sendNotificationEvent(
      String userId, NotificationType type, String actorId, String targetId, String targetType) {
    sendNotificationEvent(userId, type, actorId, targetId, targetType, null);
  }
}
