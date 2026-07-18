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

### Run everything with Docker

To build and run both MySQL and the API in containers:

```bash
cp .env.example .env   # fill in JWT_SECRET, MAIL_*, ADMIN_PASSWORD
docker compose up --build -d
```

The API becomes available on `http://localhost:8080` once the containers are healthy.

### Run tests

```bash
cd cloud-storage-system
mvn test
```

Tests use an in-memory H2 database — no MySQL required.

## API Documentation

Interactive OpenAPI/Swagger documentation is available once the app is running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Use the **Authorize** button in Swagger UI to attach a JWT (`Bearer <accessToken>`) obtained from the login endpoint.

## API Reference

All responses are wrapped in a standard envelope (`status`, `message`, `path`, `data`, `timestamp`). Protected routes require an `Authorization: Bearer <accessToken>` header.

### Authentication & Account

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| POST | `/api/v1/users/register` | – | Register a new user |
| POST | `/api/v1/auth/login` | – | Log in with username/email/phone |
| POST | `/api/v1/auth/refresh-token` | – | Rotate access/refresh tokens |
| POST | `/api/v1/auth/logout` | ✔ | Revoke tokens |
| GET | `/api/v1/auth/login-history` | ✔ | View login history |
| GET | `/api/v1/auth/verify-email` | – | Verify email via token |
| POST | `/api/v1/auth/resend-verification-email` | – | Resend verification email |
| POST | `/api/v1/auth/forgot-password/email` | – | Send password reset OTP |
| POST | `/api/v1/auth/resend-password-otp` | – | Resend reset OTP |
| POST | `/api/v1/auth/verify-password-otp` | – | Verify reset OTP |
| POST | `/api/v1/auth/reset-password` | – | Reset password via OTP |
| POST | `/api/v1/auth/change-password` | ✔ | Change password |
| GET | `/api/v1/users/me/storage` | ✔ | Storage usage vs quota |

### Files & Folders

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| POST | `/api/v1/files/upload` | ✔ | Upload a file (multipart) |
| GET | `/api/v1/files` | ✔ | List files (paginated) |
| GET | `/api/v1/files/{id}` | ✔ | File metadata |
| GET | `/api/v1/files/{id}/download` | ✔ | Download a file |
| PATCH | `/api/v1/files/{id}/move` | ✔ | Move file into a folder |
| DELETE | `/api/v1/files/{id}` | ✔ | Delete a file |
| POST | `/api/v1/folders` | ✔ | Create a folder |
| GET | `/api/v1/folders` | ✔ | List root folders |
| GET | `/api/v1/folders/{id}` | ✔ | Folder contents + breadcrumbs |
| PUT | `/api/v1/folders/{id}/rename` | ✔ | Rename a folder |
| DELETE | `/api/v1/folders/{id}` | ✔ | Delete an empty folder |

### Sharing

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| POST | `/api/v1/files/{id}/share` | ✔ | Create a share link |
| GET | `/api/v1/files/{id}/shares` | ✔ | List active shares |
| GET | `/api/v1/share/{token}` | – | Public download via share link |
| DELETE | `/api/v1/share/{token}` | ✔ | Revoke a share link |

### Admin (ADMIN role)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/admin/stats` | System statistics |
| GET | `/api/v1/admin/users` | List all users |
| GET | `/api/v1/admin/users/{id}` | Get user by id |
| PUT | `/api/v1/admin/users/{id}/role` | Change user role |
| PATCH | `/api/v1/admin/users/{id}/lock` | Lock user |
| POST | `/api/v1/admin/users/{id}/unlock` | Unlock user |
| PATCH | `/api/v1/admin/users/{id}/enable` | Enable user |
| PATCH | `/api/v1/admin/users/{id}/disable` | Disable user |
| PATCH | `/api/v1/admin/users/{id}/quota` | Update storage quota |
| DELETE | `/api/v1/admin/users/{id}` | Soft-delete user |

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
