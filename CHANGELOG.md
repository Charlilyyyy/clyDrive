# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- User registration with email verification and BCrypt password hashing.
- JWT authentication with access/refresh token rotation, token blacklist, and login-attempt lockout.
- Login history and a seeded default admin account.
- Password management: forgot-password OTP flow, reset, authenticated change, and password-history reuse prevention.
- Email delivery for verification, OTP, and password-change notifications.
- Admin panel: system stats and user lifecycle (role, lock/unlock, enable/disable, soft delete) with audit logging.
- File engine: multipart upload, streamed download, paginated listing, metadata, and delete on a pluggable local-disk storage layer.
- Folder organization: nested folders, breadcrumb resolution, rename, empty-only delete, and file move.
- File sharing: expiring share links with optional password protection, public download, listing, and revocation.
- Storage quotas: per-user enforcement, a self-service usage endpoint, and an admin quota override.
- Central audit service and a scheduled cleanup job for expired tokens, OTPs, share links, and old audit logs.
- OpenAPI/Swagger documentation and Docker Compose deployment (app + MySQL).

[Unreleased]: https://example.com/clydrive
