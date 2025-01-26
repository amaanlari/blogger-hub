# Blogger Hub - Backend

Blogger Hub is a comprehensive blogging platform that enables users to create, manage, and share blogs seamlessly. This repository contains the backend API implementation built using **Spring Boot** and **MongoDB** with enterprise-level practices.

## Features

- **User Module**
    - User registration and login (JWT-based authentication)
    - User profile management
    - Follow and unfollow users

- **Blog Module**
    - Create, update, delete, and view blogs
    - Like and comment on blogs
    - Support for threaded replies in comments

- **Notification Module**
    - Real-time notifications using **Apache Kafka**
    - Notifications for likes, comments, and follows

- **Media Management**
    - Store profile pictures and blog images using **Cloudinary**

- **Database**
    - MongoDB for managing users, blogs, and interactions

## Tech Stack

- **Backend Framework**: Spring Boot
- **Database**: MongoDB (with migration plans to DynamoDB)
- **Authentication**: JWT
- **Messaging**: Apache Kafka
- **File Storage**: Cloudinary
- **Build Tool**: Maven

## Project Structure

```
blogger-hub/
├── src/
│   ├── main/
│   │   ├── java/com/bloggerhub/
│   │   │   ├── config/          # Configuration files
│   │   │   ├── controllers/     # REST controllers
│   │   │   ├── models/          # Entity classes
│   │   │   ├── repositories/    # Database repositories
│   │   │   ├── services/        # Business logic
│   │   │   └── utils/           # Utility classes
│   └── test/
│       └── java/com/bloggerhub/ # Unit and integration tests
├── pom.xml                      # Maven configuration
└── README.md                    # Project documentation
```

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/blogger-hub.git
   cd blogger-hub
   ```

2. Update the `application.yml` file with your MongoDB and Cloudinary credentials.

3. Build the project:
   ```bash
   mvn clean install
   ```

4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## API Documentation

The API documentation is available via Swagger at:
```
http://localhost:8080/swagger-ui/index.html
```

## Development Setup

### Prerequisites

- Java 17 or higher
- MongoDB installed locally or accessible via cloud
- Kafka broker setup (local or cloud)

### Configuration

The application uses profiles for different environments. Set the active profile as needed:
```bash
-Dspring.profiles.active=dev
```

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature/fix:
   ```bash
   git checkout -b feature-name
   ```
3. Commit your changes:
   ```bash
   git commit -m "Add new feature"
   ```
4. Push to your branch:
   ```bash
   git push origin feature-name
   ```
5. Create a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact

For queries, reach out to **Amaan Lari**:
- LinkedIn: [amaanlari](https://linkedin.com/in/amaanlari)
- Twitter: [amaanlari_](https://x.com/amaanlari_)