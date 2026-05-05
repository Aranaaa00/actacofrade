# ActaCofrade â€” Backend

Spring Boot REST API for **ActaCofrade**, a tool that helps cofradÃ­as (Catholic
brotherhoods) plan their events, follow up on the work of their members, and
keep an auditable history of every decision and incident that happens during
each act.

The API is the only entry point: the Angular frontend, Postman, Swagger UI and
any future client all talk to it through HTTP over JSON. Authentication is
stateless (JWT) and every operation is scoped to the hermandad of the user
that sends the request.

---

## 1. Tech stack

| Area | Choice |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 4.0 (Web MVC, Data JPA, Security, Validation) |
| Database | PostgreSQL 15+ |
| Migrations | Flyway (V1 â€¦ V16) |
| Auth | JWT (jjwt 0.12, HS256) |
| API docs | OpenAPI 3 + Swagger UI (springdoc 2.8) |
| PDF export | OpenPDF 1.3 |
| Tests | JUnit 5, Mockito, Spring Test, MockMvc |
| Coverage | JaCoCo (â‰¥ 85 % line coverage required to pass `verify`) |
| Build | Maven (`./mvnw`) |

---

## 2. Project structure

```
backend/
â”œâ”€â”€ pom.xml
â”œâ”€â”€ Dockerfile
â”œâ”€â”€ mvnw, mvnw.cmd
â””â”€â”€ src/
    â”œâ”€â”€ main/
    â”‚   â”œâ”€â”€ java/com/actacofrade/backend/
    â”‚   â”‚   â”œâ”€â”€ BackendApplication.java     â€” Spring Boot entry point
    â”‚   â”‚   â”œâ”€â”€ config/                     â€” OpenAPI config, super-admin bootstrap
    â”‚   â”‚   â”œâ”€â”€ controller/                 â€” REST endpoints (no business logic)
    â”‚   â”‚   â”œâ”€â”€ dto/                        â€” Request / response records
    â”‚   â”‚   â”œâ”€â”€ entity/                     â€” JPA entities and enums
    â”‚   â”‚   â”œâ”€â”€ exception/                  â€” GlobalExceptionHandler
    â”‚   â”‚   â”œâ”€â”€ repository/                 â€” Spring Data repositories + Specifications
    â”‚   â”‚   â”œâ”€â”€ security/                   â€” SecurityConfig, JwtService, JwtAuthenticationFilter,
    â”‚   â”‚   â”‚                                 LoginRateLimiter, CustomUserDetailsService
    â”‚   â”‚   â”œâ”€â”€ service/                    â€” Business logic, transactions, hermandad scoping
    â”‚   â”‚   â””â”€â”€ util/                       â€” SanitizationUtils, AuthorizationHelper
    â”‚   â””â”€â”€ resources/
    â”‚       â”œâ”€â”€ application.properties
    â”‚       â””â”€â”€ db/migration/               â€” Flyway scripts V1__â€¦V16__
    â””â”€â”€ test/
        â””â”€â”€ java/com/actacofrade/backend/
            â”œâ”€â”€ controller/                 â€” MockMvc integration tests
            â”œâ”€â”€ service/                    â€” Mockito unit tests
            â”œâ”€â”€ security/                   â€” JWT, filter, rate limiter, user details
            â”œâ”€â”€ util/                       â€” Sanitization, authorization helpers
            â””â”€â”€ support/                    â€” Shared test fixtures
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
* Secret: `JWT_SECRET` (mandatory, â‰¥ 32 bytes / 256 bits, validated on
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

Successful responses return the resource directly, or a Spring `Page<â€¦>`
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
| **Admin change requests** | `POST /api/admin-change-requests`, `GET /api/admin-change-requests`, `GET /api/admin-change-requests/{id}`, `GET /api/admin-change-requests/{id}/candidates`, `PATCH .../approve`, `PATCH .../reject` | Create: any non super-admin user. List / resolve: SUPER_ADMIN. |

### Task state machine

```
PLANNED â†’ ACCEPTED â†’ IN_PREPARATION â†’ CONFIRMED â†’ COMPLETED
   â”‚           â”‚
   â”‚           â””â”€â”€ REJECTED
   â””â”€â”€ reset to PLANNED
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
| Swagger UI | <http://localhost:8080/swagger-ui.html> | `https://<your-domain>/swagger-ui.html` |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> | `https://<your-domain>/v3/api-docs` |

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

* `Hermandad` 1â€”N `User`, 1â€”N `Event`, 1â€”N `AdminChangeRequest`.
* `User` Nâ€”M `Role` through `user_roles`.
* `User` 1â€”1 `UserAvatar` (optional binary avatar).
* `Event` Nâ€”1 `User` (responsible) and 1â€”N `Task`, `Decision`, `Incident`,
  `AuditLog`.
* `Task` Nâ€”1 `User` for `assignedTo`, `createdBy` and `confirmedBy`.
* `Incident` Nâ€”1 `User` for `reportedBy` and `resolvedBy`.
* `Decision` Nâ€”1 `User` for `reviewedBy`.

Every primary key is an auto-increment `INTEGER`. Status fields use native
PostgreSQL enums (`role_code`, `event_status`, `event_type`, `task_status`,
`decision_status`, `incident_status`, `hermandad_area`,
`admin_change_request_status`). Performance indexes exist on the most common
foreign keys (`hermandad_id`, `event_id`, `assigned_to`, `responsible_id`).

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
| `DB_USER` / `DB_PASSWORD` | yes | â€” | Database credentials. |
| `JWT_SECRET` | **yes** | â€” | Signing key, â‰¥ 32 bytes. The application refuses to start otherwise. |
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

### Option A â€” Docker Compose (recommended)

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

### Option B â€” Maven, locally

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
JaCoCo line-coverage rule (â‰¥ 85 % on `service`, `controller`, `security` and
`util`). The HTML coverage report is written to
`target/site/jacoco/index.html`.

To run only one test class:

```bash
./mvnw test -Dtest=EventServiceTest
```

---

## 9. Quick health check

After `docker compose up -d --build`:

* `GET http://localhost:8080/v3/api-docs` â†’ returns the OpenAPI JSON.
* `GET http://localhost:8080/swagger-ui.html` â†’ loads Swagger UI.
* `POST http://localhost:8080/api/auth/login` with valid credentials â†’
  returns a JWT.

The same paths also work through the frontend's reverse proxy on port `80`
(`http://localhost/swagger-ui.html`, `http://localhost/api/auth/login`,
â€¦), which is the configuration used in production.

If any of those calls fails, check the container logs:

```bash
docker compose logs -f backend
```
