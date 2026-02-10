# Notification System Documentation

## Overview

The Blogger Hub application now includes a comprehensive notification system that uses **Apache Kafka** for asynchronous event processing. Users receive notifications for various interactions including likes, comments, replies, and new followers. Additionally, a **user blocking** feature has been implemented to allow users to prevent interactions with specific users.

## Architecture

### Components

1. **Kafka Producer** (`KafkaNotificationProducer`) - Sends notification events to Kafka topics
2. **Kafka Consumer** (`KafkaNotificationConsumer`) - Listens to Kafka topics and processes notification events
3. **Notification Service** (`NotificationService`) - Business logic for managing notifications
4. **Block Service** (`BlockService`) - Manages user blocking relationships
5. **REST Controllers** - Expose APIs for notifications and blocking

### Event Flow

```
User Action (Like/Comment)
    → Service Layer triggers notification
    → KafkaNotificationProducer sends event to Kafka topic
    → KafkaNotificationConsumer receives event
    → NotificationService creates notification in database
    → User can fetch notifications via REST API
```

## Notification Types

The system supports the following notification types:

- `NEW_FOLLOWER` - When someone follows you
- `POST_LIKED` - When someone likes your post
- `POST_COMMENTED` - When someone comments on your post
- `COMMENT_REPLIED` - When someone replies to your comment
- `COMMENT_LIKED` - When someone likes your comment
- `MENTION_IN_POST` - When someone mentions you in a post
- `MENTION_IN_COMMENT` - When someone mentions you in a comment

## API Endpoints

### Notification APIs

#### 1. Get Notifications
```http
GET /api/notifications?page=0&size=20&unreadOnly=false
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Notifications fetched successfully",
  "data": {
    "notifications": [
      {
        "id": "notification123",
        "type": "POST_LIKED",
        "actor": {
          "id": "user123",
          "username": "johndoe",
          "profilePicture": "..."
        },
        "target": {
          "id": "post123",
          "type": "post",
          "title": "Getting Started with Spring Boot"
        },
        "message": "johndoe liked your post \"Getting Started with Spring Boot\"",
        "preview": null,
        "isRead": false,
        "createdAt": "2025-01-15T10:30:00Z",
        "readAt": null
      }
    ],
    "unreadCount": 5,
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  }
}
```

#### 2. Get Unread Count
```http
GET /api/notifications/unread-count
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Unread count fetched successfully",
  "data": {
    "count": 5
  }
}
```

#### 3. Mark Notification as Read
```http
PATCH /api/notifications/{notificationId}/read
Authorization: Bearer {token}
```

#### 4. Mark All Notifications as Read
```http
PATCH /api/notifications/read-all
Authorization: Bearer {token}
```

#### 5. Delete Notification
```http
DELETE /api/notifications/{notificationId}
Authorization: Bearer {token}
```

### User Blocking APIs

#### 1. Block a User
```http
POST /api/block
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": "user123",
  "reason": "Harassment"
}
```

#### 2. Unblock a User
```http
DELETE /api/block/{userId}
Authorization: Bearer {token}
```

#### 3. Get Blocked Users
```http
GET /api/block?page=0&size=20
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Blocked users fetched successfully",
  "data": {
    "blockedUsers": [
      {
        "blockId": "block123",
        "blockedAt": "2025-01-15T10:30:00Z",
        "reason": "Harassment",
        "userId": "user123",
        "username": "johndoe",
        "profilePicture": "...",
        "bio": "..."
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 5,
      "totalPages": 1
    }
  }
}
```

#### 4. Check if User is Blocked
```http
GET /api/block/check/{userId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Block status fetched successfully",
  "data": {
    "isBlocked": true
  }
}
```

## Blocking Effects

When a user blocks another user:

1. **Notifications** - No notifications are created between blocked users
2. **Interactions** - Blocked users cannot:
   - See each other's posts (future implementation)
   - Comment on each other's posts (future implementation)
   - Follow each other (future implementation)
   - Send notifications to each other (implemented)

## Kafka Configuration

### Environment Variables

Add the following environment variables:

```bash
# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP_ID=blogger-hub-notifications
KAFKA_TOPIC_NOTIFICATIONS=blogger-hub-notifications
```

### Local Development with Docker

Run Kafka locally using Docker:

```bash
# Start Zookeeper
docker run -d --name zookeeper -p 2181:2181 confluentinc/cp-zookeeper:latest

# Start Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=host.docker.internal:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest
```

Or use Docker Compose:

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

## Database Schema

### Notification Collection

