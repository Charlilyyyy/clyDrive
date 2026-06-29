# Non-Functional Requirements

Quality attributes and constraints that shape how clyDrive is built and operated.

---

## 1. Security

| Requirement | Detail |
|-------------|--------|
| Authentication | Stateless JWT-based auth; no server-side sessions |
| Access token lifetime | Short-lived (15 minutes) to limit exposure if leaked |
| Refresh token lifetime | Long-lived (7 days) with rotation on each refresh |
| Token blacklist | Invalidate access tokens on logout and password change |
| Password storage | BCrypt hashing; never store or return plain-text passwords |
| Password policy | Enforce history limit — cannot reuse last 5 passwords |
| Account lockout | Lock after 5 failed login attempts for 15 minutes |
| Authorization | Role-based access control (`USER`, `ADMIN`) |
| Resource ownership | Users can only access their own files and folders |
| Email verification | Account inactive until email is confirmed |
| OTP expiry | Password-reset OTPs expire after 10 minutes |
| Secrets management | `JWT_SECRET`, mail credentials via environment variables — never in source code |
| HTTPS | Production deployment must terminate TLS at the reverse proxy or load balancer |

---

## 2. Scalability

| Requirement | Detail |
|-------------|--------|
| Stateless API | No session affinity required; horizontal scaling of app instances |
| Database connection pool | HikariCP with configurable min/max pool size |
| File storage abstraction | Local disk in v1; storage layer designed so S3-compatible backends can be swapped in |
| Lazy initialization | Spring beans loaded on demand to reduce cold-start memory |
| Pagination | File and user list endpoints return paginated results |
| Async email | Verification and OTP emails sent asynchronously to avoid blocking requests |

---

## 3. Performance

| Requirement | Detail |
|-------------|--------|
| File streaming | Download responses stream bytes — do not load entire file into memory |
| JPA open-in-view | Disabled to prevent lazy-loading queries during response serialization |
| Jackson serialization | Skip null fields; avoid serializing empty beans |
| Connection timeout | HikariCP connection timeout set to 20 seconds |
| Indexing | Database indexes on frequently queried columns (`user_id`, `email`, `username`, share `token`) |

---

## 4. Reliability & Data Integrity

| Requirement | Detail |
|-------------|--------|
| Transactional uploads | File metadata and storage-quota update must succeed or roll back together |
| Soft delete | User deletion is soft — record retained for audit trail |
| Scheduled cleanup | Background jobs purge expired tokens, OTPs, and share links |
| Audit logging | Significant actions recorded with user ID, action type, IP, and timestamp |
| Idempotent logout | Blacklisting an already-blacklisted token must not error |

---

## 5. Maintainability

| Requirement | Detail |
|-------------|--------|
| Layered architecture | Controller → Service → Repository separation |
| DTOs | Request/response objects separate from JPA entities |
| Global exception handler | Centralized error mapping to consistent `ApiResponse` format |
| Structured logging | SLF4J with contextual log messages per endpoint |
| Configuration externalization | Security limits, JWT expiry, and mail settings in `application.properties` |
| Package structure | Group by concern: `controller`, `service`, `repository`, `module`, `security`, `exception` |

---

## 6. Observability

| Requirement | Detail |
|-------------|--------|
| Application logs | Rolling file appender (`application.log`) |
| Error logs | Separate rolling error log (`error.log`) |
| Request logging | Log IP, URI, and key identifiers on each endpoint entry/exit |
| Login audit | Persist every login attempt with status (success / failed / locked) |

---

## 7. Usability (API Consumer)

| Requirement | Detail |
|-------------|--------|
| Consistent responses | All endpoints return `ApiResponse<T>` with `status`, `message`, `path`, and `data` |
| Meaningful HTTP codes | `201` on create, `200` on success, `400` validation, `401` unauthenticated, `403` forbidden, `404` not found |
| Validation messages | Bean Validation (`@Valid`) with clear field-level error messages |
| API versioning | All routes prefixed with `/api/v1` |

---

## 8. Compatibility & Deployment

| Requirement | Detail |
|-------------|--------|
| Java version | Java 21 |
| Database | MySQL 8.x |
| Build tool | Maven |
| Containerization | Docker Compose for local app + MySQL (planned) |
| Environment profiles | Separate config for `dev` and `prod` via Spring profiles |

---

## Configuration Reference

Values planned for `application.properties`:

```properties
# JWT
jwt.access-token-validity=900000        # 15 minutes (ms)
jwt.refresh-token-validity=604800000    # 7 days (ms)

# Security
security.max-failed-attempts=5
security.lock-time-minutes=15
security.password-history-limit=5
otp.expiry.minutes=10

# Connection pool
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000

# JPA
spring.jpa.open-in-view=false
spring.main.lazy-initialization=true
```
