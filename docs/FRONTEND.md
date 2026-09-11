# Frontend

The React + TypeScript SPA in `frontend/`, built by Maven and served from the same JAR as the API.

- **Stack:** React 18, TypeScript (strict), Vite, React Router v6, TanStack Query v5, Zustand, Tailwind + shadcn/ui, Axios, React Hook Form + Zod, Vitest.
- **Contract:** [`API_CONTRACT.md`](API_CONTRACT.md) — read §0 before touching anything that talks to the API. The corrections in [Backend quirks](#backend-quirks-that-shape-this-code) below take precedence where the two disagree.

---

## Development setup

```bash
./run-local.sh --dev     # backend on :8080, Vite on :5173, hot reload for both
```

Or separately:

```bash
mvn spring-boot:run                 # backend on :8080
cd frontend && npm install && npm run dev    # Vite on :5173
```

Open **http://localhost:5173**, not :8080 — the dev server proxies `/api` through to the backend.

### The dev proxy is not optional

`vite.config.ts` proxies `/api` → `http://localhost:8080`. Two defects in `SecurityConfig.corsConfigurationSource()` make any genuinely cross-origin call fail:

1. `allowedOrigins("*")` is combined with `allowCredentials(true)`. Spring has rejected that combination since 5.3 — it throws at request-processing time rather than falling back to permissive behaviour.
2. `allowedMethods` omits `PATCH`, so browser preflights fail for all four PATCH endpoints (notification read / read-all, user partial update, post premium toggle).

The proxy makes dev same-origin and the packaged JAR makes production same-origin, so neither defect is reachable. Don't "fix" this by pointing the client at an absolute `http://localhost:8080` base URL.

---

## Maven integration

`frontend-maven-plugin` (see `pom.xml`) runs three executions during `mvn package`:

1. `install-node-and-npm` — downloads a project-local Node into `frontend/node/` (pinned by `node.version` / `npm.version` properties; nothing machine-wide is touched or required)
2. `npm ci` — reproducible install from `frontend/package-lock.json`
3. `npm run build` — Vite emits straight into `src/main/resources/static/`, so the SPA is packaged inside the JAR

```bash
mvn clean package                         # backend + frontend
mvn clean package -DskipFrontend=true     # backend only
```

`src/main/resources/static/` is generated output. Never hand-edit it, and never commit anything there but `.gitkeep` (a small Vite plugin rewrites that file after each build, since `emptyOutDir` would otherwise delete it).

---

## Routing

`SpaFallbackController` forwards deep links to `index.html`, and it does so by matching a fixed set of patterns: any single extension-less segment, plus `/u/**`, `/posts/**` and `/admin/**`.

**Adding a nested route outside those prefixes means editing that controller too** — otherwise the route works via client-side navigation but 404s on refresh or on a shared link.

---

## Architecture

```
src/
├── app/          App.tsx (routes), providers.tsx
├── features/     auth, home, explore, blogs, profile, notifications
│   └── <feature>/{api,hooks,components}
└── shared/
    ├── lib/      http.ts, api-response.ts, auth-bridge.ts, queryClient.ts
    ├── types/    api.ts (envelopes), models.ts (domain)
    ├── utils/    errorHandler.ts, validators.ts, formatting.ts
    └── components/ui/   shadcn primitives
```

`shared/` never imports from `features/`. The HTTP client needs the current token, so the auth store registers itself through `shared/lib/auth-bridge.ts` rather than being imported directly.

---

## Response handling

Everything below lives in `shared/lib/api-response.ts` and `shared/lib/http.ts`. Read those two files before writing any new API call.

### Two nesting depths

`GlobalResponseHandler` wraps every response in `{timestamp, data, error}`. Most controllers already return a `DataResponse`, so the payload ends up **double-wrapped** at `data.data`. Three endpoints (`/auth/access-token`, `/auth/refresh-token`, `/users/health`) return a bare DTO and are **single-wrapped** at `data`.

`unwrapResponse()` handles both. The response interceptor calls it once, so feature code reads a plain payload and never sees an envelope.

### Three error shapes

| Shape | Looks like | `ApiError.kind` | Produced by |
|---|---|---|---|
| **A** | outer `error` is non-null | `exception` | Any unhandled exception → always HTTP 500 |
| **B** | `data.success === false` | `business` | Hand-built `ErrorResponse` → 400/401/403/404 |
| **C** | `data.status` is a number, `data.path` present | `unauthenticated` | Missing/expired token via `AccessTokenEntryPoint` → 401 |

The discrimination order matters and is load-bearing: Shape B bodies also carry a numeric status code, so testing `success === false` must come before the Shape C test. Getting that backwards turns an ownership failure into a perceived token expiry and sends the client into a refresh loop. There is a test pinning exactly this.

**Branch on `kind`, never on HTTP status.** In this API a wrong password is a 500, an ownership violation is a 401, and a role denial is a 500.

### Token refresh

- Only a **Shape C** 401 triggers a refresh. A Shape B 401 is a business outcome (unverified account, ownership mismatch) and refreshing on it would loop forever.
- Refresh goes through `/auth/access-token` (non-rotating), not `/auth/refresh-token`. Rotation deletes the old refresh-token row server-side, so two concurrent rotations would invalidate each other.
- A single in-flight promise is shared, so a burst of parallel 401s causes exactly one refresh.
- A dead refresh token comes back as a **500 Shape A carrying "Invalid token"**, not a 401 — so the hard-logout path keys off the refresh call failing, not off a status code.

### Field casing

There is no blanket transform, and adding one would break things. Three conventions coexist, sometimes inside a single response body:

- snake_case for DTO fields (Jackson's global strategy)
- **no `is_` prefix on booleans** — `premium`, `read`, `verified`, because `isPremium()` serialises as `premium`
- camelCase for anything built with `Map.of(...)` — `unreadCount`, `totalElements`, `totalPages`, `isBlocked`
- camelCase **Java field names** in the `PATCH /users/{id}` request body, which reflects over raw map keys
- query params bind by Java parameter name: `?unreadOnly=true`, but `?is_premium=true` (that one has an explicit `@RequestParam(name=…)`)

Types in `shared/types/models.ts` are hand-written to match the exact wire keys.

---

## Backend quirks that shape this code

Verified against a running instance. Where these contradict `API_CONTRACT.md`, these are correct.

- **`@PreAuthorize` denials return 500, not 403.** No `AccessDeniedHandler` is registered, so the denial is thrown inside the handler and caught by `@ExceptionHandler(Exception.class)`. Shape C is 401-only.
- **List endpoints can answer `null` instead of `[]`.** `toArray()` in `features/blogs/api/blogsApi.ts` normalises this at the boundary.
- **Validation is almost entirely absent server-side.** `@Valid` is wired on two endpoints in the whole API, and when it does fire it produces a 500 with a Spring dump rather than a 400. All real validation is client-side, in `shared/utils/validators.ts`.
- **Signup collapses every failure into `"Invalid credentials"`** — a duplicate username, a duplicate email and a database outage are indistinguishable. The signup error copy hedges accordingly.
- **`POST /blogposts` returns no ID.** The editor re-lists the author's posts to discover what it just created.
- **`PUT /users/{id}` overwrites all four fields and re-hashes the password every time**, so a bio edit through it would require re-sending the user's plaintext password. Profile edits use `PATCH`. Username and email are therefore not editable in the UI.
- **Avatar uploads overwrite a fixed Cloudinary public ID**, so the URL is stable and the browser serves the stale image — `bustCache()` appends a cache-busting parameter.
- **Likes have no unique index and no "did I like this" query.** Like state is derived from `/interactions/likes/user/posts` and the button is never optimistic. `DELETE /interactions/likes` takes a JSON body (`axios.delete(url, { data })`) and deletes *a* like on the post rather than necessarily the caller's own.
- **Notifications are eventually consistent.** Likes and comments publish to Kafka fire-and-forget and the consumer swallows failures with no DLQ, so a notification may arrive late or never. Never insert one optimistically, and never assert on one in a test.
- **Not built, because no endpoint exists:** follow/unfollow, tags, bookmarks, drafts, password reset, and "who liked this post" (that endpoint requires a body on a GET, which browsers forbid).

### Fixed rather than worked around

Two backend defects had no client-side workaround and were fixed in place:

- `CommentsRepository.findCommentsByPostId` was declared `Object`, so Spring Data treated it as a single-result query: zero comments returned `null`, one returned a bare object, and **two or more threw `IncorrectResultSizeDataAccessException` → 500**, making the endpoint unusable on any real conversation. It now returns `List<Comments>` with an explicit sort.
- There was no way to resolve a comment's author: comments store only `user_id`, `/users/id/{id}` is self-only, and `/users/{username}` takes a username. `GET /users/lookup?ids=…` was added, returning an email-free `PublicUserDto`.

---

## Testing

```bash
cd frontend && npm test
```

The suite concentrates on the load-bearing parts — the unwrapper across all three error shapes and both nesting depths, the Shape-B-before-Shape-C ordering trap, single-flight refresh under concurrent 401s, the no-refresh-on-Shape-B-401 rule, and the hard-logout path. Don't test backend behaviour; test that this client survives it.
