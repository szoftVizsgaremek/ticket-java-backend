# Ticket System API

Base URL (dev): `http://localhost:8080/api` · Format: JSON · Auth: session cookie

This document describes the backend as built for users, tickets and auth. Section 7 lists known gaps and planned changes, so read it before relying on anything here long term.

## Endpoint index

| Method | Path | Who can call it | Summary |
|---|---|---|---|
| POST | `/auth/login` | public | Log in, sets the `session` cookie |
| POST | `/auth/logout` | anyone | End the session, clear the cookie |
| GET | `/auth/me` | logged in | Current user |
| POST | `/users` | admin or superadmin | Create a user |
| GET | `/users` | logged in | List **active** users, ordered by name |
| GET | `/users/{id}` | logged in | One user (`me` allowed). Works for deactivated users too |
| PATCH | `/users/{id}` | see rules | Update a user / change a password |
| DELETE | `/users/{id}` | admin or superadmin (see rules) | **Deactivate** a user (soft delete) |
| POST | `/users/{id}/reactivate` | admin or superadmin (see rules) | Reactivate a user |
| POST | `/tickets` | logged in | Create a ticket |
| GET | `/tickets` | logged in | All tickets, newest first |
| GET | `/tickets/{id}` | logged in | One ticket |
| GET | `/tickets/assigned-to/{userId}` | logged in | Tickets assigned to a user, newest first |
| GET | `/tickets/opened-by/{userId}` | logged in | Tickets opened by a user, newest first |
| PATCH | `/tickets/{id}` | superadmin, or matching job title | Update a ticket |

---

## 1. Conventions

### Authentication
- `POST /auth/login` sets a cookie named `session` (`HttpOnly`, `SameSite=Lax`, `Path=/`, 7 days). Every other endpoint except login and logout needs it.
- The frontend must send cookies on every request (`credentials: "include"` in `fetch`).
- A missing, unknown, or expired session, or a deactivated account, returns `401 {"message": "Not authenticated"}`.
- Sessions live in server memory and are lost when the server restarts. The frontend should treat any 401 as "log in again".

### Errors
Every error response has the same shape, including errors raised by the framework:

```json
{ "message": "Human readable text" }
```

| Status | Meaning |
|---|---|
| 400 | Validation failed, malformed JSON, or a bad path value |
| 401 | Not logged in, or bad credentials on login |
| 403 | Logged in but not allowed (always `"Forbidden"`) |
| 404 | Resource doesn't exist (or is a deactivated user where noted) |
| 405 | Wrong HTTP method |
| 409 | Conflict (duplicate username/email, user already active) |
| 500 | Unexpected server error (`"Internal server error"`) |

Branch on the **status code**. Message wording is not standardized yet and may change.

### Formats
- `birthDate`: `"YYYY-MM-DD"`. Timestamps (`createdAt`, `updatedAt`): ISO-8601 UTC, e.g. `"2026-11-10T09:01:04.781991Z"`.
- Unknown JSON fields in a request body are silently ignored (for example, `username` in a user PATCH does nothing).
- In PATCH bodies, a field that is **omitted** is left unchanged.
- Text values are trimmed before they are stored.

---

## 2. Roles and permissions

Roles: `user`, `admin`, `superadmin`. They control permissions only. `jobTitle` is a separate, descriptive field (with one exception: it is used for the ticket-update rule).

| Action | user | admin | superadmin |
|---|---|---|---|
| View users and tickets | yes | yes | yes |
| Create a ticket | yes | yes | yes |
| Create a user | no | yes, role `user` only | yes, any role |
| Change own password | yes | yes | yes |
| Edit own name / email / jobTitle / birthDate | no | no | yes |
| Edit another user's profile fields | no | only `user`-role accounts | anyone |
| Change anyone's role | no | no | yes |
| Reset another user's password | no | only `user`-role accounts | anyone |
| Deactivate / reactivate a user | no | only other `user`-role accounts | anyone, including self |
| Update a ticket | job-title match | job-title match | always |

Superadmin accounts are created directly in the database, not through the API.

---

## 3. Objects

### User (`UserResponse`)
```json
{
  "id": 7,
  "name": "Test Admin2",
  "username": "testadmin2",
  "birthDate": "1992-01-01",
  "role": "admin",
  "jobTitle": "Area Manager",
  "active": true,
  "email": "ta2@example.com"
}
```
Password hashes are never returned.

### User summary (`UserSummary`, embedded in tickets)
```json
{ "id": 7, "name": "Test Admin2", "username": "testadmin2", "jobTitle": "Area Manager", "active": true }
```

