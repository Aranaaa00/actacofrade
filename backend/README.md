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
