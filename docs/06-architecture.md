# System Architecture

High-level structure of clyDrive — how components connect and how a request flows through the system.

---

## Overview

clyDrive is a **stateless REST API** backed by MySQL (metadata) and local disk (file bytes). Clients authenticate with JWT and interact with resources through versioned HTTP endpoints.

```mermaid
flowchart TB
    subgraph clients [Clients]
        Web[Web / Mobile / CLI]
        Postman[Postman / curl]
    end

    subgraph app [Spring Boot Application]
        Controller[Controllers]
        Security[JWT Security Filter]
        Service[Services]
        Repo[Repositories]
        Storage[File Storage Layer]
        Scheduler[Cleanup Scheduler]
        Mail[Email Service]
    end

    subgraph data [Data Stores]
        MySQL[(MySQL)]
        Disk[(Local File System)]
    end

    Web --> Controller
    Postman --> Controller
    Controller --> Security
    Security --> Service
    Service --> Repo
    Service --> Storage
    Service --> Mail
    Repo --> MySQL
    Storage --> Disk
    Scheduler --> Repo
```

---

## Layered Architecture

Each layer has a single responsibility. Dependencies flow **downward only**.

| Layer | Package | Responsibility |
|-------|---------|----------------|
| **Controller** | `controller` | HTTP mapping, request validation, response wrapping |
| **Service** | `service` / `service.impl` | Business logic, transactions, orchestration |
| **Repository** | `repository` | JPA data access — no business rules |
| **Entity** | `module` | JPA entities mapped to database tables |
| **DTO** | `dtos.request` / `dtos.response` | API contracts — separate from entities |
| **Security** | `security` | JWT filter, entry point, user details loader |
| **Config** | `config` | Security, async, password encoder, auditor |
| **Exception** | `exception` | Custom exceptions + global handler |
| **Util** | `util` | JWT helpers, shared utilities |
| **Scheduler** | `scheduler` | Cron jobs for token/OTP cleanup |
| **Initializer** | `initializer` | Seed default admin on startup |

```mermaid
flowchart LR
    Client -->|HTTP| Controller
    Controller -->|DTO| Service
    Service -->|Entity| Repository
    Repository -->|SQL| MySQL
    Service -->|bytes| FileStorage
    FileStorage -->|I/O| Disk
```

---

## Component Map

| Component | Purpose |
|-----------|---------|
| `AuthController` | Login, logout, refresh, password flows |
| `UsersController` | Registration |
| `FileController` | Upload, download, list, delete files |
| `FolderController` | Folder CRUD and nesting |
| `ShareController` | Share link create/revoke/public download |
| `AdminController` | User management and system stats |
| `AuthService` | Authentication logic, token lifecycle |
| `UserService` | Registration and profile |
| `FileService` | File metadata + storage orchestration |
| `FolderService` | Folder tree operations |
| `ShareService` | Share token generation and validation |
| `AdminService` | Admin operations |
| `FileStorageService` | Abstract read/write/delete on disk |
| `EmailService` | Async SMTP for verification and OTP |
| `AuditService` | Write audit log entries |
| `TokenBlacklistService` | Invalidate JWTs on logout |
| `CleanupScheduler` | Purge expired tokens, OTPs, shares |
| `JwtAuthFilter` | Intercept requests, validate Bearer token |
| `GlobalExceptionHandler` | Map exceptions to `ApiResponse` errors |

---

## Request Lifecycle

### Authenticated request

```mermaid
sequenceDiagram
    participant C as Client
    participant F as JwtAuthFilter
    participant CT as Controller
    participant S as Service
    participant R as Repository
    participant DB as MySQL

    C->>F: GET /api/v1/files (Bearer token)
    F->>F: Validate JWT, check blacklist
    F->>CT: Forward with SecurityContext
    CT->>S: listFiles(userId)
    S->>R: findByUserId(userId)
    R->>DB: SELECT ...
    DB-->>R: rows
    R-->>S: List FileEntity
    S-->>CT: List FileResponse
    CT-->>C: ApiResponse 200
```

### File upload

```mermaid
sequenceDiagram
    participant C as Client
    participant CT as FileController
    participant S as FileService
    participant FS as FileStorageService
    participant R as Repository
    participant DB as MySQL
    participant D as Disk

    C->>CT: POST /api/v1/files/upload (multipart)
    CT->>S: upload(file, userId)
    S->>S: Check storage quota
    S->>FS: save(bytes, path)
    FS->>D: Write file
    S->>R: save(FileEntity)
    R->>DB: INSERT
    S-->>CT: FileResponse
    CT-->>C: ApiResponse 201
```

---

## Deployment Topology (v1)

```mermaid
flowchart LR
    Client -->|HTTPS| Nginx[Nginx / Reverse Proxy]
    Nginx --> App[Spring Boot :8080]
    App --> MySQL[(MySQL :3306)]
    App --> Vol[Mounted Volume /uploads]
```

| Environment | App | Database | File storage |
|-------------|-----|----------|--------------|
| Local dev | `localhost:8080` | MySQL Docker or local | `./uploads/` |
| Production | Container or VM | Managed MySQL | Persistent volume or future S3 |

---

## Design Principles

1. **Stateless** — no HTTP sessions; scale horizontally by adding app instances
2. **Metadata vs. bytes** — MySQL stores metadata; disk stores file content
3. **Storage abstraction** — `FileStorageService` interface hides local disk; swap to S3 later
4. **DTO boundary** — entities never exposed directly in API responses
5. **Fail secure** — unauthenticated or unauthorized requests rejected before reaching services
6. **Async side effects** — email sending does not block the HTTP response
