# ActaCofrade

ActaCofrade is a web application for Catholic brotherhoods (*cofradías*).
It helps them organise their public events: planning the program, sharing
the work between members, recording the decisions taken in each meeting
and keeping a clear history of every incident that happens.

The project has two parts:

* **Backend** — a Spring Boot REST API (Java 21) that owns the business
  rules, the database and the authentication.
* **Frontend** — an Angular Single Page Application served by Nginx, which
  also acts as the reverse proxy of the API.

The whole stack runs with a single command:

```bash
docker compose up -d
```

Full step-by-step deployment instructions, environment variables and
verification commands live in [DEPLOY.md](DEPLOY.md). Detailed
documentation of the API itself is in
[backend/README.md](backend/README.md).

---

## Architecture

Three services share a private Docker network. Only the frontend is
visible from the host. The backend and the database are reachable only
from inside that network.

```text
   Browser ──► localhost:80
                    │
                    ▼
            ┌──────────────────────┐
            │  frontend  (Nginx)   │
            │  · Angular SPA       │
            │  · /api/* → backend  │
            │  · /swagger-ui*      │
            │  · /v3/api-docs      │
            └─────────┬────────────┘
                      │  internal network
                      ▼
            ┌──────────────────────┐
            │  backend  (Spring)   │
            │  · REST API          │
            │  · JWT auth          │
            │  · Flyway migrations │
            └─────────┬────────────┘
                      │  internal network
                      ▼
            ┌──────────────────────┐
            │  db  (PostgreSQL 15) │
            │  · volume: pg_data   │
            └──────────────────────┘
```

| Service    | Image                        | Internal port | Published on host |
|------------|------------------------------|---------------|-------------------|
| `frontend` | built locally (`nginx:alpine`) | 80            | **80**            |
| `backend`  | built locally (Temurin JRE 21) | 8080          | no                |
| `db`       | `postgres:15-alpine`           | 5432          | no                |

How they talk to each other:

* The browser only calls the frontend on `http://localhost`.
* Nginx forwards `/api/*`, `/swagger-ui*` and `/v3/api-docs*` to
  `backend:8080`.
* The backend connects to the database with the URL
  `jdbc:postgresql://db:5432/${POSTGRES_DB}`.
* Names like `backend` and `db` are resolved by the Docker DNS of the
  `actacofrade_network` bridge.

---

## Project layout

```
.
├── backend/                Spring Boot REST API (see backend/README.md)
│   ├── src/                Java code and tests
│   ├── Dockerfile          Multi-stage build (Maven → Temurin JRE)
│   └── pom.xml
├── frontend/               Angular SPA + Nginx reverse proxy
│   ├── src/                Angular code
│   ├── nginx.conf          Reverse proxy + static config
│   └── Dockerfile          Multi-stage build (Node → Nginx)
├── docs/                   Functional docs and Postman collection
├── docker-compose.yml      Stack definition (db + backend + frontend)
├── .env.example            Template for the .env file
├── DEPLOY.md               Full deployment guide
└── .github/workflows/      CI pipelines (backend.yml, frontend.yml)
```

---

## Quick start

```bash
git clone https://github.com/Aranaaa00/actacofrade.git
cd actacofrade
cp .env.example .env
# Edit .env and set strong values (see DEPLOY.md)
docker compose up -d --build
docker compose ps
```

Once every service shows `(healthy)`:

| URL                                       | What you get                  |
|-------------------------------------------|-------------------------------|
| `http://localhost/`                       | Angular SPA (login screen)    |
| `http://localhost/api/...`                | REST API                      |
| `http://localhost/swagger-ui.html`        | Interactive API documentation |
| `http://localhost/v3/api-docs`            | Raw OpenAPI document          |

---

## API in short

* Base path: `/api`.
* Authentication: `Authorization: Bearer <token>`, obtained from
  `POST /api/auth/login`.
* Roles: `SUPER_ADMIN`, `ADMINISTRADOR`, `RESPONSABLE`, `COLABORADOR`,
  `CONSULTA`.
* Main resources: `auth`, `me`, `users`, `roles`, `hermandades`,
  `events`, `tasks`, `decisions`, `incidents`, `audit`, `dashboard`,
  `admin-change-requests`.

The full endpoint list, the task state machine and the error envelope
are documented in [backend/README.md](backend/README.md).

The API is also documented live in Swagger UI at
`http://localhost/swagger-ui.html` and as raw JSON at
`http://localhost/v3/api-docs`.

---

## Continuous integration

The repository has two GitHub Actions workflows. Each one runs only when
its part of the project changes:

* [.github/workflows/backend.yml](.github/workflows/backend.yml) —
  compiles the backend, runs unit and integration tests, publishes the
  Surefire reports, and finally builds the backend Docker image.
* [.github/workflows/frontend.yml](.github/workflows/frontend.yml) —
  installs dependencies with `npm ci`, runs the production
  `ng build`, uploads the `dist/` artifact, and builds the frontend
  Docker image.

The pipelines do not need any secret to run. They use only public
images and a throw-away `JWT_SECRET` defined inside the runner.

---

## Security highlights

* Only port `80` is published on the host. Backend and database use
  `expose:` and stay on the internal Docker network.
* All secrets are read from `.env` (git-ignored) and injected as
  container environment variables. Nothing is hard-coded.
* The backend container runs as a non-root user.
* Nginx adds defensive HTTP headers (`X-Frame-Options`,
  `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`).
* Authentication uses signed JWTs (HS256) and the secret must be at
  least 32 bytes. The login endpoint is rate-limited per IP and email.

HTTPS is not terminated inside the stack on purpose: this academic
deployment is reached through `localhost`. In a real production setup
TLS would be terminated by an upstream component (Traefik, Caddy or a
managed load balancer) in front of the Nginx container.
