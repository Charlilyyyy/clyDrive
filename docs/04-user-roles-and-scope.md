# User Roles & Scope

Who can do what in clyDrive, and what is intentionally excluded from the first version.

---

## Roles

clyDrive has two roles. Every account has exactly one.

| Role | Description |
|------|-------------|
| `USER` | Default role on registration. Can manage own files, folders, and shares. |
| `ADMIN` | Elevated role. Can manage all users and view system-wide statistics. |

A seeded admin account is created on first application startup.

---

## Permissions Matrix

| Action | USER | ADMIN |
|--------|:----:|:-----:|
| Register & verify email | ✓ | — |
| Login / logout / refresh token | ✓ | ✓ |
| Change own password | ✓ | ✓ |
| View own login history | ✓ | ✓ |
| Upload, download, delete own files | ✓ | ✓ |
| Create and manage own folders | ✓ | ✓ |
| Create and revoke own share links | ✓ | ✓ |
| View own storage usage | ✓ | ✓ |
| Access another user's files | ✗ | ✗ |
| View system statistics | ✗ | ✓ |
| List / view all users | ✗ | ✓ |
| Lock, unlock, enable, disable users | ✗ | ✓ |
| Change user roles | ✗ | ✓ |
| Soft-delete users | ✗ | ✓ |
| Adjust user storage quota | ✗ | ✓ |

> Admins manage users — they do **not** get automatic access to other users' files.

---

## Account States

Each user has a `status` field and supporting flags that control access.

| Status | Meaning | Can log in? |
|--------|---------|:-----------:|
| `ACTIVE` | Normal operating account | ✓ (if email verified) |
| `DISABLED` | Manually deactivated by admin | ✗ |
| `LOCKED` | Too many failed login attempts, or locked by admin | ✗ |
| `DELETED` | Soft-deleted by admin | ✗ |

Additional flags:

| Flag | Purpose |
|------|---------|
| `emailVerified` | Must be `true` before login is allowed |
| `enabled` | Admin toggle; `false` blocks login regardless of status |
| `accountNonLocked` | Set to `false` during lockout window |
| `lockedUntil` | Timestamp when automatic lockout expires |
| `failedAttempts` | Counter reset on successful login |

**Login is allowed only when:** `emailVerified = true`, `enabled = true`, `accountNonLocked = true`, and `status = ACTIVE`.

---

## Public (Unauthenticated) Endpoints

These endpoints do not require a JWT:

| Endpoint | Purpose |
|----------|---------|
| `POST /api/v1/users/register` | Create account |
| `POST /api/v1/auth/login` | Authenticate |
| `POST /api/v1/auth/refresh-token` | Renew access token |
| `GET  /api/v1/auth/verify-email` | Confirm email via token |
| `POST /api/v1/auth/resend-verification-email` | Resend verification |
| `POST /api/v1/auth/forgot-password/email` | Start password reset |
| `POST /api/v1/auth/resend-password-otp` | Resend reset OTP |
| `POST /api/v1/auth/verify-password-otp` | Verify reset OTP |
| `POST /api/v1/auth/reset-password` | Set new password |
| `GET  /api/v1/share/{token}` | Download via share link |

All other endpoints require a valid Bearer token.

---

## Out of Scope (v1)

The following are **not** included in the first version. They may be added later.

| Excluded | Reason |
|----------|--------|
| Frontend / web UI | Backend-only; clients consume the REST API directly |
| Mobile apps | Same as above |
| Real-time collaboration | Multi-user editing is a separate domain |
| File versioning / revision history | Adds significant storage and metadata complexity |
| In-app file preview (PDF, image viewer) | Requires rendering layer beyond storage API |
| Full-text search inside files | Needs indexing infrastructure (e.g. Elasticsearch) |
| OAuth / social login (Google, GitHub) | Email/password auth is sufficient for v1 |
| Two-factor authentication (2FA) | Planned enhancement, not required for MVP |
| S3 / cloud object storage | v1 uses local disk; abstraction layer allows future swap |
| CDN delivery for downloads | Direct streaming from app server in v1 |
| Rate limiting middleware | Documented as a production recommendation, not built in v1 |
| Multi-tenancy / organizations | Single-user accounts only |
| Folder-level sharing | v1 shares individual files only |
| Edit permission on shares | v1 share links are view/download only |
| Trash / recycle bin with restore | v1 uses hard delete for files |
| WebSocket notifications | No real-time push in v1 |
| Billing / payment integration | Storage quota is admin-managed, not purchased |

---

## Future Considerations

Items worth revisiting after v1 is complete:

- S3-compatible storage backend
- Folder sharing and collaborative permissions
- Trash with soft-delete and restore for files
- Rate limiting and request throttling
- OpenAPI / Swagger UI for interactive API exploration
- Docker-based production deployment
- OAuth2 social login
