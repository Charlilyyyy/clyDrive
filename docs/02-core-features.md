# Core Features

Functional requirements for clyDrive, grouped by module.

---

## 1. User Management

| Feature | Description |
|---------|-------------|
| Register | Create an account with username, email, and password |
| Email verification | Activate account via a link sent to the registered email |
| Resend verification | Request a new verification email if the original expired or was lost |
| Profile defaults | Assign default role (`USER`) and storage quota on registration |

**API prefix:** `/api/v1/users`

---

## 2. Authentication & Session

| Feature | Description |
|---------|-------------|
| Login | Authenticate with username or email; receive access + refresh tokens |
| Logout | Invalidate the current access token and revoke the refresh token |
| Refresh token | Obtain a new access token without re-entering credentials |
| Login history | View past login attempts (timestamp, IP, status) |
| Account lockout | Lock account after repeated failed login attempts |

**API prefix:** `/api/v1/auth`

---

## 3. Password Management

| Feature | Description |
|---------|-------------|
| Forgot password | Request an OTP sent to the registered email |
| Verify OTP | Confirm identity before allowing a password reset |
| Resend OTP | Request a new OTP during the reset flow |
| Reset password | Set a new password after OTP verification |
| Change password | Authenticated users can update their password |
| Password history | Prevent reuse of the last N passwords |

**API prefix:** `/api/v1/auth`

---

## 4. File Management

| Feature | Description |
|---------|-------------|
| Upload | Upload a file via multipart request; store metadata in DB and bytes on disk |
| Download | Stream file content to the client by file ID |
| List files | Paginated list of the authenticated user's files |
| File metadata | Retrieve name, type, size, and upload date for a single file |
| Delete | Remove file from storage and delete metadata record |
| Rename | Update the display name of a file |
| Move | Place a file inside a folder |

**API prefix:** `/api/v1/files`

---

## 5. Folder Organization

| Feature | Description |
|---------|-------------|
| Create folder | Create a root or nested folder (optional parent folder) |
| List folders | List root-level folders for the authenticated user |
| Folder contents | View subfolders and files inside a folder |
| Rename folder | Update folder name |
| Delete folder | Remove an empty folder, or cascade-delete contents |
| Breadcrumbs | Resolve the full path from root to a folder |

**API prefix:** `/api/v1/folders`

---

## 6. File Sharing

| Feature | Description |
|---------|-------------|
| Create share link | Generate a tokenized link with an expiry date |
| Public download | Allow anyone with the link to download the file (no login required) |
| List shares | View active share links for a file |
| Revoke share | Invalidate a share link before it expires |
| Expiry enforcement | Reject access to expired or revoked links |

**API prefix:** `/api/v1/files` (create/list) · `/api/v1/share` (public access)

---

## 7. Storage Quota

| Feature | Description |
|---------|-------------|
| Default quota | Every user receives a storage limit on registration |
| Usage tracking | Increment on upload, decrement on delete |
| Quota check | Block uploads when `storageUsed >= storageQuota` |
| View usage | User can see used vs. total storage |
| Admin override | Admin can adjust a user's quota |

---

## 8. Admin Management

| Feature | Description |
|---------|-------------|
| System stats | Total users, storage consumed, and other aggregates |
| List users | View all registered users |
| Get user | View a single user by ID |
| Update role | Promote or demote between `USER` and `ADMIN` |
| Lock / unlock | Manually lock or unlock a user account |
| Enable / disable | Activate or deactivate a user without deleting |
| Soft delete | Mark a user as deleted while retaining audit trail |

**API prefix:** `/api/v1/admin` · requires `ADMIN` role

---

## 9. Cross-Cutting

| Feature | Description |
|---------|-------------|
| Audit logging | Record significant actions (login, upload, delete, share, admin ops) |
| Token blacklist | Invalidate JWTs on logout and password change |
| Scheduled cleanup | Purge expired tokens, OTPs, and share links |
| Standard API responses | Consistent `ApiResponse` wrapper with status, message, and data |
| Global error handling | Uniform error format with appropriate HTTP status codes |

---

## Feature Summary

```
clyDrive
├── Users        → register, verify email
├── Auth         → login, logout, refresh, login history
├── Password     → forgot, reset, change
├── Files        → upload, download, list, delete, rename, move
├── Folders      → create, list, rename, delete, nest
├── Sharing      → create link, public download, revoke
├── Quota        → track usage, enforce limits
└── Admin        → stats, user CRUD, role management
```
