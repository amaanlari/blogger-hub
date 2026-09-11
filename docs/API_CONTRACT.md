# Blogger Hub — API Contract

> **API_CONTRACT.md represents the API contract discovered from the current backend implementation.**
> Every endpoint, field, status code, and rule below was read directly from the source code (controllers, services, DTOs, security config, `application.yaml`). Nothing here is invented. Where the implementation is inconsistent or looks unintentional, it is called out explicitly in **⚠️ boxes** so the frontend team knows what to expect instead of what "should" happen.

---

## 0. Read this before writing any HTTP client code

These four facts change how every single response must be parsed. They are easy to miss by reading controller code alone.

### 0.1 Every JSON field is `snake_case` on the wire

`application.yaml` sets:
```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```
All Java fields are camelCase, but Jackson serializes them as `snake_case`. Boolean getters named `isXxx()` become the property `xxx` (not `is_xxx`) per standard JavaBean introspection — e.g. `isPremium()` → JSON key `premium`; `isRead()` → JSON key `read`; `isVerified()` → JSON key `verified`. `isEmailNotificationsEnabled()` → `email_notifications_enabled`.

### 0.2 Every response body is wrapped a second time by a global advice

`GlobalResponseHandler` (a `ResponseBodyAdvice`) wraps **every** controller return value in:

```json
{
  "timestamp": "2026-09-09T12:34:56.789",
  "data": { /* whatever the controller returned */ },
  "error": null
}
```

...**unless** the body is already an `ApiResponse` (only true for the two exception handlers below) or the request path contains `/v3/api-docs` or `/actuator`.

This means most endpoints — which return a `DataResponse`/`SuccessResponse`/`ErrorResponse` (`{success, status_code, message, data|error}`) — are **double-wrapped**. For example, a successful login is:

```json
{
  "timestamp": "2026-09-09T12:34:56.789",
  "data": {
    "success": true,
    "status_code": 200,
    "message": "Logged in",
    "data": {
      "user_id": "65f...",
      "access_token": "eyJ...",
      "refresh_token": "eyJ..."
    }
  },
  "error": null
}
```
The real payload is at `response.data.data`, not `response.data`. This applies to nearly every endpoint documented below — look for the "Full wire response" example on each one.

A few endpoints (`POST /api/auth/access-token`, `POST /api/auth/refresh-token`, `GET /api/users/health`) return a plain object/String directly (not a `DataResponse`), so for those the payload is one level shallower, at `response.data` directly.

### 0.3 Two completely different error-body shapes exist