### Ticket (`TicketResponse`)
```json
{
  "id": 1,
  "name": "Fan noise",
  "description": "Loud fan on rack 3",
  "status": "open",
  "priority": "high",
  "type": "hardware",
  "uploader": { "id": 5, "name": "Test Admin", "username": "testuser", "jobTitle": "Software Engineer", "active": true },
  "assignee": { "id": 7, "name": "Test Admin2", "username": "testadmin2", "jobTitle": "Area Manager", "active": true },
  "createdAt": "2026-11-10T09:01:04.781991Z",
  "updatedAt": "2026-11-10T09:06:25.751232Z"
}
```
`uploader` and `assignee` can point to deactivated users (`active: false`) so ticket history keeps showing who was involved.

---

## 4. Auth endpoints

### POST `/auth/login`
Body:
```json
{ "username": "testuser", "password": "Abc123!" }
```
- **200**: a User object, plus the `session` cookie.
- **401**: wrong username or password, or the account is deactivated (same message for all three, on purpose).

### POST `/auth/logout`
No body. Always **200** `{"message": "ok"}`; the session is ended and the cookie cleared. Works with or without a cookie.

### GET `/auth/me`
- **200**: the current User.
- **401**: not logged in.

---

## 5. User endpoints

### POST `/users`
Who: admin or superadmin (others get 403).

Body:
```json
{
  "name": "Jane HR", "username": "janehr", "email": "jane@example.com",
  "password": "Abc123!", "jobTitle": "HR Manager", "birthDate": "1990-05-15",
  "role": "user"
}
```
- All fields are required except `role`, which defaults to `"user"`. An **admin** may only create `role: "user"`; setting any other role gets 403. A superadmin may set any role.
- **201**: the created User.
- **400**: missing fields, a length or password rule violated (section 6), or an invalid role.
- **409**: username or email already exists. This includes deactivated accounts, so a rehire should be a reactivation.

### GET `/users`
- **200**: array of User, active users only, ordered by name. Use this for assignee pickers.

### GET `/users/{id}`
`{id}` is a number or the literal `me`.
- **200**: the User. Deactivated users are still returned (`active: false`).
- **404**: no such user.

### PATCH `/users/{id}`
Body: any subset of
```json
{ "name": "...", "email": "...", "password": "...", "role": "...", "jobTitle": "...", "birthDate": "1990-05-15" }
```
`username` cannot be changed. What is allowed depends on who is calling and who the target is:

| Caller | Target | Allowed fields |
|---|---|---|
| superadmin | any active user | everything, including `role` |
| anyone else | themselves | `password` only; any other field returns 403 |
| admin | another `user`-role account | name, email, jobTitle, birthDate, password; sending `role` returns 403 |
| anyone else | anyone else | nothing (403) |

- **200**: the updated User. An empty body is a valid no-op.
- **404**: the target doesn't exist **or is deactivated**. This is checked before permissions.
- **400**: a length or password rule violated, an invalid role, or a new password identical to the current one.
- **403**: not allowed (see the table).
- **409**: the email is already used by another account.
- Changing someone else's password ends **all of that user's sessions** (they must log in again). Changing your own password keeps your current session.
- A change is all or nothing: if any field is invalid, nothing is saved.

### DELETE `/users/{id}`
Deactivates the user. The row is kept, so ticket history still works.
- **204**: no body.
- **404**: no such user, or already deactivated.
- **403**: not allowed. Admins can only deactivate other `user`-role accounts. Superadmins can deactivate anyone.
- The target's sessions are ended immediately. They can no longer log in and disappear from `GET /users`.

### POST `/users/{id}/reactivate`
No body. Same permission rules as deactivation.
- **204**: no body. The user can log in again with their existing password.
- **404**: no such user.
- **409**: the user is already active.
- **403**: not allowed.

---

## 6. Ticket endpoints

### POST `/tickets`
Body:
```json
{ "name": "Fan noise", "description": "Loud fan on rack 3", "type": "hardware", "priority": "high", "assigneeId": 7 }
```
- Required: `name`, `description`, `type`, `assigneeId`. `priority` is optional and defaults to `low`.
- The caller becomes the uploader. The status always starts as `open`.
- **201**: the created Ticket.
- **400**: missing fields, a value outside the allowed lists, a name or description that is too long, or an assignee that doesn't exist **or is deactivated** (`"Invalid assignee"`).

### GET `/tickets`
**200**: array of Ticket, newest first. There is no pagination, so filter on the client.

### GET `/tickets/{id}`
- **200**: the Ticket.
- **404**: no such ticket.
- **400**: `{id}` is not a number.

