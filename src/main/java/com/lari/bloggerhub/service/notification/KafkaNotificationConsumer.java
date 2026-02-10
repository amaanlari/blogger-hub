package com.lari.bloggerhub.service.notification;

import com.lari.bloggerhub.dto.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Service for consuming notification events from Kafka.
 *
 * <p>This service listens to Kafka topics and processes notification events by creating actual
 * notification records in the database.
 */
@Service
public class KafkaNotificationConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaNotificationConsumer.class);

  private final NotificationService notificationService;

  public KafkaNotificationConsumer(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /**
   * Listens to the notifications topic and processes incoming events.
   *
   * @param event the notification event
   * @param partition the Kafka partition
   * @param offset the message offset
   */
  @KafkaListener(
      topics = "${spring.kafka.topic.notifications}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory")
  public void consumeNotificationEvent(
      @Payload NotificationEvent event,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {

    log.info(
        "Received notification event from Kafka: partition={}, offset={}, event={}",
        partition,
        offset,
        event);

    try {
      // Process the notification event
      notificationService.createNotification(
          event.getUserId(),
          event.getType(),
          event.getActorId(),
          event.getTargetId(),
          event.getTargetType(),
          event.getContent());

      log.info("Successfully processed notification event: {}", event);

    } catch (Exception e) {
      log.error("Error processing notification event: {}", event, e);
      // In production, you might want to send to a dead-letter queue (DLQ)
      // or implement retry logic here
    }
  }
}