- **Exceptions caught by `GlobalExceptionHandler`** (only `ResourceNotFoundException` and generic `Exception`) produce an **un-wrapped** `ApiResponse` (bypasses §0.2 because it's already `ApiResponse`):
  ```json
  { "timestamp": "...", "data": null, "error": { "status": "NOT_FOUND", "message": "...", "subErrors": null } }
  ```
- **Errors returned directly by a controller/service** as `ResponseEntity.status(X).body(new ErrorResponse(...))` go through the double-wrap in §0.2:
  ```json
  { "timestamp": "...", "data": { "success": false, "status_code": 400, "message": "...", "error": null }, "error": null }
  ```
  Note `error` appears at **both** the outer and inner level, and the inner one is usually `null` even on failure — the real message is `data.message`.

See **Section: Error Contract** for the full breakdown of which endpoints produce which shape.

### 0.4 ⚠️ Known runtime gaps the frontend will hit immediately

| # | Issue | Why it matters |
|---|-------|-----------------|
| 1 | **CORS `allowCredentials(true)` + `allowedOrigins("*")`** in `SecurityConfig.corsConfigurationSource()` is an invalid combination — Spring rejects `*` origins when credentials are allowed. Any browser request that relies on cookies/credentials from a different origin will fail CORS at runtime. | If the frontend calls the API cross-origin with `credentials: 'include'`, expect CORS failures until the backend fixes this. Calling without credentials (pure `Authorization: Bearer` header, no cookies) largely avoids the issue since the browser won't require the credentialed CORS path — but this is backend config, not something the frontend can work around cleanly. |
| 2 | **CORS `allowedMethods` is `GET, POST, PUT, DELETE, OPTIONS` — `PATCH` is missing.** Several endpoints are mapped with `@PatchMapping` (blog post premium toggle, user partial update, notification read/read-all). | A browser preflight for a `PATCH` request will be rejected by CORS even though the endpoint itself works fine for same-origin or non-browser clients (Postman, mobile apps, `curl`). |
| 3 | **Validation annotations on DTOs are frequently declared but not enforced.** Only `LoginRequestDto` (via `AuthController.login`) and `BlockUserRequestDto` (via `BlockController.blockUser`) are annotated with `@Valid` on the controller parameter. `SignupRequestDto`, `UpdateBlogUserRequestDto`, `BlogPostRequestDto`, `CommentRequestDto` etc. have **no `@Valid`**, so their `@NotBlank`/`@Size`/`@Email` constraints are **never checked** by the framework. | Do not rely on the backend to reject e.g. an empty `username` on signup, or an invalid email on profile update — it will silently accept it. |
| 4 | **When `@Valid` validation *does* fail (login, block-user), the resulting `MethodArgumentNotValidException` is NOT specifically handled** — `GlobalExceptionHandler` only has handlers for `ResourceNotFoundException` and `Exception`, so it falls into the generic `Exception` handler and returns **HTTP 500**, not 400. | A malformed login/block request returns a 500 with a long Spring-generated validation message as `error.message`, not a clean 400. |
| 5 | **Authentication failures on login/signup/logout/token-refresh return HTTP 500, not 401.** `AuthService` throws `BadCredentialsException` (wrong password, duplicate username/email, invalid/expired refresh token) and `UsernameNotFoundException` (user not found), but neither is registered in `GlobalExceptionHandler`. Both extend `Exception`, so they're caught by the generic handler → **500 Internal Server Error**, with `error.message` set to things like `"Invalid credentials"`, `"Username already exists"`, `"Invalid token"`, `"User not found"`. | Do not branch UI logic on HTTP status 401 for login failures — check `error.message` text instead, and treat 500 as a possible "expected" failure for auth endpoints. |
| 6 | **`ResourceNotFoundException` is defined but never thrown anywhere in the codebase.** Its `@ExceptionHandler` (the only one that produces a real 404 via `GlobalExceptionHandler`) is effectively dead code. | Real 404s only come from endpoints that manually build `ResponseEntity.status(404)` (see per-endpoint tables below) — most "not found" scenarios elsewhere (e.g. viewing a non-existent user by username, generating a token for a deleted user) surface as 500, not 404. |
| 7 | **`assert` statements in `LikesService.removeLike` and `CommentsService.removeComment`** are no-ops in production (Java assertions are disabled unless the JVM is run with `-ea`). Removing a non-existent like/comment will `NullPointerException` internally → 500, not 404. | Guard against calling remove endpoints for IDs you haven't confirmed exist. |
| 8 | **`GET /api/users/all-users-details`** (ADMIN_USER only) returns the **raw `BlogUser` documents** straight from the repository, including the bcrypt `password` hash field. | This leaks password hashes to any admin-authenticated client. Flagged here for awareness — not modified as part of this documentation task. |
| 9 | **`GET /api/users/health`** returns a plain `String` body (`"Service is up and running"`). Because `GlobalResponseHandler` unconditionally tries to wrap every body in `ApiResponse`, and Spring has already selected `StringHttpMessageConverter` for this return type before the advice runs, this is very likely to throw a `ClassCastException` at serialization time. Not independently executed/confirmed in this pass — treat as unverified but high-risk. | Don't rely on this route for health checks from the frontend; use `/actuator/health` instead (see Authentication section). |
| 10 | **`GET /api/interactions/likes/post/users`** is a `@GetMapping` that reads a `@RequestBody`. GET requests with a body are non-standard; `fetch`/`axios` support sending one, but intermediary proxies/caches may strip it. | Prefer testing this endpoint directly before wiring it into the UI. |

---

## 1. Tech stack (from `pom.xml` / source)

| Aspect | Value |
|---|---|
| Spring Boot | 3.3.5 (`spring-boot-starter-parent`) |
| Java | 21 |
| Build tool | Maven |
| Database | MongoDB (`spring-boot-starter-data-mongodb`) |
| Cache/OTP store | Redis (`spring-boot-starter-data-redis`) |
| Messaging | Kafka (`spring-kafka`) — used internally for async notification fan-out, not exposed as an HTTP API |
| Security | Spring Security 6, stateless, custom JWT (`com.auth0:java-jwt`) |
| File storage | Cloudinary (profile pictures) |
| Email | Gmail API (OAuth2) + `spring-boot-starter-mail` |
| Validation | `spring-boot-starter-validation` (Jakarta Bean Validation) — see §0.4 items 3–4 for how inconsistently it's actually wired up |
| API docs | **No Swagger/OpenAPI dependency present.** `springdoc`/`swagger` do not appear anywhere in `pom.xml`. `SecurityConfig` whitelists `/swagger-ui/**`, `/swagger-resources/**`, `/v2/api-docs`, and `GlobalResponseHandler` special-cases `/v3/api-docs`, but none of these paths are actually served — there is no OpenAPI generator on the classpath. Treat this as leftover/anticipatory configuration, not a working docs endpoint. |

---

## 2. API Summary Table

| Method | Endpoint | Purpose | Auth | Roles | Success |
|---|---|---|---|---|---|
| **Authentication** |
| POST | `/api/auth/signup` | Register a new user | No | — | 200 |
| POST | `/api/auth/login` | Log in, get tokens | No | — | 200 |
| POST | `/api/auth/logout` | Invalidate one refresh token | No¹ | — | 200 |
| POST | `/api/auth/logout-all` | Invalidate all refresh tokens for a user | No¹ | — | 200 |
| POST | `/api/auth/access-token` | Exchange refresh token for new access token | No¹ | — | 200 |
| POST | `/api/auth/refresh-token` | Rotate refresh token, get new access+refresh token | No¹ | — | 200 |
| POST | `/api/auth/verify-otp` | Verify email via OTP | No | — | 200 |
| **Users** |
| GET | `/api/users/health` | Liveness check | No | — | 200 (see §0.4 #9) |
| GET | `/api/users/id/{userId}` | Get user by ID (self only) | Yes | FREE_USER + self | 200 |
| GET | `/api/users/{username}` | Get user by username | No² | — | 200 |
| GET | `/api/users/all` | List all users (summary DTO) | Yes | ADMIN_USER | 200 |
| GET | `/api/users/all-users-details` | List all users (raw documents, incl. password hash — see §0.4 #8) | Yes | ADMIN_USER | 200 |
| PUT | `/api/users/{userId}` | Full profile update (self only) | Yes | FREE_USER + self | 200 |
| PATCH | `/api/users/{userId}` | Partial profile update via arbitrary field map (self only) | Yes | FREE_USER + self | 200 |
| POST | `/api/users/{userId}/upload-profile-picture` | Upload profile picture (self only) | Yes | FREE_USER + self | 200 |
| POST | `/api/users/{userId}/remove-profile-picture` | Remove profile picture (self only) | Yes | FREE_USER + self | 200 |
| **Blog Posts** |
| GET | `/api/blogposts/{username}` | List a user's posts | Yes | any authenticated | 200 |
| GET | `/api/blogposts/post/{id}` | Get a single post by ID | Yes | any authenticated (+ PREMIUM_USER for premium posts) | 200 |
| POST | `/api/blogposts` | Create a post | Yes | any authenticated | 200 |
| PUT | `/api/blogposts/{id}` | Update a post (author only) | Yes | author | 200 |
| DELETE | `/api/blogposts/{id}` | Delete a post (author only) | Yes | author | 200 |
| PATCH | `/api/blogposts/{id}/premium` | Toggle a post's premium flag (⚠️ not ownership-checked, see notes) | Yes | PREMIUM_USER | 200 |
| **Blocking** |
| POST | `/api/block` | Block a user | Yes | FREE_USER | 200 |
| DELETE | `/api/block/{userId}` | Unblock a user | Yes | FREE_USER | 200 |
| GET | `/api/block` | List blocked users (paginated) | Yes | FREE_USER | 200 |
| GET | `/api/block/check/{userId}` | Check if a user is blocked | Yes | FREE_USER | 200 |
| **Comments** |
| POST | `/api/interactions/comments` | Add a top-level comment | Yes | any authenticated | 200 |
| POST | `/api/interactions/comments/reply` | Reply to a comment | Yes | any authenticated | 200 |
| DELETE | `/api/interactions/comments` | Delete a comment (author only) | Yes | any authenticated | 200 |
| GET | `/api/interactions/comments` | List comments for a post | Yes | any authenticated | 200 |
| **Likes** |
| POST | `/api/interactions/likes` | Like a post | Yes | any authenticated | 200 |
| DELETE | `/api/interactions/likes` | Unlike a post | Yes | any authenticated | 200 |
| GET | `/api/interactions/likes/user/posts` | List posts liked by current user | Yes | any authenticated | 200 |
| GET | `/api/interactions/likes/post/users` | List users who liked a post (author only, see §0.4 #10) | Yes | post author | 200 |
| **Notifications** |
| GET | `/api/notifications` | List notifications (paginated, filterable) | Yes | FREE_USER | 200 |
| GET | `/api/notifications/unread-count` | Count unread notifications | Yes | FREE_USER | 200 |
| PATCH | `/api/notifications/{notificationId}/read` | Mark one notification read | Yes | FREE_USER | 200 |
| PATCH | `/api/notifications/read-all` | Mark all notifications read | Yes | FREE_USER | 200 |
| DELETE | `/api/notifications/{notificationId}` | Delete a notification | Yes | FREE_USER | 200 |

¹ These endpoints take the refresh token in the request **body**, not the `Authorization` header, and are matched by the `/api/auth/**` security whitelist, so the JWT filter's header check is irrelevant to them — but they are meaningless without a valid refresh token in the body.
² `/api/users/{username}` is explicitly whitelisted in `SecurityConfig.AUTH_WHITELIST`, so it does **not** require a bearer token, unlike almost every other non-auth endpoint.

---

## 3. Authentication & Authorization

### 3.1 Mechanism: stateless JWT, no sessions, no cookies

- `SecurityConfig` sets `SessionCreationPolicy.STATELESS` and disables CSRF.
- A custom `JwtFilter` (`OncePerRequestFilter`) runs before `UsernamePasswordAuthenticationFilter` and reads the **access token** from:
  ```
  Authorization: Bearer <access_token>
  ```
- If the header is missing, malformed, or the token fails verification, **the filter does not reject the request** — it just leaves `SecurityContext` unauthenticated and logs a warning, then continues the chain. The actual 401 comes later from Spring Security's `authorizeHttpRequests` rule (`.anyRequest().authenticated()`) via `AccessTokenEntryPoint`, or from a specific `@PreAuthorize` check.
- Tokens are signed with HMAC512 using two **separate** secrets (`jwt.auth.accessTokenSecret`, `jwt.auth.refreshTokenSecret`), issuer claim `"blogger-hub"`.
- Access token claims: `sub` = user ID, `iat`, `exp` (`jwt.auth.accessTokenExpirationMinutes` from config, env var `ACCESS_TOKEN_EXPIRATION_MINUTES`).
- Refresh token claims: `sub` = user ID, `tokenId` = a server-side `RefreshToken` document ID, `iat`, `exp` (`jwt.auth.refreshTokenExpirationDays`, env var `REFRESH_TOKEN_EXPIRATION_DAYS`). Refresh tokens are **stateful** — the `tokenId` claim must still exist in the `refresh_token` Mongo collection or the token is treated as invalid, even if not yet expired. This is how logout/logout-all work (they delete the DB row).
- No cookies are set or read anywhere in the codebase. No CSRF tokens. No OAuth for end users (Google OAuth is used only server-side for the Gmail-sending service account, unrelated to user auth).

### 3.2 Login flow the frontend should implement

1. `POST /api/auth/signup` with `{username, email, password, bio}` → creates the user (unverified), sends a verification OTP email asynchronously, and **immediately** returns a working `access_token`/`refresh_token` pair. The user does **not** need to verify their email before receiving tokens from signup.
2. `POST /api/auth/login` with `{username, password}`:
   - If the account has `is_verified == false`, the backend **re-sends** a verification OTP email and returns `401` with message `"User not verified"` (this is the one genuine 401 auth failure — see §0.4 #5 for why other auth failures return 500 instead).
   - Otherwise authenticates via Spring's `AuthenticationManager` (BCrypt password check) and returns `access_token`/`refresh_token`.
3. Store `access_token` and `refresh_token` client-side (e.g. memory + secure storage — there is no cookie mechanism, so this is entirely the frontend's responsibility).
4. Attach `Authorization: Bearer <access_token>` to every subsequent request to a non-whitelisted path.
5. On `access_token` expiry (calls start failing on protected routes — see below for what that looks like), call `POST /api/auth/access-token` with `{refresh_token}` to get a new access token without rotating the refresh token, or `POST /api/auth/refresh-token` to rotate both.
6. On logout, call `POST /api/auth/logout` with `{refresh_token}` to invalidate just that session, or `POST /api/auth/logout-all` to invalidate every refresh token owned by the user (both take the token in the body; the server derives the user ID from the token itself, there is no explicit `user_id` field required).

### 3.3 What "access token expired/invalid" looks like to the frontend

**Verified against a running instance while building the frontend** (see docs/FRONTEND.md) — this section originally guessed the response would be a non-JSON/plain-text body based on reading `AccessTokenEntryPoint.commence()`'s `HttpServletResponse.sendError(401, "Unauthorized")` call in isolation. Actually running it showed that's wrong: `sendError()` triggers Spring Boot's own default `/error` handling, which *does* produce a JSON body for a non-HTML request — and that body then gets caught and double-wrapped by `GlobalResponseHandler` like any other response, since it isn't already an `ApiResponse`. Corrected below.

Because `JwtFilter` swallows invalid/expired tokens silently (see §3.1), a request with a bad/expired token behaves **identically** to a request with no token at all: it reaches `.anyRequest().authenticated()` unauthenticated, `AccessTokenEntryPoint.commence()` fires, and the actual observed response is:
```json
{ "timestamp": "2026-09-09T21:08:21.338230623",
  "data": { "timestamp": "2026-09-09T15:38:21.337+00:00", "status": 401, "error": "Unauthorized", "path": "/api/users/id/..." },
  "error": null }
```
i.e. Spring Boot's standard `BasicErrorController` attributes (`timestamp`, `status`, `error`, `path` — no `message`, no `success`, no `status_code`), sitting under the usual outer `ApiResponse` wrapper. This is a **third, distinct shape** from the two described in §0.3 — it has neither `error` at the outer level (Shape A) nor `success`/`status_code` at the inner level (Shape B). A `@PreAuthorize` role denial (403) was not independently re-verified in this pass but almost certainly produces the same shape with `status: 403`, for the same reason (same default error-handling path).

**Recommendation for the frontend:** don't try to distinguish this from a parse failure — treat any 401 whose body matches *neither* Shape A nor Shape B as "token invalid/expired," and any such 403 as "insufficient permissions." Both are handled generically in `frontend/src/api/client.ts`.

### 3.4 Public (no-token-required) endpoints

From `SecurityConfig.AUTH_WHITELIST` + the explicit `permitAll()` on `/api/auth/**` and `/actuator/**`:
```
/api/auth/**
/api/users/{username}      (GET — get a user's public profile by username)
/actuator/**
/swagger-resources/**, /swagger-ui.html, /v2/api-docs, /swagger-ui/**   (not actually served — see §1)
```
Everything else requires a valid, authenticated principal (`anyRequest().authenticated()`), on top of which many endpoints add `@PreAuthorize` role checks.

### 3.5 Roles

Defined in `enums/Role.java`: `FREE_USER`, `PREMIUM_USER`, `ADMIN_USER`. Every new signup gets `[FREE_USER]` by default (`BlogUser`'s default constructor). Roles are **not mutually exclusive tiers** in the code — a user's `roles` field is a `List<Role>`, so a user could in principle hold multiple roles simultaneously, but nothing in the exposed API lets a user promote themselves or another user; there is no "grant role" endpoint at all. Role changes would have to happen directly in the database or via the generic `PATCH /api/users/{userId}` partial-update endpoint if the caller sets the `roles` field directly (that endpoint accepts an arbitrary `Map<String, Object>` and reflectively sets matching fields on `BlogUser` — see its docs below for why this is a broad/risky endpoint).

`@PreAuthorize("hasRole('FREE_USER')")` in practice means "any authenticated normal user," since every account has this role by default. `@PreAuthorize("hasAnyRole('ADMIN_USER')")` / `hasRole('ADMIN_USER')` gates the two admin user-listing endpoints. `@PreAuthorize("hasRole('PREMIUM_USER')")` gates only the blog-post premium-flag toggle endpoint — note this checks the *caller's* role, not the post's ownership (see that endpoint's notes).

### 3.6 Ownership checks (not role-based)

Several endpoints check "is the caller the resource owner" in code rather than via `@PreAuthorize`:
- `BlogUserController.checkUserId()` — used by every `/api/users/{userId}/**` self-service endpoint. If the authenticated user's ID doesn't match the `{userId}` path variable, it returns **401 Unauthorized** (not 403) with body `{success:false, status_code:401, message:"You are not authorized to access this user's profile", error:"Logged in user's id does not match the requested user's id"}`. Note the 401 here despite this being an authorization failure, not an authentication one.
- `BlogPostService.updateBlogPost`/`deleteBlogPost` — checks `blogPost.createdBy.id == currentUser.id`, returns **403 Forbidden** with message `"Logged in user's id does not match requested user's id"` if not.
- `CommentsService.removeComment` — checks `comment.userId == currentUser.id`, returns **403 Forbidden** ("Unauthorized access") if not.
- `LikesService.getUsersWhoLikedPost` — checks the post's author equals the caller; if not, throws `BadCredentialsException("Invalid user")`, which (per §0.4 #5) is **not** specifically handled → surfaces as **500**, not 403.

### 3.7 CORS

From `SecurityConfig.corsConfigurationSource()`:
```
allowedOrigins: ["*"]
allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]   (no PATCH, no HEAD)
allowedHeaders: ["*"]
allowCredentials: true
```
See §0.4 #1 and #2 for why this configuration is broken for credentialed and `PATCH` requests specifically.

---

## 4. Endpoint Reference

Field names below are exactly what's on the wire (snake_case, per §0.1). Every "Full wire response" example shows the **actual** double-wrapped envelope per §0.2, so you can copy it straight into a mock.

### 4.1 Authentication (`/api/auth`) — all public

---
#### `POST /api/auth/signup`
Registers a user, sends a verification OTP email (async, fire-and-forget), and returns usable tokens immediately (the account does not need to be verified to use these tokens — nothing in the code checks `is_verified` except the login flow).

- **Auth:** none
- **Request body:**
  ```json
  { "username": "janedoe", "email": "jane@example.com", "password": "Secr3tPass!", "bio": "Hi, I write about Java." }
  ```
- **Request schema:**
  | Field | Type | Required | Constraints |
  |---|---|---|---|
  | `username` | string | yes (by convention; **not enforced**, see §0.4 #3) | 3–20 chars, declared not blank |
  | `email` | string | yes (not enforced) | valid email, ≤60 chars |
  | `password` | string | yes (not enforced) | 6–40 chars |
  | `bio` | string | no | ≤300 chars |

  ⚠️ None of these constraints are actually validated — `signup()` has no `@Valid`. Malformed/empty values will be accepted by the framework and may fail later at the DB layer (e.g. unique index violations on username/email) or succeed with garbage data.
- **Success — 200:**
  ```json
  { "success": true, "status_code": 200, "message": "Signed up",
    "data": { "user_id": "665f1...", "access_token": "eyJ...", "refresh_token": "eyJ..." } }
  ```
  Full wire response wraps this in `{timestamp, data: <above>, error: null}` per §0.2.
- **Errors:**
  | Status | When | Body (`error.message` inside the un-wrapped `ApiResponse`, since this hits the generic exception handler) |
  |---|---|---|
  | 500 | Username already taken, email already registered, or any other exception during signup | `"Invalid credentials"` — the service catches its own specific errors and re-throws a generic `BadCredentialsException("Invalid credentials")`, so the specific reason ("Username already exists" / "Email already exists") is **swallowed** and never reaches the client. |
- **Business rules:** Password is BCrypt-hashed server-side. A `RefreshToken` document is created and tied to the new user. Default role `FREE_USER`, `is_verified: false`, default profile picture from `cloudinary.cloud.default-profile-pic`.
- **Frontend notes:** Because the specific duplicate-username/email reason is discarded, you cannot distinguish "username taken" from "email taken" from this response — both look like a generic 500 with `"Invalid credentials"`.

---
#### `POST /api/auth/login`
- **Auth:** none
- **Request body:** `{ "username": "janedoe", "password": "Secr3tPass!" }`
- **Request schema:** `username` (string, `@NotBlank`), `password` (string, `@NotBlank`) — **this endpoint is one of only two in the whole API where `@Valid` is actually wired up**, so a blank field does get rejected, but see the 500-not-400 caveat below.
- **Success — 200:** same shape as signup: `DataResponse` with `message: "Logged in"`, `data: {user_id, access_token, refresh_token}`.
- **Errors:**
  | Status | When |
  |---|---|
  | 401 | Account exists but `is_verified == false`. Body: `{success:false, status_code:401, message:"User not verified", error:null}` (double-wrapped `ErrorResponse`, per §0.2). A verification email is re-sent as a side effect. |
  | 500 | `username`/`password` blank (validation exception, unhandled — §0.4 #4), user not found, or wrong password (`BadCredentialsException`/`UsernameNotFoundException`, unhandled — §0.4 #5). All produce the un-wrapped `ApiResponse{data:null, error:{status:"INTERNAL_SERVER_ERROR", message:"Invalid credentials"}}` shape, **except** the "user not found" case, whose `error.message` is `"User not found"`. |
- **Frontend notes:** There is no reliable 400/401 signal for "wrong password" — treat 500 with these specific messages as expected outcomes for this endpoint, not as unexpected server errors.

---
#### `POST /api/auth/logout`
- **Auth:** none required by the security layer (path is whitelisted), but functionally requires a valid refresh token in the body.
- **Request body:** `{ "refreshToken": "eyJ..." }` — ⚠️ **this one DTO is NOT snake_case on the way in** (`RefreshTokenRequestDto` has no `@JsonProperty`/naming override applied differently than others, but Jackson's global `SNAKE_CASE` strategy still converts the single field `refreshToken` → `refresh_token` on both serialize and deserialize, same as everywhere else). So the correct body key is **`refresh_token`**, matching the pattern everywhere else in the API.
- **Success — 200:** `SuccessResponse`: `{success:true, status_code:200, message:"Logged out"}`.
- **Errors:**
  | Status | When |
  |---|---|
  | 500 | Refresh token invalid, expired, or its `tokenId` no longer exists in the DB → `BadCredentialsException("Invalid token")`, unhandled → 500 with `error.message: "Invalid token"`. Also 500 if `refresh_token` is missing entirely (NPE decoding a null token). |
- **Business rule:** deletes the single `RefreshToken` row identified by the token's `tokenId` claim. Does not touch the access token — an already-issued access token remains valid until it expires naturally (no server-side access-token revocation exists anywhere in this API).

---
#### `POST /api/auth/logout-all`
Same shape/behavior as `logout`, but deletes **every** `RefreshToken` row owned by the token's user ID (`refreshTokenRepository.deleteByOwner_Id`). Message on success: `"Logged out from all"`. Same 500-on-invalid-token behavior.

---
#### `POST /api/auth/access-token`
Exchanges a still-valid, still-DB-present refresh token for a **new access token**, without rotating the refresh token itself.
- **Request body:** `{ "refresh_token": "eyJ..." }`
- **Success — 200:** returns a **bare `TokenResponseDto`**, not wrapped in `DataResponse` — so the full wire response is only single-wrapped:
  ```json
  { "timestamp": "...", "data": { "user_id": "665f1...", "access_token": "eyJ...(new)", "refresh_token": "eyJ...(same, echoed back)" }, "error": null }
  ```
- **Errors:** 500, `error.message: "Invalid token"`, same conditions as logout.

---
#### `POST /api/auth/refresh-token`
Like `access-token`, but **rotates** the refresh token: deletes the old `RefreshToken` DB row, creates a new one, and returns a brand-new refresh token string alongside the new access token. Same request/response/error shape as `access-token` above, except `refresh_token` in the response is a genuinely new value.

---
#### `POST /api/auth/verify-otp`
Verifies a user's email using the 6-digit numeric OTP sent by `sendVerificationEmail` (Redis-backed, TTL from `otp.ttl` config, default 1).
- **Request body:** `{ "email": "jane@example.com", "otp": "483920" }`
- **Success — 200:** `SuccessResponse`: `{success:true, status_code:200, message:"Email verified"}`. Sets `is_verified: true` on the matching `BlogUser`.
- **Errors:**
  | Status | When |
  |---|---|
  | 401 | OTP doesn't match or has expired. Body: `{success:false, status_code:401, message:"Invalid OTP", error:null}`. |
  | 500 | `email` doesn't correspond to any user *and* the OTP happened to match (edge case — `verifyEmail` looks up the user only after the OTP check passes) → `UsernameNotFoundException("User not found by email")`, unhandled → 500. |

---

### 4.2 Users (`/api/users`)

---
#### `GET /api/users/health`
- **Auth:** none (not in the whitelist by path, but also not matched by `anyRequest()` restrictions in a way that matters — actually **not whitelisted**, so per `.anyRequest().authenticated()` this technically requires auth; test before assuming it's public). Returns plain text `"Service is up and running"`.
- ⚠️ See §0.4 #9 — likely broken by the global response wrapper. Do not use this as your app's health check; use `/actuator/health` if enabled for your environment, or confirm this route's actual behavior in a running instance first.

---
#### `GET /api/users/id/{userId}`
Fetch a user's own profile by ID. Self-access only.
- **Auth:** Bearer token. Role: `FREE_USER`. Ownership: path `{userId}` must equal the caller's own ID.
- **Path params:** `userId` (string, Mongo ObjectId as string)
- **Success — 200:**
  ```json
  { "success": true, "status_code": 200, "message": "User found.",
    "data": { "id": "665f1...", "username": "janedoe", "email": "jane@example.com",
              "bio": "Hi, I write about Java.", "profile_picture": "https://res.cloudinary.com/...",
              "roles": ["FREE_USER"] } }
  ```
- **Errors:**
  | Status | When |
  |---|---|
  | 401 | `{userId}` doesn't match the caller's own ID. Body: `{success:false, status_code:401, message:"You are not authorized to access this user's profile", error:"Logged in user's id does not match the requested user's id"}`. |
  | 500 | User ID doesn't exist → `UsernameNotFoundException("User not found")`, unhandled → 500. |
  | 403 | Caller lacks `FREE_USER` role (shouldn't occur for normal accounts, since it's granted by default) — standard Spring Security `@PreAuthorize` denial; see §3.3 for the actual response shape. |

---
#### `GET /api/users/{username}`
Public profile lookup by username. **The only user-facing GET besides the auth routes that requires no token.**
- **Auth:** none
- **Path params:** `username` (string)
- **Success — 200:** same `BlogUserResponseDto` shape as above (includes `email` — there is no field-level redaction for public profile views; anyone can see anyone's email address and role list via this endpoint).
- **Errors:** 500 if the username doesn't exist (`UsernameNotFoundException`, unhandled).

---
#### `GET /api/users/all`
- **Auth:** Bearer token. Role: `ADMIN_USER`.
- **Success — 200:** `data` is a JSON array of `BlogUserResponseDto` (same shape as above, one per user).
- **Special case — 204 No Content:** if there are zero users, returns `SuccessResponse` with status 204 and message `"No users found."`. ⚠️ Returning a JSON body alongside a 204 status is technically invalid per HTTP semantics; some HTTP clients will strip/ignore the body on a 204 response, so don't rely on reading `message` in this case — treat 204 as "empty list" and move on.
- **Errors:** 403 if not `ADMIN_USER` (Spring Security denial).

---
#### `GET /api/users/all-users-details`
- **Auth:** Bearer token. Role: `ADMIN_USER`.
- **Success — 200:** `data` is a JSON array of **raw `BlogUser` Mongo documents** — ⚠️ see §0.4 #8. Fields include everything in the `BlogUser` entity: `id, username, email, password (bcrypt hash!), bio, profile_picture, verified, email_notifications_enabled, roles, status, created_at, updated_at`. No DTO projection is applied here, unlike every other user-listing endpoint.
- **Frontend notes:** Do not render `password` from this response anywhere in the UI, and consider whether this endpoint should even be wired into an admin dashboard given the exposure — flagging for your team's awareness, not something this documentation pass fixes.

---
#### `PUT /api/users/{userId}`
Full profile replace. Self-access only.
- **Auth:** Bearer token. Role: `FREE_USER`. Ownership enforced (401 on mismatch, same as `GET .../id/{userId}`).
- **Path params:** `userId`
- **Request body:**
  ```json
  { "username": "janedoe2", "email": "jane2@example.com", "password": "NewPass123!", "bio": "Updated bio" }
  ```
- **Request schema:** identical field set/constraints as `SignupRequestDto` (`username` 3–20 chars, `email` valid+≤60 chars, `password` 6–40 chars, `bio` ≤300 chars) — ⚠️ again **not enforced**, no `@Valid` on this controller method.
- **Business rule:** **all four fields are overwritten unconditionally** — there is no partial-update semantics here and no "leave blank to keep existing value" behavior. Sending an empty string for any field will blank it out. `password` is always re-hashed and saved, even if it's identical to before (there's no "did the password change" check).
- **Success — 200:** `SuccessResponse`: `{success:true, status_code:200, message:"User updated successfully."}` (no updated user object is returned — the frontend must already have the new values it just sent, or re-fetch via `GET /api/users/id/{userId}`).
- **Errors:** 401 on ownership mismatch (same shape as above); 500 if `userId` doesn't exist.

---
#### `PATCH /api/users/{userId}`
Arbitrary partial field update. Self-access only.
- **Auth:** Bearer token. Role: `FREE_USER`. Ownership enforced (401 on mismatch).
- **Path params:** `userId`
- **Request body:** an arbitrary JSON object whose keys are **Java field names on `BlogUser` exactly as declared in the class** (camelCase, e.g. `"bio"`, `"profilePicture"`, `"emailNotificationsEnabled"` — NOT the snake_case wire names used elsewhere, because this map is matched via Java reflection (`BlogUser.class.getDeclaredField(fieldName)`) against the raw incoming map keys, which bypasses Jackson's property-naming translation for the *keys* of a `Map<String,Object>`). Example:
  ```json
  { "bio": "New bio text", "emailNotificationsEnabled": false }
  ```
  ⚠️ **This is a materially different key convention than every other endpoint in this API** — every other request/response body uses snake_case field names (per §0.1), but this one requires exact Java field names because it's a raw `Map<String,Object>`, not a DTO Jackson can rename. Sending `"email_notifications_enabled"` here will silently do nothing (no matching field found → `NoSuchFieldException` → see error table).
- **Business rule / risk:** because this reflects directly onto `BlogUser`'s declared fields with no allow-list, a caller can set **any** field on the entity this way, including `roles`, `status`, `password` (as a plain string, bypassing BCrypt hashing entirely!), or `id`. There is no server-side restriction limiting this to "safe" profile fields. Documented as-is; not modified in this pass.
- **Success — 200:** `SuccessResponse`: `{success:true, status_code:200, message:"User updated successfully."}`.
- **Errors:**
  | Status | When |
  |---|---|
  | 401 | Ownership mismatch. |
  | 500 | Any key in the map doesn't match a declared `BlogUser` field (`NoSuchFieldException`, wrapped in a `RuntimeException`, unhandled) or the value type doesn't match the field's type (`IllegalAccessException`/`IllegalArgumentException` at the reflective `field.set()` call). |

---
#### `POST /api/users/{userId}/upload-profile-picture`
- **Auth:** Bearer token. Role: `FREE_USER`. Ownership enforced (401 on mismatch).
- **Path params:** `userId`
- **Request:** `multipart/form-data` with a file part. The controller parameter is `@RequestParam MultipartFile profilePicture` with no explicit `name=` attribute, so the expected form field name is **`profilePicture`** (relies on the compiler retaining parameter names — standard for Spring Boot's default Maven compiler config, but worth confirming against a live request if uploads mysteriously 400).
- **Success — 200:**
  ```json
  { "success": true, "status_code": 200, "message": "Profile picture added successfully.",
    "data": { /* raw Cloudinary upload API response — includes secure_url, public_id, width, height, format, bytes, etc. */ } }
  ```
  `data` is whatever Cloudinary's Java SDK returns from `uploader().upload(...)` — an untyped `Map`. The field your UI actually wants is `data.secure_url`.
- **Errors:**
  | Status | When |
  |---|---|
  | 401 | Ownership mismatch. |
  | 500 | Cloudinary upload failure (network, quota, bad file, etc.) → explicit `ErrorResponse` with `message:"Failed to add profile picture."` and `error` set to the underlying exception message (this one **does** carry a useful `error` field, unlike most others). |
- **Business rule:** uploads to Cloudinary path `blogger_hub/{userId}/profile_pic` with a fixed `public_id` of `profile-pic-image/{userId}` and `overwrite: true` — re-uploading always replaces the previous picture at the same Cloudinary asset ID.

---
#### `POST /api/users/{userId}/remove-profile-picture`
- **Auth:** Bearer token. Role: `FREE_USER`. Ownership enforced (401 on mismatch).
- **Success — 200:** `SuccessResponse`, message `"Profile picture removed successfully."` (or `"...Profile pic is not a URL."` if the stored value didn't start with `http`, an edge case for legacy/malformed data).
- **Errors:**
  | Status | When |
  |---|---|
  | 401 | Ownership mismatch. |
  | 400 | User's `profile_picture` already equals the default (`cloudinary.cloud.default-profile-pic`) — nothing to remove. Body: `{success:false, status_code:400, message:"Profile picture is already removed.", error:"User has no profile picture to remove."}`. |
  | 500 | Cloudinary deletion failure. |
- **Business rule:** resets `profile_picture` to the configured default image URL; does not delete the user's Cloudinary asset unless the current value looks like a URL.

---

### 4.3 Blog Posts (`/api/blogposts`)

---
#### `GET /api/blogposts/{username}`
List all posts authored by a given username.
- **Auth:** Bearer token required (this path is **not** in the whitelist, unlike the visually-similar `/api/users/{username}`).
- **Path params:** `username`
- **Success — 200:** `data` is an array of `BlogPostResponseDto`:
  ```json
  { "success": true, "status_code": 200, "message": "Blog posts found successfully.",
    "data": [ {
      "blog_post_id": "66a1...", "title": "Hello World", "description": "My first post",
      "banner_image_url": "https://...", "content": "<p>...</p>", "premium": false,
      "created_by": "janedoe", "created_at": "2026-08-01T10:00:00Z",
      "updated_by": "janedoe", "updated_at": "2026-08-01T10:00:00Z"
    } ] }
  ```
  Note `created_by`/`updated_by` are **plain username strings** here (the service explicitly resolves them from the `BlogUserRef`), unlike `GET /api/blogposts/post/{id}` below where they come back as nested objects.
- **Business rule:** returns **all** posts by that user regardless of premium flag or the caller's role — there is no premium filtering on the list endpoint, only on the single-post endpoint.
- **Errors:** 500 if `username` doesn't exist (`UsernameNotFoundException`, unhandled).

---
#### `GET /api/blogposts/post/{id}`
Fetch a single post, enforcing the premium paywall.
- **Auth:** Bearer token required.
- **Path params:** `id` (blog post ID)
- **Success — 200:** `data` is the **raw `BlogPost` Mongo document** (not the `BlogPostResponseDto` used elsewhere — inconsistent shape):
  ```json
  { "success": true, "status_code": 200, "message": "Blog post found successfully.",
    "data": {
      "blog_post_id": "66a1...", "title": "Hello World", "description": "My first post",
      "banner_image_url": "https://...", "content": "<p>...</p>", "premium": false,
      "created_by": { "id": "665f1...", "username": "janedoe" },
      "created_at": "2026-08-01T10:00:00Z",
      "updated_by": { "id": "665f1...", "username": "janedoe" },
      "updated_at": "2026-08-01T10:00:00Z"
    } }
  ```
  Here `created_by`/`updated_by` are **objects** `{id, username}`, not strings — the opposite of the list endpoint above. Handle both shapes if you share a TypeScript type between the two calls.
- **Errors:**
  | Status | When |
  |---|---|
  | 404 | Post ID doesn't exist. Body: `{success:false, status_code:404, message:"Blog post not found", error:null}`. This is one of the few genuine, correctly-coded 404s in the API. |
  | 403 | Post's `premium` flag is `true` and the caller's roles don't include `PREMIUM_USER`. Body: `{success:false, status_code:403, message:"This is a premium post", error:null}`. |
- **Business rule:** the premium check only triggers if `authentication.getPrincipal()` is a `BlogUser` instance (always true for a real authenticated request) — a free user gets the 403 with **no post content at all** (not even title/description as a teaser).

---
#### `POST /api/blogposts`
Create a post.
- **Auth:** Bearer token required. No specific role beyond being authenticated.
- **Request body:**
  ```json
  { "title": "Hello World", "description": "My first post", "banner_image_url": "https://...", "content": "<p>Full content</p>" }
  ```
- **Request schema:** `title`, `description`, `banner_image_url`, `content` — all plain strings, **no validation constraints declared or enforced** (no annotations on `BlogPostRequestDto` at all). Any field, including all of them, can be null/blank/omitted and the post will still be created.
- **Business rule:** `is_premium` is **not settable at creation** — it always defaults to `false` (Java's default `boolean`) regardless of what's sent in the body (there's no `isPremium`/`premium` field on `BlogPostRequestDto`, so nothing to copy). To make a post premium, a separate `PATCH .../{id}/premium` call is required afterward, by a `PREMIUM_USER`-role caller (not necessarily the author — see that endpoint).
- **Success — 200:** `SuccessResponse`: `{success:true, status_code:200, message:"Blog post created successfully"}` — **the created post's ID is not returned.** The frontend must call `GET /api/blogposts/{username}` afterward to discover the new post's ID.
- **Errors:** none explicitly coded; a bad request would surface as a 500 from an unexpected exception (e.g. `BeanUtils.copyProperties` failure), not documented as a normal path here since it isn't a designed behavior.

---
#### `PUT /api/blogposts/{id}`
Update a post. Author-only.
- **Auth:** Bearer token required. Ownership: caller must be the post's `created_by`.
- **Path params:** `id`
- **Request body:** same shape as create (`title, description, banner_image_url, content`).
- **Business rule:** `is_premium` is **untouched** by this call (same reasoning as create — the DTO has no premium field, and `BeanUtils.copyProperties` only copies matching property names, so the existing `is_premium` value on the document is preserved across updates). `updated_at` is explicitly set to `Instant.now()` after the copy.
- **Success — 200:** `SuccessResponse`, message `"Blog post updated successfully"`.
- **Errors:**
  | Status | When |
  |---|---|
  | 404 | Post ID doesn't exist. `message:"Blog post not found"`. |
  | 403 | Caller isn't the post's author. `message:"Logged in user's id does not match requested user's id"`. |

---
#### `DELETE /api/blogposts/{id}`
Author-only delete.
- **Auth:** Bearer token required. Ownership enforced (403 on mismatch, same message as update).
- **Success — 200:** `SuccessResponse`, message `"Blog post deleted successfully"`.
- **Errors:** 404 (post not found), 403 (not the author). Same bodies as `PUT`.
- **Business rule:** hard delete — no soft-delete/archival. Does **not** cascade-delete the post's comments or likes (no code does this), so orphaned `Comments`/`Likes` documents referencing the deleted `post_id` will remain in Mongo.

---
#### `PATCH /api/blogposts/{id}/premium`
Toggle a post's premium flag.
- **Auth:** Bearer token required. Role: `PREMIUM_USER`. ⚠️ **No ownership check** — any user holding the `PREMIUM_USER` role can toggle the premium flag on **any** post in the system, including posts they didn't author. This looks like it was intended as an author-only or admin-only action but is implemented as a bare role check.
- **Path params:** `id`
- **Query params:** `is_premium` (boolean, required — note the query param name uses `@RequestParam(name = "is_premium")`, so this one query parameter genuinely is snake_case at the Java annotation level, not just via the global Jackson strategy which doesn't apply to query params anyway).
  Example: `PATCH /api/blogposts/66a1.../premium?is_premium=true`
- **Success — 200:** `SuccessResponse`, message `"Blog post premium status updated successfully"`.
- **Errors:** 404 if post ID doesn't exist (`message:"Blog post not found"` — note: this handler duplicates the literal string rather than reusing `Constant.POST_NOT_FOUND`, though the text happens to be identical).

---

### 4.4 Blocking (`/api/block`)

All endpoints require `@PreAuthorize("hasRole('FREE_USER')")`, i.e. any authenticated user.

---
#### `POST /api/block`
- **Request body:** `{ "user_id": "665f2...", "reason": "spam" }` — `@Valid` is applied here (one of only two endpoints in the whole API where it's wired up); `user_id` is `@NotBlank`, `reason` is optional/nullable.
- **Success — 200:** `SuccessResponse`, message `"User blocked successfully"`, or `"User is already blocked"` if the block already existed (idempotent — not an error).
- **Errors:**
  | Status | When |
  |---|---|
  | 400 | `user_id` equals the caller's own ID ("Cannot block yourself"). |
  | 404 | Target user ID doesn't exist ("User not found"). |
  | 500 | `user_id` blank (unhandled validation exception, per §0.4 #4). |
- **Business rule:** blocking has broad side effects handled elsewhere in the system — blocked users' notifications to each other are suppressed (`NotificationService.createNotification` checks the block relationship both directions before creating a notification), but blocking does **not** retroactively hide existing posts/comments/likes between the two users, and does not prevent the blocked user from still commenting/liking the blocker's content going forward (no block check exists in `CommentsService`/`LikesService`/`BlogPostService`). Only the notification-suppression side effect is actually implemented, despite the class-level Javadoc comment claiming broader effects ("Viewing each other's posts," "Commenting," "Following").

---
#### `DELETE /api/block/{userId}`
- **Path params:** `userId` (the blocked user's ID)
- **Success — 200:** `SuccessResponse`, `"User unblocked successfully"`.
- **Errors:** 404 if no block relationship exists between the caller and `userId` ("Block relationship not found").

---
#### `GET /api/block`
Paginated list of users the caller has blocked.
- **Query params:** `page` (int, default `0`), `size` (int, default `20`) — zero-indexed pages, standard Spring Data `Pageable` semantics. No sort parameter is exposed (fixed insertion order from Mongo).
- **Success — 200:**
  ```json
  { "success": true, "status_code": 200, "message": "Blocked users fetched successfully",
    "data": {
      "blocked_users": [
        { "blockId": "...", "blockedAt": "2026-08-01T12:00:00Z", "reason": "spam",
          "userId": "665f2...", "username": "spammer99", "profilePicture": "https://...", "bio": "..." }
      ],
      "pagination": { "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
    } }
  ```
  ⚠️ **This response is NOT snake_case** — it's built from an ad-hoc `Map<String,Object>` with hard-coded camelCase keys (`blockId`, `blockedAt`, `userId`, `totalElements`, etc.) rather than a typed DTO, so Jackson's global `SNAKE_CASE` strategy (which only rewrites *bean property* names, not literal map keys) does not apply here. This is the **one endpoint in the entire API whose data payload breaks the snake_case convention** described in §0.1 — copy the exact keys shown above.
- **Business rule:** if the blocked user's account was subsequently deleted, `userId`/`username`/`profilePicture`/`bio` are simply omitted from that entry's map (only `blockId`, `blockedAt`, `reason` remain) rather than erroring.

---
#### `GET /api/block/check/{userId}`
- **Path params:** `userId`
- **Success — 200:** `{ "success": true, "status_code": 200, "message": "Block status fetched successfully", "data": { "isBlocked": true } }` — ⚠️ again a raw `Map.of("isBlocked", ...)`, so the key is **`isBlocked`**, not `is_blocked` — same snake_case-breaking pattern as above.
- **Business rule:** direction-sensitive — this only reports whether *the caller* has blocked `userId`, not whether `userId` has blocked the caller. (A separate, non-HTTP-exposed helper `areUsersBlocked` checks both directions, used internally by notification suppression.)

---

### 4.5 Comments (`/api/interactions/comments`)

---
#### `POST /api/interactions/comments`
Add a top-level comment to a post.
- **Auth:** Bearer token required, any authenticated user.
- **Request body:** `{ "content": "Great post!", "post_id": "66a1..." }` (`parent_id` is accepted by the DTO but explicitly passed as `null` by this controller method regardless of what's sent — use the `/reply` endpoint for threaded replies).
- **Success — 200:** `data` is the saved `Comments` document:
  ```json
  { "success": true, "status_code": 200, "message": "Comment added successfully",
    "data": { "id": "66b2...", "post_id": "66a1...", "user_id": "665f1...", "parent_id": null,
              "content": "Great post!", "created_at": "2026-09-09T10:00:00Z" } }
  ```
- **Errors:** 404 if `post_id` is null or doesn't exist. Body: `{success:false, status_code:404, message:"Post not found", error:null}`.
- **Business rule:** triggers an async Kafka `NotificationEvent` (`POST_COMMENTED`) to the post's author, unless caller == author or the two have blocked each other. Notification delivery is fire-and-forget — a Kafka outage does not fail the comment request itself (producer errors aren't awaited by the controller).

---
#### `POST /api/interactions/comments/reply`
Reply to an existing comment (or post, technically — the only functional difference from the base `addComment` is that `parent_id` is passed through instead of forced to `null`).
- **Request body:** `{ "content": "I agree!", "post_id": "66a1...", "parent_id": "66b2..." }`
- **Success/error shapes:** identical to `POST /api/interactions/comments` above, same "Post not found" 404 if `post_id` is invalid — note `parent_id` is **not validated** to exist; a bogus `parent_id` is silently accepted and just won't match any real parent comment on the client side.
- **Business rule:** if `parent_id` doesn't correspond to an existing comment (`findById` returns empty), the reply is still saved but **no notification is sent** (no error either — the notification block is just skipped since `parentComment` is null).

---
#### `DELETE /api/interactions/comments?comment_id={id}`
Delete a comment. Author-only.
- **Query params:** `comment_id` (string, required)
- **Success — 200:** `data` is the (now-deleted) `Comments` document that was removed, message `"Comment removed successfully"`.
- **Errors:** 403 if caller isn't the comment's author (`message:"Unauthorized access"`). ⚠️ If `comment_id` doesn't exist at all, `commentsRepository.findById(...)` returns empty, and the code does `assert comment != null` (a no-op in production per §0.4 #7) followed immediately by `comment.getUserId()` on a null reference → `NullPointerException` → unhandled → **500**, not 404.
- **Business rule:** if the deleted comment is a top-level comment (`parent_id == null`), **all** comments whose `parent_id` equals this comment's ID are also deleted (`deleteCommentsByParentId`) — but this is only one level deep; if any of those replies themselves had further replies (not supported by the UI model here since `parent_id` always points to a top-level comment in this schema, but nothing enforces that at the DB level), those would become orphaned.

---
#### `GET /api/interactions/comments?post_id={id}`
List all comments for a post (flat list, not nested/threaded — the frontend is responsible for grouping by `parent_id` into a tree if desired).
- **Query params:** `post_id` (string, required)
- **Success — 200:** `data` is an array of `Comments` documents (or possibly a single object/other shape — the repository method `findCommentsByPostId` is declared to return a raw `Object`, not `List<Comments>`, so its actual runtime shape depends entirely on Spring Data Mongo's query-derivation behavior for a non-collection return type; in practice this returns a `List<Comments>` at runtime for a query expected to match multiple documents, but the declared type doesn't guarantee it). No pagination — all comments for the post are returned in one call.
- **Errors:** none explicit; an invalid/missing `post_id` simply returns an empty list, not a 404.

---

### 4.6 Likes (`/api/interactions/likes`)

---
#### `POST /api/interactions/likes`
- **Request body:** `{ "blog_post_id": "66a1..." }` — note this is a raw `Map<String,String>`, and the controller reads the key literally as `blog_post_id` (already snake_case in the Java source itself, not a naming-strategy artifact).
- **Success — 200:** `data` is the saved `Likes` document: `{ "id": "...", "post_id": "66a1...", "user_id": "665f1...", "created_at": "..." }`, message `"Like added successfully"`.
- **Errors:** none explicitly coded in the controller/service. The `Likes` document has a unique compound index on `(post_id, user_id)` — liking the same post twice as the same user will throw a Mongo `DuplicateKeyException` at the repository layer, which the method signature declares (`throws DuplicateKeyException`) but does **not** catch — it propagates up unhandled → generic `Exception` handler → **500**, not 409. There is no explicit "already liked" check before insert.
- **Business rule:** triggers async `POST_LIKED` notification to the post's author (same suppression rules as comments — skipped if self-like or mutually blocked).

---
#### `DELETE /api/interactions/likes`
- **Request body:** `{ "blog_post_id": "66a1..." }`
- **Success — 200:** `SuccessResponse`, `"Like removed successfully"`.
- **Business rule / risk:** looks up the like via `likesRepository.findByPostId(blogPostId)` — **this finds *a* like on that post, not necessarily the caller's own like** (the query is by `post_id` alone, not `post_id` + `user_id`). The code then does `assert Objects.equals(like.getUserId(), userId)` to check ownership, but per §0.4 #7 **this assertion is a no-op in production**, so in practice this endpoint will delete *whichever* like Mongo happens to return for that post ID — which may belong to a **different user** than the caller, if multiple users liked the same post. Flagging as a likely functional bug; documented as observed, not fixed.
- **Errors:** if the post has no likes at all, `findByPostId` returns `null`, and `like.getUserId()` NPEs → unhandled → 500, not 404.

---
#### `GET /api/interactions/likes/user/posts`
Posts liked by the current authenticated user.
- **Success — 200:** `data` is an array of `BlogPostResponseDto` (same shape as the blog-posts-by-username list, with `created_by`/`updated_by` as plain username strings). `message` is dynamic: `"Liked posts by {username}"`.
- No pagination.

---
#### `GET /api/interactions/likes/post/users`
Users who liked a given post. **Post-author-only.**
- ⚠️ **This is a `GET` with a JSON request body** — `{ "blog_post_id": "66a1..." }` — see §0.4 #10. Not a query param or path variable despite being conceptually read-only.
- **Success — 200:** `data` is an array of `BlogUserResponseDto` for every user who liked the post.
- **Errors:** if the caller isn't the post's author, throws `BadCredentialsException("Invalid user")` → unhandled (per §0.4 #5 pattern) → **500**, not 403. If `blog_post_id` doesn't exist, `blogPostRepository.getByBlogPostId(...)` returns `null` and `.getCreatedBy()` NPEs → also 500.

---

### 4.7 Notifications (`/api/notifications`)

All require `@PreAuthorize("hasRole('FREE_USER')")`.

---
#### `GET /api/notifications`
Paginated, sorted, optionally filtered list of the caller's notifications.
- **Query params:** `page` (int, default `0`), `size` (int, default `20`), `unread_only` (boolean, default `false`) — ⚠️ the Java parameter is `unreadOnly` with no explicit `@RequestParam(name=...)`, so Spring binds it from the query string using the **exact parameter name** `unreadOnly`, not `unread_only` (query-parameter binding is unaffected by the Jackson body-serialization naming strategy in §0.1 — that strategy only applies to JSON bodies, not `@RequestParam`). Use `?unreadOnly=true`, not `?unread_only=true`.
- **Sort:** always `created_at` descending — not client-configurable.
- **Success — 200:**
  ```json
  { "success": true, "status_code": 200, "message": "Notifications fetched successfully",
    "data": {
      "notifications": [ {
        "id": "66c1...", "type": "POST_LIKED",
        "actor": { "id": "665f2...", "username": "bob", "profile_picture": "https://..." },
        "target": { "id": "66a1...", "type": "post", "title": "Hello World" },
        "message": "bob liked your post \"Hello World\"",
        "preview": null, "read": false,
        "created_at": "2026-09-09T09:00:00Z", "read_at": null
      } ],
      "unreadCount": 3,
      "pagination": { "page": 0, "size": 20, "totalElements": 3, "totalPages": 1 }
    } }
  ```
  ⚠️ Same pattern as the blocked-users list: `notifications` and `pagination`'s **inner** contents (`notifications[].*`) go through `NotificationResponseDto`, a real bean, so those *are* snake_case (`profile_picture`, `created_at`, `read_at`, `read` — remember `isRead()` → `read`, not `is_read`). But the **outer** map keys (`notifications`, `unreadCount`, `pagination`) and the `pagination` object's own keys (`page`, `size`, `totalElements`, `totalPages`) are again a raw `Map<String,Object>`/`Map.of(...)`, so `unreadCount` and `totalElements`/`totalPages` stay camelCase. **Mixed casing within a single response is real and expected here** — don't assume a blanket transform will work; match the exact keys shown.
- **Business rule:** notification types are one of `NEW_FOLLOWER, POST_LIKED, POST_COMMENTED, COMMENT_REPLIED, COMMENT_LIKED, MENTION_IN_POST, MENTION_IN_COMMENT` — however, only `POST_LIKED`, `POST_COMMENTED`, and `COMMENT_REPLIED` are actually ever produced anywhere in the exposed API (via likes/comments services). `NEW_FOLLOWER` and the two `MENTION_*` types are modeled end-to-end (enum, email templates, message-building switch) but **there is no "follow" endpoint and no mention-detection logic anywhere in the codebase** — they're unreachable in practice today.

---
#### `GET /api/notifications/unread-count`
- **Success — 200:** `{ "success": true, "status_code": 200, "message": "Unread count fetched successfully", "data": { "count": 3 } }`.

---
#### `PATCH /api/notifications/{notificationId}/read`
- **Path params:** `notificationId`
- **Success — 200:** `SuccessResponse`, `"Notification marked as read"`.
- **Errors:** 404 if not found (`"Notification not found"`); 403 if it belongs to another user (`message: Constant.NOT_AUTHORIZED_TO_ACCESS_PROFILE` = `"You are not authorized to access this user's profile"` — reused from the user-profile constant, slightly mismatched wording for this context but the actual string sent).

---
#### `PATCH /api/notifications/read-all`
- **Success — 200:** `SuccessResponse` whose `message` is dynamically built: e.g. `"5 notifications marked as read"` (the count is interpolated directly into the message string — there's no separate `count` field in the response body, so if the frontend needs the number, it must parse it out of the message text).

---
#### `DELETE /api/notifications/{notificationId}`
- **Path params:** `notificationId`
- **Success — 200:** `SuccessResponse`, `"Notification deleted successfully"`.
- **Errors:** same 404/403 as the mark-as-read endpoint.

---

## 5. Data Models

### 5.1 Request DTOs

| DTO | Used by | Fields (wire name: type, required¹, constraints) |
|---|---|---|
| `SignupRequestDto` | `POST /api/auth/signup` | `username`: string, declared-required, 3–20 chars (**not enforced**) · `email`: string, declared-required, valid email ≤60 chars (**not enforced**) · `password`: string, declared-required, 6–40 chars (**not enforced**) · `bio`: string, optional, ≤300 chars |
| `LoginRequestDto` | `POST /api/auth/login` | `username`: string, required, blank rejected (**enforced**, but see §0.4 #4 for the 500-not-400 caveat) · `password`: string, required, blank rejected (**enforced**, same caveat) |
| `RefreshTokenRequestDto` | logout, logout-all, access-token, refresh-token | `refresh_token`: string, no declared constraints, functionally required |
| `UserOtpRequestDto` | `POST /api/auth/verify-otp` | `email`: string, no constraints · `otp`: string, no constraints |
| `UpdateBlogUserRequestDto` | `PUT /api/users/{userId}` | same fields/constraints as `SignupRequestDto` minus a description — **none enforced** |
| `BlockUserRequestDto` | `POST /api/block` | `user_id`: string, required, blank rejected (**enforced**) · `reason`: string, optional |
| `BlogPostRequestDto` | create/update blog post | `title`, `description`, `banner_image_url`, `content`: all string, no constraints declared or enforced |
| `CommentRequestDto` | add/reply comment | `content`, `post_id`, `parent_id`: all string, no constraints declared or enforced |
| `Map<String,Object>` (no class) | `PATCH /api/users/{userId}` | arbitrary — see that endpoint's docs; **keys must be Java field names, not snake_case** |
| `Map<String,String>` (no class) | add/remove like, get-users-who-liked | key `blog_post_id`: string |

¹ "Required" here means "the frontend should send it for the feature to work," not "the backend rejects its absence" — cross-reference §0.4 #3/#4 for what's actually enforced.

### 5.2 Response DTOs

**`BlogUserResponseDto`** (public-safe user projection — used by nearly every user-facing endpoint except `all-users-details`):
```json
{ "id": "string", "username": "string", "email": "string", "bio": "string|null",
  "profile_picture": "string (URL)", "roles": ["FREE_USER" /* | PREMIUM_USER | ADMIN_USER, array */] }
```
Note: still includes `email` for *any* viewer, including the public `/api/users/{username}` lookup — there is no field suppression based on viewer identity.

**`BlogPostResponseDto`** (used by list endpoints — post-by-username, liked-by-user):
```json
{ "blog_post_id": "string", "title": "string", "description": "string", "banner_image_url": "string",
  "content": "string", "premium": false, "created_by": "string (username)", "created_at": "ISO-8601 instant",
  "updated_by": "string (username)", "updated_at": "ISO-8601 instant" }
```

**Raw `BlogPost` document** (used by `GET /api/blogposts/post/{id}` only — different shape than the DTO above):
```json
{ "blog_post_id": "string", "title": "string", "description": "string", "banner_image_url": "string",
  "content": "string", "premium": false,
  "created_by": { "id": "string", "username": "string" }, "created_at": "ISO-8601 instant",
  "updated_by": { "id": "string", "username": "string" }, "updated_at": "ISO-8601 instant" }
```

**`TokenResponseDto`** (bare, single-wrapped — see §0.2):
```json
{ "user_id": "string", "access_token": "string (JWT)", "refresh_token": "string (JWT)" }
```

**`NotificationResponseDto`**:
```json
{ "id": "string", "type": "NEW_FOLLOWER|POST_LIKED|POST_COMMENTED|COMMENT_REPLIED|COMMENT_LIKED|MENTION_IN_POST|MENTION_IN_COMMENT",
  "actor": { "id": "string", "username": "string", "profile_picture": "string|null" },
  "target": { "id": "string", "type": "post|comment|user", "title": "string|null" },
  "message": "string (human-readable, pre-built server-side)",
  "preview": "string|null (truncated to 100 chars)",
  "read": false, "created_at": "ISO-8601 instant", "read_at": "ISO-8601 instant|null" }
```

**Raw `Comments` document** (returned by all comment endpoints):
```json
{ "id": "string", "post_id": "string", "user_id": "string", "parent_id": "string|null",
  "content": "string", "created_at": "ISO-8601 instant" }
```

**Raw `Likes` document** (returned by `POST /api/interactions/likes`):
```json
{ "id": "string", "post_id": "string", "user_id": "string", "created_at": "ISO-8601 instant" }
```

### 5.3 Shared envelope/wrapper types

**`ApiResponse<T>`** (outer envelope, §0.2) — always present on every response:
```json
{ "timestamp": "ISO-8601 local date-time (no explicit zone, server-local)", "data": "T|null", "error": "ApiError|null" }
```

**`ApiError`** (only produced by `GlobalExceptionHandler`, i.e. the un-wrapped error shape in §0.3):
```json
{ "status": "string (Spring HttpStatus enum name, e.g. \"NOT_FOUND\", \"INTERNAL_SERVER_ERROR\")",
  "message": "string", "subErrors": "array|null (always null in practice — nothing ever populates it)" }
```
Note `status` here is the **enum name** (`"NOT_FOUND"`), not the numeric code — different from every other status representation in this API, which uses the numeric `status_code`.

**`DataResponse`** (inner, success-with-payload — §0.2):
```json
{ "success": true, "status_code": 200, "message": "string", "data": "T" }
```

**`SuccessResponse`** (inner, success-no-payload):
```json
{ "success": true, "status_code": 200, "message": "string" }
```

**`ErrorResponse`** (inner, failure — note it still goes through the outer `ApiResponse` wrap since it's a `Response`, not an `ApiResponse`):
```json
{ "success": false, "status_code": 4xx, "message": "string", "error": "any|null" }
```

---

## 6. Error Contract

There is **no single, consistent error shape** in this API. Two independent mechanisms produce two different shapes, and which one you get depends entirely on whether the failure was raised as a Java exception that bubbled up to `GlobalExceptionHandler`, or was constructed manually inside a controller/service as a `ResponseEntity.status(x).body(new ErrorResponse(...))`.

### 6.1 Shape A — exception-handler errors (un-wrapped `ApiResponse`)
Only ever produced for: any uncaught exception anywhere in the app (→ 500), since `ResourceNotFoundException`'s dedicated 404 path is unreachable (never thrown — §0.4 #6).
```json
{ "timestamp": "2026-09-09T12:00:00.123",
  "data": null,
  "error": { "status": "INTERNAL_SERVER_ERROR", "message": "Invalid credentials", "subErrors": null } }
```
`error.message` is literally `e.getMessage()` from whatever exception was thrown — this can be a clean string (`"Invalid credentials"`, `"User not found"`, `"Invalid token"`) or, for framework exceptions like `MethodArgumentNotValidException`, a long multi-line Spring-generated validation dump. Do not assume it's always short/user-presentable.

### 6.2 Shape B — manual controller/service errors (double-wrapped, per §0.2)
```json
{ "timestamp": "2026-09-09T12:00:00.123",
  "data": { "success": false, "status_code": 404, "message": "Blog post not found", "error": null },
  "error": null }
```
The real status code and message are at `data.status_code` / `data.message`; the outer `error` is always `null` for this shape, and the inner `data.error` is usually also `null` (a couple of endpoints — profile picture upload/delete failures — populate `data.error` with an underlying exception message).

### 6.3 Status codes actually observed, by cause

| Status | Real 4xx/5xx meaning here | Where it comes from |
|---|---|---|
| 400 | A genuine client-input business-rule violation | Manual checks only: block-yourself, remove-already-removed-profile-picture, invalid-OTP-on-email-change |
| 401 | Either "not authenticated" (framework, Spring Boot's default error shape — §3.3, a third shape distinct from A/B) or, confusingly, **an ownership/authorization mismatch** (Shape B) — the two are indistinguishable by status code alone, only by body shape/content | `AccessTokenEntryPoint` (no/invalid token) vs. `BlogUserController.checkUserId` (self-service ownership checks) vs. `AuthService` (unverified account, bad OTP) |
| 403 | Either a `@PreAuthorize` role denial (framework, same default-error shape as the 401 case) or a manual ownership check (JSON, Shape B) | Spring Security vs. blog-post/comment/notification ownership checks |
| 404 | Resource genuinely not found | Manual checks only, in: block/unblock, get/update/delete blog post, add comment (bad `post_id`), mark/delete notification |
| 500 | **Catch-all for almost every other failure mode**, including many that look like they should be 400/401/403/404/409 — see §0.4 items 4–7 and the per-endpoint "Errors" tables above for the full list of scenarios that land here | Any unhandled exception (`BadCredentialsException`, `UsernameNotFoundException`, `NullPointerException` from skipped null-checks, `DuplicateKeyException` from double-likes, reflection errors from the field-map update endpoint, validation exceptions) |
| 204 | Empty-list "success," not an error | `GET /api/users/all` when there are zero users (body present despite 204 being technically bodyless — see that endpoint's notes) |

**422 Unprocessable Entity** is never used anywhere in this codebase. **409 Conflict** is never used either, even for the one case (`POST /api/interactions/likes` double-like) that maps naturally to it — that case is 500 instead (§4.6).

---

## 7. Pagination, filtering, sorting — summary

Only three endpoints paginate, and each does so with its own ad-hoc shape (no shared pagination DTO exists):

| Endpoint | Params | Sort | Response pagination shape |
|---|---|---|---|
| `GET /api/block` | `page` (default 0), `size` (default 20) | insertion order (no explicit sort) | `data.pagination = {page, size, totalElements, totalPages}` (camelCase keys, raw map) |
| `GET /api/notifications` | `page` (default 0), `size` (default 20), `unreadOnly` (default false, **not** `unread_only`) | fixed: `created_at` descending | same shape as above, plus sibling `data.unreadCount` |

No endpoint exposes a client-controllable `sort` parameter. `GET /api/interactions/comments`, `GET /api/blogposts/{username}`, `GET /api/interactions/likes/user/posts`, and `GET /api/users/all` return **complete, unpaginated** collections — be prepared for unbounded list sizes on those.

---

## 8. Special headers / content types

| Header/behavior | Applies to |
|---|---|
| `Authorization: Bearer <access_token>` | Every endpoint except `/api/auth/**`, `/api/users/{username}` (GET), and `/actuator/**` |
| `Content-Type: application/json` | All JSON-body endpoints (the vast majority) |
| `Content-Type: multipart/form-data` | `POST /api/users/{userId}/upload-profile-picture` only |
| No custom headers (API keys, tenant headers, idempotency keys, etc.) are read anywhere in the codebase | — |
| No `Set-Cookie` is ever issued | — |
| CORS headers reflect the (broken — §0.4 #1/#2) config in `SecurityConfig` | all cross-origin requests |

---

## 9. Existing Swagger/OpenAPI documentation

There is none to compare against. `pom.xml` has no `springdoc-openapi` or `swagger` dependency. `SecurityConfig` and `GlobalResponseHandler` both reference Swagger/OpenAPI-shaped paths (`/swagger-ui/**`, `/v2/api-docs`, `/v3/api-docs`), but nothing on the classpath actually serves those routes — they are inert. `docs/openapi.yaml` is not generated as part of this pass since the source code (per the task's own instruction to prefer it over any existing docs) shows no working OpenAPI generation to reconcile against, and hand-authoring a full spec risks drifting from this contract as the single source of truth. If an OpenAPI spec becomes a requirement, it should be generated from this document rather than the other way around.

---

## 10. Summary of things that look incomplete or suspicious (not fixed in this pass)

1. `POST /api/interactions/likes` — no duplicate-like guard before insert; relies on a Mongo unique index and lets the resulting `DuplicateKeyException` surface as an unhandled 500.
2. `DELETE /api/interactions/likes` — deletes *a* like on the post by `post_id` alone, not the caller's own like; the ownership `assert` is a no-op in production (§0.4 #7). Possible cross-user deletion bug.
3. `GET /api/users/all-users-details` — leaks raw `BlogUser` documents including bcrypt password hashes to `ADMIN_USER` callers.
4. `PATCH /api/users/{userId}` — unrestricted reflective field-set onto `BlogUser`, including `roles`, `status`, and a plaintext (non-BCrypt) `password` overwrite.
5. `PATCH /api/blogposts/{id}/premium` — role-gated (`PREMIUM_USER`) but not ownership-gated; any premium user can toggle any post's premium flag.
6. Auth failures (wrong password, duplicate signup, invalid/expired refresh token, user-not-found) consistently surface as HTTP 500 instead of 401/404, because `BadCredentialsException`/`UsernameNotFoundException` aren't registered in `GlobalExceptionHandler`.
7. `ResourceNotFoundException` + its handler exist but are dead code — never thrown.
8. `@Valid` is applied to only 2 of the ~10 DTOs that carry validation annotations; the rest are decorative.
9. CORS config combines `allowCredentials(true)` with `allowedOrigins("*")` (invalid) and omits `PATCH` from `allowedMethods` (breaks 4 real endpoints for browser clients).
10. `GET /api/users/health` almost certainly breaks under the global response-wrapping advice (§0.4 #9) — unverified by execution in this pass, flagged for manual confirmation.
11. `NEW_FOLLOWER`, `MENTION_IN_POST`, `MENTION_IN_COMMENT` notification types are fully modeled (enum, email template, message builder) but have no producing endpoint anywhere — there's no follow feature and no mention-detection in the exposed API.
12. Blocking's own class Javadoc claims broader enforcement ("prevents viewing/commenting/following") than what's actually implemented (only notification suppression).

---

**API_CONTRACT.md represents the API contract discovered from the current backend implementation.**
