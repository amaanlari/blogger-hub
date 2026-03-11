#!/bin/sh
set -e

KAFKA_CONFIG="/opt/kafka/config/server.properties"

echo "Configuring Kafka..."

echo "node.id=${KAFKA_NODE_ID}" >> $KAFKA_CONFIG
echo "process.roles=${KAFKA_PROCESS_ROLES}" >> $KAFKA_CONFIG
echo "controller.quorum.voters=${KAFKA_CONTROLLER_QUORUM_VOTERS}" >> $KAFKA_CONFIG
echo "controller.listener.names=${KAFKA_CONTROLLER_LISTENER_NAMES}" >> $KAFKA_CONFIG
echo "listeners=${KAFKA_LISTENERS}" >> $KAFKA_CONFIG
echo "advertised.listeners=${KAFKA_ADVERTISED_LISTENERS}" >> $KAFKA_CONFIG
echo "listener.security.protocol.map=${KAFKA_LISTENER_SECURITY_PROTOCOL_MAP}" >> $KAFKA_CONFIG
echo "inter.broker.listener.name=${KAFKA_INTER_BROKER_LISTENER_NAME}" >> $KAFKA_CONFIG
echo "log.dirs=${KAFKA_LOG_DIRS}" >> $KAFKA_CONFIG
echo "offsets.topic.replication.factor=${KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR}" >> $KAFKA_CONFIG
echo "transaction.state.log.replication.factor=${KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR}" >> $KAFKA_CONFIG
echo "transaction.state.log.min.isr=${KAFKA_TRANSACTION_STATE_LOG_MIN_ISR}" >> $KAFKA_CONFIG
echo "auto.create.topics.enable=${KAFKA_AUTO_CREATE_TOPICS_ENABLE}" >> $KAFKA_CONFIG

mkdir -p /var/lib/kafka/data

echo "Formatting Kafka storage..."

/opt/kafka/bin/kafka-storage.sh format \
-t ${CLUSTER_ID} \
-c ${KAFKA_CONFIG} \
--ignore-formatted

echo "Starting Kafka..."

/opt/kafka/bin/kafka-server-start.sh ${KAFKA_CONFIG} &

echo "Waiting for Kafka..."

until /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1
do
  sleep 3
done

echo "Kafka started."

echo "Creating topic..."

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --if-not-exists \
  --topic blogger-hub-notifications \
  --partitions 3 \
  --replication-factor 1

echo "Starting Spring Boot..."

exec java \
-jar app.jar \
--spring.profiles.active=${SPRING_PROFILES_ACTIVE}