```javascript
{
  "_id": "notification123",
  "user_id": "user123",        // Recipient
  "type": "POST_LIKED",
  "actor_id": "user456",        // Who triggered the action
  "actor_username": "johndoe",
  "actor_profile_picture": "...",
  "target_id": "post123",       // Related entity ID
  "target_type": "post",        // "post", "comment", "user"
  "target_title": "Post Title",
  "content": "Comment preview...",
  "message": "johndoe liked your post",
  "is_read": false,
  "created_at": "2025-01-15T10:30:00Z",
  "read_at": null
}
```

**Indexes:**
- Compound index on `(user_id, is_read, created_at)`
- Index on `user_id`
- Index on `is_read`

### BlockedUser Collection

```javascript
{
  "_id": "block123",
  "blocker_id": "user123",     // User who initiated the block
  "blocked_id": "user456",     // User who is blocked
  "reason": "Harassment",
  "blocked_at": "2025-01-15T10:30:00Z"
}
```

**Indexes:**
- Compound unique index on `(blocker_id, blocked_id)`
- Index on `blocker_id`
- Index on `blocked_id`

## Integration in Existing Services

Notification triggers have been integrated into:

1. **LikesService** - Sends `POST_LIKED` notification when a post is liked
2. **CommentsService** - Sends:
   - `POST_COMMENTED` when commenting on a post
   - `COMMENT_REPLIED` when replying to a comment

### Example Integration

```java
@Service
public class LikesService {

  private final KafkaNotificationProducer notificationProducer;

  public ResponseEntity<Response> addLike(String blogPostId, Authentication authentication) {
    // ... save like logic ...

    // Send notification
    BlogPost post = blogPostRepository.findById(blogPostId).orElse(null);
    if (post != null && post.getCreatedBy() != null) {
      notificationProducer.sendNotificationEvent(
          post.getCreatedBy().getId(),     // Recipient
          NotificationType.POST_LIKED,
          currentUserId,                    // Actor
          blogPostId,                       // Target ID
          "post"                            // Target type
      );
    }

    return ResponseEntity.ok(...);
  }
}
```

## Features

### ✅ Implemented
- Kafka-based asynchronous notification processing
- Real-time notification creation and delivery
- Read/unread status tracking
- Pagination support for notifications
- User blocking system with reason tracking
- Block status checking
- Automatic notification prevention for blocked users
- Notification triggers for likes and comments

### 🚧 Future Enhancements
- WebSocket integration for real-time push notifications
- Email notifications for important events
- In-app notification badge with unread count
- Notification preferences (allow users to customize which notifications they receive)
- Grouped notifications ("X, Y, and 5 others liked your post")
- Notification history archiving
- Push notifications for mobile apps
- Follow notifications
- Mention parsing and notifications

## Testing

### Manual Testing

1. **Start Kafka** (using Docker or local installation)
2. **Start the application**
3. **Test notification flow**:
   - User A likes User B's post
   - Check User B's notifications: `GET /api/notifications`
   - Verify notification appears with correct details
4. **Test blocking**:
   - User A blocks User B
   - User B likes User A's post
   - Verify no notification is created

### Kafka Topics

Monitor Kafka topics:

```bash
# List topics
kafka-topics.sh --list --bootstrap-server localhost:9092

# Consume messages from notifications topic
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic blogger-hub-notifications --from-beginning
```

## Error Handling

The system includes comprehensive error handling:

1. **Kafka Producer Failures** - Logged with error details
2. **Consumer Processing Errors** - Caught and logged (consider implementing DLQ for production)
3. **Database Errors** - Standard Spring Data error handling
4. **Authorization Errors** - HTTP 403 for unauthorized access
5. **Not Found Errors** - HTTP 404 for missing resources

## Performance Considerations

1. **Asynchronous Processing** - Kafka ensures notifications don't block main operations
2. **Database Indexes** - Optimized queries for fetching notifications
3. **Pagination** - Prevents loading excessive data
4. **Caching** - Consider caching unread counts in Redis for high-traffic scenarios
5. **Batch Processing** - Consumer can be scaled horizontally for high volumes

## Security

1. **Authorization** - All endpoints require authentication via JWT
2. **User Validation** - Users can only access their own notifications
3. **Block Privacy** - Users cannot see who has blocked them
4. **Input Validation** - All request DTOs are validated

## Monitoring

Monitor the following metrics:

1. **Kafka Metrics**:
   - Consumer lag
   - Message processing rate
   - Error rate
2. **Application Metrics**:
   - Notification creation rate
   - Unread notification count per user
   - Block/unblock operations
3. **Database Metrics**:
   - Notification collection size
   - Query performance

## Conclusion

The notification system is now fully integrated and ready for production use. It provides a scalable, asynchronous solution for user notifications with support for user blocking and privacy controls.
