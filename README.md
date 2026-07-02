# clyDrive

**Cloud File Storage System (Mini Google Drive)** — a secure backend API for uploading, organizing, sharing, and managing files in the cloud.

## Problem

Users need a dependable place to store digital assets (documents, images, videos) that is accessible from any client, organized with folders, shareable via links, and protected by strong authentication. clyDrive addresses this by providing a REST API backed by Java, Spring Boot, MySQL, and JWT-based security.

## Tech Stack

- Java 21
- Spring Boot 3.4 · Spring Security · Spring Data JPA
- MySQL 8
- JWT (access + refresh tokens)
- Local file storage (abstracted for future S3 support)

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- Docker (for MySQL)

### 1. Start MySQL

From the repository root:

```bash
docker compose up -d
```

This creates the `cloud_storage_system` database on `localhost:3306` (user: `root`, password: `root`).

### 2. Set environment variables

Copy the example file and fill in your values:

```bash
cd cloud-storage-system
cp .env.example .env
```

Export the variables before running the app:

```bash
export JWT_SECRET="your-256-bit-secret-key-change-in-production"
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-password"
export ADMIN_PASSWORD="your-admin-password"
```

| Variable | Required | Description |
|----------|:--------:|-------------|
| `JWT_SECRET` | Yes | Secret key for signing JWTs (min 32 characters) |
| `MAIL_USERNAME` | Yes | SMTP username for verification/OTP emails |
| `MAIL_PASSWORD` | Yes | SMTP password or app password |
| `ADMIN_USERNAME` | No | Default admin username (default: `admin`) |
| `ADMIN_EMAIL` | No | Default admin email (default: `admin@clydrive.local`) |
| `ADMIN_PASSWORD` | Yes | Password for the seeded admin account |

### 3. Run the application

```bash
cd cloud-storage-system
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

### 4. Verify health

```bash
curl http://localhost:8080/api/v1/health
```

Expected response:

```json
{
  "status": 200,
  "message": "Service is healthy",
  "path": "/api/v1/health",
  "data": {
    "status": "UP",
    "application": "clydrive",
    "version": "0.0.1-SNAPSHOT"
  }
}
```

### Run tests

```bash
cd cloud-storage-system
mvn test
```

Tests use an in-memory H2 database — no MySQL required.

## Project Structure

```
clyDrive/
├── docs/                    # Requirements and architecture docs
├── docker-compose.yml       # MySQL for local development
└── cloud-storage-system/    # Spring Boot application
    ├── src/main/java/com/clydrive/
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   ├── module/
    │   ├── dtos/
    │   ├── security/
    │   ├── config/
    │   └── ...
    └── src/main/resources/
        ├── application.properties
        └── logback-spring.xml
```

## Documentation

### Requirements

- [Problem Statement](docs/01-problem-statement.md)
- [Core Features](docs/02-core-features.md)
- [Non-Functional Requirements](docs/03-non-functional-requirements.md)
- [User Roles & Scope](docs/04-user-roles-and-scope.md)
- [Success Criteria](docs/05-success-criteria.md)

### Architecture

- [System Architecture](docs/06-architecture.md)
- [API — Auth & Users](docs/07-api-auth-and-users.md)
- [API — Files, Folders & Sharing](docs/08-api-files-folders-sharing.md)
- [API — Admin](docs/09-api-admin.md)
- [Database Design](docs/10-database-design.md)
- [File Storage Strategy](docs/11-file-storage-strategy.md)
- [Security Design](docs/12-security-design.md)
- [API Response & Error Handling](docs/13-api-response-and-errors.md)