### GET `/tickets/assigned-to/{userId}` and GET `/tickets/opened-by/{userId}`
**200**: array of Ticket, newest first. Works for deactivated users too. A non-numeric id returns 400.

### PATCH `/tickets/{id}`
Body: any subset of
```json
{ "status": "closed", "priority": "high", "type": "software", "assigneeId": 3 }
```
`name` and `description` can't be changed after creation. Any status change is allowed, including reopening a closed ticket.

Who can update: a **superadmin**, or any user whose `jobTitle` **exactly equals** the uploader's or the **current** assignee's `jobTitle` (case-sensitive). The admin role gives no extra access to tickets.

- **200**: the updated Ticket. An empty body is a no-op and does not change `updatedAt`.
- **403**: no job-title match.
- **404**: no such ticket.
- **400**: a value outside the allowed lists, or the new assignee doesn't exist or is deactivated (`"Invalid assignee"`).
- The permission check uses the assignee as it was **before** the request, so reassigning a ticket to yourself cannot unlock it.
- The current uploader and assignee may be deactivated. Their coworkers can still update the ticket.
- A change is all or nothing, as with users.

---

## 7. Validation reference

| Field | Rule |
|---|---|
| `name` (user) | 1 to 100 characters |
| `username` | 1 to 50 characters, unique (case-insensitive), cannot be changed |
| `email` | 1 to 255 characters, unique (case-insensitive). No format check yet |
| `jobTitle` | 1 to 100 characters |
| `birthDate` | `YYYY-MM-DD`, required on create |
| `password` | at least 6 characters, with a lowercase letter, an uppercase letter, a digit and a special (non-alphanumeric) character; at most 72 bytes |
| `role` | `user`, `admin`, `superadmin` |
| ticket `name` | 1 to 100 characters |
| ticket `description` | 1 to 250 characters |
| ticket `status` | `open`, `in_progress`, `closed` |
| ticket `priority` | `low`, `moderate`, `high`, `critical` |
| ticket `type` | `thermal`, `software`, `hardware` |

---

## 8. Behavior notes

- **Deactivation is the only way to remove a user.** There is no hard delete. Deactivated users keep their tickets, can't log in, don't appear in `GET /users`, and are rejected as new assignees. They are still readable through `GET /users/{id}` and inside tickets.
- **Audit log.** Password changes and resets, role changes (with `old -> new`), deactivations and reactivations are recorded server-side. There is no endpoint to read the log yet.
- **Tickets are never deleted**, only closed.

---

## 9. Known gaps and planned changes

Known gaps in the current build:

1. `GET /users/{id}` with a non-numeric id other than `me` currently returns **500** instead of 400.
2. No CORS configuration was written during the build. If the frontend runs on a different origin, one must be added, with credentials allowed.
3. No CSRF protection beyond `SameSite=Lax`. The `Secure` cookie flag is not set (fine for local HTTP, not for production).
4. Sessions never expire on the server and are lost on restart.
5. The 3-month password expiry is **not enforced** yet. The time of the last change is recorded, nothing reads it.
6. Changing your own password does not require the current password (open decision).
7. No login throttling or lockout.
8. No pagination. `GET /tickets` returns everything.
9. Concurrent edits to the same record are last-write-wins (optimistic locking not added).
10. Not built yet: comments, attachments, password reset by email, ticket change history.
11. Plain users see every employee's birth date and email in `GET /users` (open decision).

Planned, but **not decided**. Tell the frontend team early:

- **Spring Security rework.** The cookie will probably be renamed `JSESSIONID`, and `POST`/`PATCH`/`DELETE` may need a CSRF header copied from a cookie. Error bodies will keep the `{"message": ...}` shape.
- **Pagination.** List responses could change from a bare array to an object that wraps the items. Route all list fetching through one function in the frontend now, so that change stays small.
- **Error message wording** will be standardized.

---

## 10. Frontend checklist

- Send cookies on every request, and handle 401 globally by returning to the login screen.
- Treat `DELETE /users/{id}` as "deactivate". Show deactivated users as former employees (`active: false`) wherever they appear in ticket history.
- Handle `204 No Content` (no JSON body) for deactivate and reactivate.
- Use `GET /users` for assignee pickers, since it already excludes deactivated users.
- Hide buttons the user can't use, as the permission table in section 2 describes. The server still enforces every rule.
- Filter and sort tickets on the client for now.
- Use dropdowns for `role`, `status`, `priority`, `type`, and `jobTitle`. Job titles are matched as exact text, so a typo breaks the ticket-update rule.
