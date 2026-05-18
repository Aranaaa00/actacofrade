# Changelog

All notable changes to ActaCofrade are collected in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

Upcoming improvements in progress. When a new version is released, whatever is here will be moved to its corresponding section.

---

## [1.0.0] — 2026-05-18

First public release of ActaCofrade. It coincides with the TFG submission and leaves the project in a state I consider stable and ready for anyone to run locally with a single command.

From this point on, any change that breaks the API, changes the database schema in a non-backwards-compatible way, or modifies required environment variables will increase the `MAJOR` version.

### Added

- Full application for brotherhoods and confraternities: event planning, task assignment, decision records, incident tracking, and an auditable history per edition.
- REST API built with Spring Boot 4.0 on Java 21, using JWT (HS256), role-based access control (`SUPER_ADMIN`, `ADMINISTRADOR`, `RESPONSABLE`, `COLABORADOR`, `CONSULTA`) and scoping by brotherhood.
- SPA in Angular 20 with standalone components, lazy loading on all routes, `@if/@for` control flow and signals.
- Schema managed with Flyway (V1…V21; V5 was reserved and is not reused).
- Reverse proxy with Nginx that serves the SPA and re-exposes `/api`, `/swagger-ui` and `/v3/api-docs`.
- Default security headers: strict `Content-Security-Policy`, HSTS, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`, COOP and CORP.
- Rate limiting on `/api/auth/` from Nginx, complementary to the backend rate limiter.
- Interactive API documentation in Swagger UI and a Postman collection ready to import in [`docs/postman/`](docs/postman/).
- TFG memory split by chapters in [`docs/`](docs/) and deployment guide in [`DEPLOY.md`](DEPLOY.md).
- Three GitHub Actions workflows: [`backend.yml`](.github/workflows/backend.yml), [`frontend.yml`](.github/workflows/frontend.yml) and [`cd.yml`](.github/workflows/cd.yml). The first two publish images to GHCR and scan each image with Trivy; the third also publishes to Docker Hub.
- Dependabot configuration for Maven, npm, Docker and GitHub Actions ([`dependabot.yml`](.github/dependabot.yml)).

### Security

- BCrypt with cost factor 12 for password hashing (compatible with old hashes when verifying).
- Fail-fast on startup if the `SUPERADMIN_*` variables are only partially set: either all three are provided, or none.
- Test user seeds (`SEED_TEST_USERS=true`) are blocked in code if the active profile is `prod` or `production`, even if the variable was left set by mistake.
- Defensive parsing of `CORS_ALLOWED_ORIGINS`: tolerates spaces and empty entries.
- Vulnerability scanning with Trivy (HIGH and CRITICAL) on every image built by CI.

### Changed

- `Content-Security-Policy` moved to Nginx with explicit values instead of inheriting from Cloudflare.
- `error_page` in Nginx now only applies to the SPA block: responses from `/api`, `/swagger-ui` and `/v3/api-docs` are no longer overwritten with `index.html`.
- The `act-card--dark` visual variant is only applied from tablet size and up: on mobile, all cards keep the light palette to avoid the vertical alternating band effect that added no useful information.

### Fixed

- Nested `<search>` element on the event listing page (invalid HTML) and the search bar `aria-label`.
- Full migration of `super-admin-users` templates from `*ngIf` / `*ngFor` / `ng-template` to `@if` / `@for` with explicit `track`.
- Documentation: Flyway migration count, Angular version and real backend coverage threshold in [`backend/README.md`](backend/README.md), [`frontend/README.md`](frontend/README.md), [`docs/05-diseno.md`](docs/05-diseno.md) and [`docs/06-desarrollo.md`](docs/06-desarrollo.md).

### Notes for people coming from a previous clone

- The new CSP is strict: if you add third-party scripts or styles, you will need to extend the directives in [`frontend/nginx.conf`](frontend/nginx.conf).
- The BCrypt factor went from 10 to 12. Old passwords still work, but the first login for each user will take a few extra milliseconds.
- Flyway V5 is intentionally empty. Do not fill it in: create a new migration with a higher number.

[Unreleased]: https://github.com/Aranaaa00/actacofrade/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Aranaaa00/actacofrade/releases/tag/v1.0.0
