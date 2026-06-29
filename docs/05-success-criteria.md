# Success Criteria

Measurable conditions that define when clyDrive is considered complete and working correctly.

---

## Project-Level Goals

The project is **done** when all of the following are true:

| # | Criterion | Verification |
|---|-----------|--------------|
| 1 | Application starts without errors against a MySQL database | Run `mvn spring-boot:run` |
| 2 | All documented API endpoints respond with the standard `ApiResponse` format | Manual or integration tests |
| 3 | JWT authentication protects all non-public routes | Unauthenticated request returns `401` |
| 4 | A user can complete the full lifecycle: register → verify → login → upload → share → download → delete | End-to-end manual test |
| 5 | Admin can manage users and view system stats | Admin token on `/api/v1/admin/*` |
| 6 | Storage quota is enforced on upload | Upload beyond quota returns `400` or `403` |
| 7 | Expired share links and tokens are rejected | Wait for expiry or mock time |
| 8 | Secrets are loaded from environment variables, not hardcoded | Code review |
| 9 | README contains setup instructions sufficient for a new developer | Peer review |
| 10 | API is documented via OpenAPI / Swagger | `/swagger-ui` accessible |

---

## Module Acceptance Criteria

### User Management

- [ ] `POST /api/v1/users/register` creates a user with `role = USER` and `emailVerified = false`
- [ ] Duplicate username or email returns a `409` conflict error
- [ ] Password is stored as a BCrypt hash

### Authentication

- [ ] `POST /api/v1/auth/login` returns access token and refresh token on valid credentials
- [ ] Login fails with `401` when email is not verified
- [ ] Account locks after 5 consecutive failed attempts
- [ ] `POST /api/v1/auth/logout` blacklists the access token; subsequent requests with it return `401`
- [ ] `POST /api/v1/auth/refresh-token` issues a new access token and rotates the refresh token
- [ ] `GET /api/v1/auth/login-history` returns only the requesting user's history

### Password Management

- [ ] Forgot-password flow sends OTP to registered email
- [ ] OTP expires after 10 minutes
- [ ] Reset password rejects passwords that match any of the last 5 used
- [ ] `POST /api/v1/auth/change-password` invalidates existing tokens

### File Management

- [ ] `POST /api/v1/files/upload` stores file on disk and saves metadata in DB
- [ ] `GET /api/v1/files/{id}/download` streams the correct file content
- [ ] User cannot access another user's file (returns `403` or `404`)
- [ ] `DELETE /api/v1/files/{id}` removes both disk file and DB record
- [ ] Upload increments `storageUsed`; delete decrements it

### Folder Organization

- [ ] `POST /api/v1/folders` creates a root or nested folder
- [ ] `GET /api/v1/folders/{id}` returns subfolders and files inside
- [ ] Deleting a non-empty folder is handled per design (block or cascade)
- [ ] Moving a file updates its folder reference

### File Sharing

- [ ] `POST /api/v1/files/{id}/share` returns a tokenized link with expiry
- [ ] `GET /api/v1/share/{token}` downloads the file without authentication
- [ ] Expired or revoked token returns `404` or `410`
- [ ] Only the file owner can create or revoke shares

### Storage Quota

- [ ] Default quota assigned on registration
- [ ] Upload blocked when `storageUsed + fileSize > storageQuota`
- [ ] User can view current usage vs. limit

### Admin

- [ ] All `/api/v1/admin/*` endpoints return `403` for `USER` role
- [ ] `GET /api/v1/admin/stats` returns accurate user and storage counts
- [ ] Admin can lock, unlock, enable, disable, and soft-delete users
- [ ] Admin can change a user's role between `USER` and `ADMIN`

### Cross-Cutting

- [ ] All errors return consistent `ApiResponse` with appropriate HTTP status
- [ ] Audit log entries created for login, upload, delete, share, and admin actions
- [ ] Scheduled job purges expired tokens, OTPs, and share links

---

## Requirements Checklist

All requirement documents are complete before implementation begins:

| Document | Covers |
|----------|--------|
| [Problem Statement](01-problem-statement.md) | Why the project exists and who it serves |
| [Core Features](02-core-features.md) | What the system does, grouped by module |
| [Non-Functional Requirements](03-non-functional-requirements.md) | Security, performance, and operational constraints |
| [User Roles & Scope](04-user-roles-and-scope.md) | Who can do what; what is excluded from v1 |
| [Success Criteria](05-success-criteria.md) | How we know the project is finished |

---

## Smoke Test Scenario

A single end-to-end path that validates the core value of clyDrive:

```
1. Register a new user
2. Verify email via link
3. Login → receive tokens
4. Upload a file (e.g. test.pdf)
5. Create a folder and move the file into it
6. Generate a share link
7. Download the file via the share link (no auth)
8. Delete the file
9. Logout
10. Confirm the old access token is rejected
```

If all 10 steps pass, the core product works.
