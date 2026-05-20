# ActaCofrade Backend

Spring Boot REST API for **ActaCofrade**, a tool that helps cofradías (Catholic
brotherhoods) plan their events, follow up on the work of their members, and
keep an auditable history of every decision and incident that happens during
each act.

The API is the only entry point: the Angular frontend, Postman, Swagger UI and
any future client all talk to it through HTTP over JSON. Authentication is
stateless (JWT) and every operation is scoped to the hermandad of the user
that sends the request.

---

## Index

- [1. Tech stack](#1-tech-stack)
- [2. Project structure](#2-project-structure)
- [3. Authentication and roles](#3-authentication-and-roles)
- [4. API overview](#4-api-overview)
- [5. Interactive documentation (Swagger UI)](#5-interactive-documentation-swagger-ui)
- [6. Database model](#6-database-model)
- [7. Configuration (environment variables)](#7-configuration-environment-variables)
- [8. How to run the backend](#8-how-to-run-the-backend)
- [9. Quick health check](#9-quick-health-check)

---

## 1. Tech stack

| Area | Choice |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 4.0 (Web MVC, Data JPA, Security, Validation) |
| Database | PostgreSQL 15+ |
| Migrations | Flyway (V1 … V21, V5 reservado — ver nota en `V6__add_hermandad.sql`) |
| Auth | JWT (jjwt 0.12, HS256) |
| API docs | OpenAPI 3 + Swagger UI (springdoc 2.8) |
| PDF export | OpenPDF 1.3 |
| Tests | JUnit 5, Mockito, Spring Test, MockMvc |
| Coverage | JaCoCo (≥ 80 % line coverage required to pass `verify`) |
| Build | Maven (`./mvnw`) |

---

## 2. Project structure

```
backend/
├── pom.xml
├── Dockerfile
├── mvnw, mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/actacofrade/backend/
    │   │   ├── BackendApplication.java     — Spring Boot entry point
    │   │   ├── config/                     — OpenAPI config, super-admin bootstrap
    │   │   ├── controller/                 — REST endpoints (no business logic)
    │   │   ├── dto/                        — Request / response records
    │   │   ├── entity/                     — JPA entities and enums
    │   │   ├── exception/                  — GlobalExceptionHandler
    │   │   ├── repository/                 — Spring Data repositories + Specifications
    │   │   ├── security/                   — SecurityConfig, JwtService, JwtAuthenticationFilter,
    │   │   │                                 LoginRateLimiter, CustomUserDetailsService
    │   │   ├── service/                    — Business logic, transactions, hermandad scoping
    │   │   └── util/                       — SanitizationUtils, AuthorizationHelper
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/               — Flyway scripts V1__…V21__ (V5 reserved)
    └── test/
        └── java/com/actacofrade/backend/
            ├── controller/                 — MockMvc integration tests
            ├── service/                    — Mockito unit tests
            ├── security/                   — JWT, filter, rate limiter, user details
            ├── util/                       — Sanitization, authorization helpers
            └── support/                    — Shared test fixtures
```

The architecture is a strict layered MVC:

* **Controllers** validate input (`@Valid`), call a service and return a
  `ResponseEntity` containing a DTO. They never touch repositories and never
  hold business rules.
* **Services** own the business logic, transactions (`@Transactional`),
  input sanitization and hermandad scoping. They map entities to DTOs.
* **Repositories** are Spring Data interfaces. Complex filtering uses
  `JpaSpecificationExecutor` (for example `EventSpecification`) so queries
  stay composable and SQL-injection safe.
* **Entities** are an internal concern: controllers always return DTOs, never
  JPA entities.

---

## 3. Authentication and roles

### How the auth flow works

1. The client sends `POST /api/auth/login` with email and password.
2. The server validates the credentials, updates `last_login` and returns a
   signed JWT plus the user profile.
3. Every protected request must include `Authorization: Bearer <token>`.
4. `JwtAuthenticationFilter` parses the token, loads the user and stores the
   `Authentication` in the `SecurityContext`.
5. `@PreAuthorize` on each endpoint checks the role. Service methods then
   apply hermandad scoping, so a user can only see and modify resources that
   belong to their own brotherhood.

### Roles

| Code | Purpose |
|---|---|
| `SUPER_ADMIN` | Platform owner. Resolves admin-change requests across hermandades. |
| `ADMINISTRADOR` | Full control inside one hermandad (users, events, configuration). |
| `RESPONSABLE` | Manages events, tasks, decisions and incidents. |
| `COLABORADOR` | Works on the tasks they are assigned and reports incidents. |
| `CONSULTA` | Read-only access. |

### Brute-force protection

`LoginRateLimiter` blocks repeated failed logins per `IP + email`. Defaults:
`5` attempts per `300` s window, then a `900` s lock. All three values are
configurable through environment variables.

### JWT

* Algorithm: `HS256`.
* Secret: `JWT_SECRET` (mandatory, ≥ 32 bytes / 256 bits, validated on
  startup).
* Expiration: `JWT_EXPIRATION_MS` (default `86400000` = 24 h).

---

## 4. API overview

All endpoints live under `/api`. The error envelope is consistent across the
whole API and is produced by `GlobalExceptionHandler`:

```json
{
  "status": "error",
  "message": "Human-readable message",
  "data": null,
  "errors": []
}
```

Successful responses return the resource directly, or a Spring `Page<…>`
object for paginated endpoints. HTTP statuses used: `200`, `201`, `204`,
`400`, `401`, `403`, `404`, `409`, `413`, `500`.

| Area | Endpoints | Access |
|---|---|---|
| **Auth** | `POST /api/auth/register`, `POST /api/auth/login` | Public. Login is rate-limited. |
| **Profile** | `GET /api/me`, `PUT /api/me`, `PATCH /api/me/password`, `POST /api/me/avatar`, `DELETE /api/me/avatar`, `GET /api/me/avatar/{userId}` | Any authenticated user. |
| **Users** | `GET/POST/PUT/DELETE /api/users`, `PATCH /api/users/{id}/toggle-active`, `GET /api/users/stats`, `GET /api/users/assignable` | Admin (assignable: admin / responsable). |
| **Roles** | `GET /api/roles` | Admin / responsable / colaborador. |
| **Hermandad** | `GET /api/hermandades/me`, `PUT /api/hermandades/me` | Read: any role. Update: admin only. |
| **Events** | `GET/POST/PUT/DELETE /api/events`, `GET /api/events/filter`, `GET /api/events/history`, `GET /api/events/available-dates`, `GET /api/events/{id}` | Read: authenticated. Create/update: admin / responsable. Delete: admin. |
| **Tasks** | `GET/POST/PUT/DELETE /api/events/{eventId}/tasks/...` plus `accept`, `start-preparation`, `confirm`, `complete`, `reject`, `reset` (PATCH) | Role depends on the transition. The state machine is enforced server-side. |
| **Decisions** | `GET/POST/PUT/DELETE /api/events/{eventId}/decisions/...`, `PATCH .../accept` | Create: admin / responsable / colaborador. Update / delete / accept: admin / responsable. |
| **Incidents** | `GET/POST/DELETE /api/events/{eventId}/incidents/...`, `PATCH .../resolve`, `.../reopen` | Create: admin / responsable / colaborador. Resolve / reopen / delete: admin / responsable. |
| **My tasks** | `GET /api/my-tasks`, `GET /api/my-tasks/stats` | Tasks assigned to the current user. |
| **Audit log** | `GET /api/events/{eventId}/history` | Authenticated. Paginated. |
| **Dashboard** | `GET /api/dashboard` | Authenticated. Aggregated statistics. |
| **Export** | `POST /api/events/{id}/export` (PDF or CSV) | Admin / responsable / colaborador. |
| **Admin change requests** | `POST /api/admin-change-requests`, `GET /api/admin-change-requests`, `GET /api/admin-change-requests/{id}`, `GET /api/admin-change-requests/{id}/candidates`, `PATCH .../approve`, `PATCH .../reject`, `PATCH .../resolve` | Create: any authenticated user except SUPER_ADMIN. List / detail / candidates / approve / reject / resolve: SUPER_ADMIN. Requests carry a `type` field that splits them into admin-handover requests and support-center categories (questions, suggestions, incidents…). Approve transfers the admin role for handover requests; resolve closes informational categories that don't need approval. |
| **SuperAdmin users** | `GET /api/super-admin/users` (paginated search by name/email), `GET /api/super-admin/users/{id}`, `PATCH .../status` (activate / suspend with optional reason), `PATCH .../role`, `POST .../verify`, `POST .../unverify`, `POST .../password-reset`, `GET /api/super-admin/users/{id}/logs`, `GET /api/super-admin/users/logs` | SUPER_ADMIN only. Every action is written to the intervention log so it can be audited from the same controller. |

### Manual verification (trust marker)

The SuperAdmin can toggle a manual verification mark on any account from the intervention center. This flag is **purely cosmetic / trust metadata**: it never grants extra roles, never changes RBAC, never bypasses guards. It is exposed read-only in `UserResponse`, `AuthResponse`, `EventResponse`, `TaskResponse`, `DecisionResponse` and `IncidentResponse` so that the frontend can render the badge consistently next to user names.

| Method | Endpoint | Body | Returns | Role | Notes |
|---|---|---|---|---|---|
| POST | `/api/super-admin/users/{id}/verify` | — (no body) | `SuperAdminUserResponse` | SUPER_ADMIN | Idempotent. Sets `manuallyVerified = true`. Audit-logged. |
| POST | `/api/super-admin/users/{id}/unverify` | — (no body) | `SuperAdminUserResponse` | SUPER_ADMIN | Idempotent. Sets `manuallyVerified = false`. Audit-logged. |

### Task state machine

```
PLANNED → ACCEPTED → IN_PREPARATION → CONFIRMED → COMPLETED
   │           │
   │           └── REJECTED
   └── reset to PLANNED
```

Each transition has its own PATCH endpoint and is validated by the service
layer; trying an illegal transition returns `409 Conflict`.

---

## 5. Interactive documentation (Swagger UI)

The API ships with a fully interactive Swagger UI generated by springdoc.
Both Swagger UI and the raw OpenAPI document follow the host the server is
running on, so the same paths work locally and on any deployed environment.

| Resource | Local development | Deployed website |
|---|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui.html> | `https://actacofrade.com/swagger-ui.html` |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> | `https://actacofrade.com/v3/api-docs` |

With Docker Compose the backend container publishes port `8080` on
`127.0.0.1`, so the URLs above work as soon as `docker compose up` finishes,
without any extra setup. The same paths are also reachable through the
frontend's Nginx reverse proxy (`http://localhost/swagger-ui.html`,
`http://localhost/v3/api-docs`), which is what the deployed website uses.

The OpenAPI document declares both `http://localhost:8080` and `/` (same
origin) as servers, so Swagger UI picks the right base URL automatically
whether you open it locally or through the public domain.

### How to call protected endpoints from Swagger

1. Open `POST /api/auth/login`, click **Try it out** and send your
   credentials.
2. Copy the `token` from the response.
3. Click the green **Authorize** button at the top of the page and paste the
   token. Swagger automatically prefixes it with `Bearer `.
4. Every subsequent request is sent with the proper `Authorization` header.

The OpenAPI document already declares the `bearerAuth` security scheme and
example payloads for the most common operations, so newcomers can try the
API without reading the source code.

---

## 6. Database model

Main relationships:

* `Hermandad` 1—N `User`, 1—N `Event`, 1—N `AdminChangeRequest`.
* `User` N—M `Role` through `user_roles`.
* `User` 1—1 `UserAvatar` (optional binary avatar).
* `Event` N—1 `User` (responsible) and 1—N `Task`, `Decision`, `Incident`,
  `AuditLog`.
* `Task` N—1 `User` for `assignedTo`, `createdBy` and `confirmedBy`.
* `Incident` N—1 `User` for `reportedBy` and `resolvedBy`.
* `Decision` N—1 `User` for `reviewedBy`.
* `AdminChangeRequest` N—1 `Hermandad`, N—1 `User` (requester), and optionally
  N—1 `User` for the proposed new admin and for the SUPER_ADMIN that
  resolves it. The `type` column distinguishes the original admin-handover
  flow from the support-center categories that reuse the same table.

Every primary key is an auto-increment `INTEGER`. Status fields use native
PostgreSQL enums (`role_code`, `event_status`, `event_type`, `task_status`,
`decision_status`, `incident_status`, `hermandad_area`,
`admin_change_request_status`). Performance indexes exist on the most common
foreign keys (`hermandad_id`, `event_id`, `assigned_to`, `responsible_id`)
and on `admin_change_requests.status`, `.hermandad_id` and `.type` so the
SuperAdmin queues open instantly even with many historical rows.

The intervention log surfaced through `/api/super-admin/users/.../logs` is
built on top of the existing `audit_log` table by filtering the rows whose
actor holds the SUPER_ADMIN role — no extra table is required, which keeps
the audit trail centralised.

The schema is owned exclusively by Flyway: `spring.jpa.hibernate.ddl-auto`
is set to `validate`, so Hibernate never touches the schema at runtime.
Migrations live in `src/main/resources/db/migration`.

---

## 7. Configuration (environment variables)

The defaults are tuned for local development with Docker Compose. In
production every value with a `${VAR}` placeholder must come from the
environment.

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_URL` | no | `jdbc:postgresql://localhost:5432/actacofrade` | JDBC URL for PostgreSQL. |
| `DB_USER` / `DB_PASSWORD` | yes | — | Database credentials. |
| `JWT_SECRET` | **yes** | — | Signing key, ≥ 32 bytes. The application refuses to start otherwise. |
| `JWT_EXPIRATION_MS` | no | `86400000` | Token lifetime in milliseconds. |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:4200` | Comma-separated origins allowed by CORS. Add the deployed frontend origin here. |
| `AVATAR_MAX_SIZE` | no | `2MB` | Multipart limit. |
| `AVATAR_MAX_BYTES` | no | `2097152` | Hard byte limit checked in the service. |
| `AVATAR_ALLOWED_TYPES` | no | `image/png,image/jpeg,image/webp,image/gif` | Allowed MIME types for avatars. |
| `SUPERADMIN_EMAIL` / `SUPERADMIN_PASSWORD` / `SUPERADMIN_FULL_NAME` | no | empty | If all three are set, a `SUPER_ADMIN` user is created on first startup. |

Multipart upload limits are also surfaced through
`spring.servlet.multipart.max-file-size` and
`spring.servlet.multipart.max-request-size`, both bound to `AVATAR_MAX_SIZE`.

---

## 8. How to run the backend

### Option A — Docker Compose (recommended)

From the **repository root**:

```bash
cp .env.example .env       # fill in DB_PASSWORD, JWT_SECRET, etc.
docker compose up -d --build
```

This starts PostgreSQL on port `5432`, the backend on port `8080` (bound to
`127.0.0.1`, so it is not exposed publicly) and the frontend on port `80`.
Flyway runs every migration automatically on startup. The frontend
container also runs an Nginx reverse proxy that exposes the API and the
interactive documentation under the same origin as the website, so the
deployed site serves Swagger UI without any extra configuration.

### Option B — Maven, locally

```bash
cd backend
./mvnw spring-boot:run
```

A reachable PostgreSQL instance and the environment variables listed above
are required.

### Build and test

```bash
cd backend
./mvnw clean verify
```

`verify` compiles, runs every unit and integration test, and enforces the
JaCoCo line-coverage rule (≥ 80 % on `service`, `controller`, `security` and
`util`). The HTML coverage report is written to
`target/site/jacoco/index.html`.

To run only one test class:

```bash
./mvnw test -Dtest=EventServiceTest
```

---

## 9. Quick health check

After `docker compose up -d --build`:

* `GET http://localhost:8080/v3/api-docs` → returns the OpenAPI JSON.
* `GET http://localhost:8080/swagger-ui.html` → loads Swagger UI.
* `POST http://localhost:8080/api/auth/login` with valid credentials →
  returns a JWT.

The same paths also work through the frontend's reverse proxy on port `80`
(`http://localhost/swagger-ui.html`, `http://localhost/api/auth/login`,
…), which is the configuration used in production.

If any of those calls fails, check the container logs:

```bash
docker compose logs -f backend
```
