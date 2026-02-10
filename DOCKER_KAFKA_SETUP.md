# Docker Compose with Kafka KRaft Setup

## Overview

The docker-compose.yaml has been updated to include Apache Kafka running in **KRaft mode** (without Zookeeper). This provides a modern, simplified Kafka deployment for the notification system.

## What's Included

### Services

1. **MongoDB** - NoSQL database (port: 27018)
2. **Redis** - Caching and session storage (port: 6380)
3. **Kafka** - Event streaming platform with KRaft (ports: 9092, 9093)
4. **Blogger-Hub** - Main Spring Boot application (port: 8080)

### Key Features

- ✅ **KRaft Mode**: No Zookeeper dependency
- ✅ **Official Apache Kafka Image**: `apache/kafka:latest`
- ✅ **Health Checks**: Automatic health monitoring
- ✅ **Topic Auto-Creation**: Disabled for production-like setup
- ✅ **Persistent Storage**: Volume for Kafka data
- ✅ **Network Isolation**: Custom bridge network

## Kafka Configuration

### KRaft Settings

```yaml
KAFKA_NODE_ID: 1
KAFKA_PROCESS_ROLES: broker,controller
KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'
```

### Listeners

- **Client Port**: 9092 (PLAINTEXT)
- **Controller Port**: 9093 (CONTROLLER)
- **Advertised Listener**: localhost:9092

### Replication Factor

Set to 1 (single node) for development:
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1`
- `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1`

## Starting the Services

### Start All Services

```bash
docker compose up -d
```

### Start Individual Services

```bash
# Start only Kafka
docker compose up -d kafka

# Start MongoDB and Redis
docker compose up -d mongodb redis

# Start the application
docker compose up -d blogger-hub
```

## Topic Management

### Created Topics

**Topic Name**: `blogger-hub-notifications`
- **Partitions**: 3
- **Replication Factor**: 1
- **Purpose**: Notification events

### Create Topic Manually

```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --if-not-exists \
  --topic blogger-hub-notifications \
  --partitions 3 \
  --replication-factor 1
```

### List All Topics

```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

### Describe a Topic

```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic blogger-hub-notifications
```

### Delete a Topic

```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --delete \
  --topic blogger-hub-notifications
```

## Testing Kafka

### Send a Test Message

```bash
echo '{"userId":"test123","type":"POST_LIKED","actorId":"actor456"}' | \
docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic blogger-hub-notifications
```

### Consume Messages

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic blogger-hub-notifications \
  --from-beginning
```

## Monitoring

### Check Service Status

```bash
docker compose ps
```

Example output:
```
NAME      SERVICE   STATUS          PORTS
kafka     kafka     Up (healthy)    0.0.0.0:9092-9093->9092-9093/tcp
mongodb   mongodb   Up              0.0.0.0:27018->27017/tcp
redis     redis     Up              0.0.0.0:6380->6379/tcp
```

### View Kafka Logs

```bash
docker logs kafka -f
```

### Check Kafka Health

```bash
docker exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9092
```

## Port Configuration

| Service | Internal Port | External Port |
|---------|---------------|---------------|
| MongoDB | 27017 | 27018 |
| Redis | 6379 | 6380 |
| Kafka (Client) | 9092 | 9092 |
| Kafka (Controller) | 9093 | 9093 |
| Blogger-Hub | 8080 | 8080 |

**Note**: External ports for MongoDB and Redis are different to avoid conflicts with locally running instances.

## Environment Variables

The application requires these environment variables:

```bash
# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:9092  # Inside Docker network
# or
KAFKA_BOOTSTRAP_SERVERS=localhost:9092  # From host machine

KAFKA_CONSUMER_GROUP_ID=blogger-hub-notifications
KAFKA_TOPIC_NOTIFICATIONS=blogger-hub-notifications
```

## Volume Management

### List Volumes

```bash
docker volume ls | grep blogger-hub
```

### Inspect Kafka Data Volume

```bash
docker volume inspect blogger-hub_kafka-data
```

### Remove All Volumes (⚠️ Data Loss)

```bash
docker compose down -v
```

## Troubleshooting

### Kafka Won't Start

1. **Check logs**:
   ```bash
   docker logs kafka
   ```

2. **Verify healthcheck**:
   ```bash
   docker inspect kafka | grep -A 10 Health
   ```

3. **Remove volumes and restart**:
   ```bash
   docker compose down -v
   docker compose up -d kafka
   ```

### Cannot Connect to Kafka

1. **From host machine**: Use `localhost:9092`
2. **From Docker containers**: Use `kafka:9092`
3. **Check if port is accessible**:
   ```bash
   telnet localhost 9092
   ```

### Topic Not Found

```bash
# List all topics
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list

# Recreate topic if needed
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic blogger-hub-notifications \
  --partitions 3 \
  --replication-factor 1
```

### Permission Issues

If you see permission errors in Kafka logs:

```bash
# Stop services
docker compose down

# Remove volumes
docker volume rm blogger-hub_kafka-data

# Restart
docker compose up -d kafka
```

## Production Considerations

For production deployment, consider:

1. **Multiple Kafka Nodes**: Increase replication factor to 3
2. **Separate Controller Nodes**: Dedicated controller quorum
3. **Authentication**: Enable SASL/SSL
4. **Monitoring**: Integrate with Prometheus/Grafana
5. **Resource Limits**: Set memory and CPU limits
6. **Backup Strategy**: Regular snapshots of Kafka data
7. **Log Retention**: Configure appropriate retention policies

### Example Production Settings

```yaml
kafka:
  environment:
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '1'
        memory: 1G
```

## Useful Commands Reference

```bash
# Start services
docker compose up -d

# Stop services
docker compose down

# View logs
docker compose logs -f kafka

# Restart a service
docker compose restart kafka

# Execute command in Kafka container
docker exec -it kafka bash

# Check Kafka version
docker exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9092 | head -1

# Monitor consumer lag
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group blogger-hub-notifications
```

## Integration with Spring Boot

The application automatically connects to Kafka using configuration in `application.yaml`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP_ID:blogger-hub-notifications}
    topic:
      notifications: ${KAFKA_TOPIC_NOTIFICATIONS:blogger-hub-notifications}
```

When running via Docker Compose, the `KAFKA_BOOTSTRAP_SERVERS` environment variable is automatically set to `kafka:9092`.

## Testing the Full Stack

1. **Start all services**:
   ```bash
   docker compose up -d
   ```

2. **Verify all services are running**:
   ```bash
   docker compose ps
   ```

3. **Check application logs**:
   ```bash
   docker compose logs -f blogger-hub
   ```

4. **Test notification flow**:
   - Make a request to like a post
   - Check Kafka for the message
   - Verify notification is created in MongoDB

## Summary

✅ **Kafka with KRaft** - Modern, Zookeeper-less deployment
✅ **Health Checks** - Automatic service monitoring
✅ **Topic Pre-creation** - Production-ready topic setup
✅ **Network Isolation** - Secure inter-service communication
✅ **Persistent Storage** - Data survives container restarts
✅ **Easy Management** - Simple commands for all operations

The notification system is now fully integrated with Kafka and ready for development and testing!
