# clyDrive

**Cloud File Storage System (Mini Google Drive)** — a secure backend API for uploading, organizing, sharing, and managing files in the cloud.

## Problem

Users need a dependable place to store digital assets (documents, images, videos) that is accessible from any client, organized with folders, shareable via links, and protected by strong authentication. clyDrive addresses this by providing a REST API backed by Java, Spring Boot, MySQL, and JWT-based security.

## Tech Stack (planned)

- Java 21
- Spring Boot · Spring Security · Spring Data JPA
- MySQL
- JWT (access + refresh tokens)
- Local file storage (abstracted for future S3 support)

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
