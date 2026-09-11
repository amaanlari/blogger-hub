package com.lari.bloggerhub.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Configuration class for Apache Kafka integration.
 *
 * <p>This class configures Kafka producers and consumers for the notification system. It sets up
 * the necessary beans for sending and receiving notification events asynchronously.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

//  @Value("${aiven.basic.auth.user.info}")
//  private String basicAuthUserInfo;

//  @Value("${spring.kafka.ssl.trust-store-location}")
//  private String caPemLocation;

//  @Value("${spring.kafka.ssl.key-store-location}")
//  private String svcPemLocation;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  /**
   * Configures the Kafka producer factory for notification events.
   *
   * @return ProducerFactory for creating Kafka producers
   */
  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
//    configProps.put("security.protocol", "SSL");
//    configProps.put("ssl.truststore.location", caPemLocation);
//    configProps.put("ssl.keystore.type", "PEM");
//    configProps.put("ssl.truststore.type", "PEM");
//    configProps.put("ssl.keystore.location", svcPemLocation);
//    configProps.put("schema.registry.url", "https://blogger-hub-stefansalvatorepvt-bb2d.d.aivencloud.com:10411");
//    configProps.put("basic.auth.credentials.source", "USER_INFO");
//    configProps.put("basic.auth.user.info","avnadmin:AVNS_j-jLFrzfYXWtLZ59VwQ");

    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * Creates a KafkaTemplate bean for sending messages to Kafka topics.
   *
   * @return KafkaTemplate for producing messages
   */
  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  /**
   * Configures the Kafka consumer factory for notification events.
   *
   * @return ConsumerFactory for creating Kafka consumers
   */
  @Bean
  public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.lari.bloggerhub.dto.event");
    configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
            "com.lari.bloggerhub.dto.event.NotificationEvent");
    configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
//    configProps.put("security.protocol", "SSL");
//    configProps.put("ssl.truststore.location", caPemLocation);
//    configProps.put("ssl.keystore.type", "PEM");
//    configProps.put("ssl.truststore.type", "PEM");
//    configProps.put("ssl.keystore.location", svcPemLocation);
//    configProps.put("schema.registry.url", "https://blogger-hub-stefansalvatorepvt-bb2d.d.aivencloud.com:10411");
//    configProps.put("basic.auth.credentials.source", "USER_INFO");
//    configProps.put("basic.auth.user.info", "avnadmin:AVNS_j-jLFrzfYXWtLZ59VwQ");

    return new DefaultKafkaConsumerFactory<>(configProps);
  }

  /**
   * Creates a Kafka listener container factory for consuming notification events.
   *
   * @return ConcurrentKafkaListenerContainerFactory for concurrent message processing
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setConcurrency(3); // Number of concurrent consumers
    return factory;
  }
}
