# Deployment guide

This guide explains how to deploy ActaCofrade from zero on any machine
with Docker, and how to check that the deployment really works.

For the project description and the architecture overview, see
[README.md](README.md). The API itself is documented in
[backend/README.md](backend/README.md).

---

## 1. Requirements

* Docker Desktop 4.x or Docker Engine ≥ 24, with the Compose plugin.
  The command `docker compose version` must work.
* Git.
* Free TCP port `80` on the host.

You do **not** need Java or Node.js on the host. The Docker images
build everything on their own.

---

## 2. Deploy from zero

```bash
# 1. Get the source code
git clone https://github.com/Aranaaa00/actacofrade.git
cd actacofrade

# 2. Create the environment file from the template
cp .env.example .env

# 3. Edit .env. The values you MUST change before the first start:
#      POSTGRES_PASSWORD
#      DB_PASSWORD            (same value as POSTGRES_PASSWORD)
#      JWT_SECRET             (>= 32 bytes; openssl rand -base64 48)
#      SUPERADMIN_EMAIL
#      SUPERADMIN_PASSWORD
#      SUPERADMIN_FULL_NAME

# 4. Build the images and start the stack
docker compose up -d --build

# 5. Wait until all three services are healthy
docker compose ps
```

When you see the three services in state `Up ... (healthy)` the
deployment is finished. The application is now available at
`http://localhost/`.

To stop everything:

```bash
docker compose down            # stop, keep the database data
docker compose down -v         # stop and delete the database data
```

---

## 3. Environment variables

All variables come from `.env`. Variables marked **required** must
have a real value (not the placeholder from `.env.example`) before the
first `docker compose up`.

| Variable                  | Required | What it does                                                              |
|---------------------------|:--------:|---------------------------------------------------------------------------|
| `POSTGRES_DB`             | yes      | Name of the database created on first start.                              |
| `POSTGRES_USER`           | yes      | Database user owned by the application.                                   |
| `POSTGRES_PASSWORD`       | yes      | Database password.                                                        |
| `DB_URL`                  | no       | JDBC URL. Compose overrides it to `jdbc:postgresql://db:5432/...`.        |
| `DB_USER` / `DB_PASSWORD` | yes      | Backend datasource (must match `POSTGRES_USER` / `POSTGRES_PASSWORD`).    |
| `JWT_SECRET`              | yes      | HMAC key for JWTs. Minimum 32 bytes.                                      |
| `JWT_EXPIRATION_MS`       | no       | JWT lifetime in milliseconds (default `86400000`, i.e. 24 h).             |
| `CORS_ALLOWED_ORIGINS`    | yes      | Allowed origins, comma separated. Example: `http://localhost`.            |
| `AVATAR_MAX_SIZE`         | no       | Spring multipart limit, e.g. `2MB`.                                       |
| `AVATAR_MAX_BYTES`        | no       | Same limit in bytes (must match `AVATAR_MAX_SIZE`).                       |
| `AVATAR_ALLOWED_TYPES`    | no       | MIME types accepted as avatar.                                            |
| `SUPERADMIN_EMAIL`        | no       | If set, a `SUPER_ADMIN` user is created on first start.                   |
| `SUPERADMIN_PASSWORD`     | no       | Password for that super-admin user.                                       |
| `SUPERADMIN_FULL_NAME`    | no       | Display name for that super-admin user.                                   |

`.env` is git-ignored and must never be committed. For CI/CD or real
production, store secrets in the secret manager of the platform
(GitHub Actions repository secrets, Docker secrets, etc.).

---

## 4. Verification

Run these commands after `docker compose up -d`. All of them must pass
to consider the deployment correct.

### 4.1 The three services are healthy

```bash
docker compose ps
```

Expected output:

```
NAME                   STATUS
actacofrade_db         Up X seconds (healthy)
actacofrade_backend    Up X seconds (healthy)
actacofrade_frontend   Up X seconds (healthy)
```

### 4.2 The frontend answers through the reverse proxy

```bash
curl -I http://localhost/
```

Expected: `HTTP/1.1 200 OK` served by `nginx`.

### 4.3 The API answers through the reverse proxy

```bash
curl -fsS http://localhost/v3/api-docs | head -c 200
```

Expected: a JSON document that starts with `{"openapi":"3...`.

```bash
curl -I http://localhost/swagger-ui.html
```

Expected: HTTP `200` or `302`. The interactive Swagger UI is served.

### 4.4 The backend port is NOT exposed (security check)

```bash
curl --max-time 3 -I http://localhost:8080/
```

Expected: connection refused or timeout. The backend must not be
reachable from the host.

### 4.5 Authentication smoke test

Replace the values with the super-admin credentials you set in `.env`:

```bash
curl -s -X POST http://localhost/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@example.com","password":"YOUR_PASSWORD"}'
```

Expected: a JSON response that contains an `accessToken` field.

### 4.6 Light load check

Send 50 requests to the OpenAPI document and print the HTTP code and
the time of each one:

```bash
for i in $(seq 1 50); do
  curl -s -o /dev/null -w "%{http_code} %{time_total}s\n" \
       http://localhost/v3/api-docs
done
```

Expected: every line shows `200` and the time stays below ~200 ms on a
normal laptop. This is enough to confirm that the reverse proxy and
the application server keep stable response times under repeated
calls.

### 4.7 Logs of each service

```bash
docker compose logs --tail=50 backend
docker compose logs --tail=20 frontend
docker compose logs --tail=20 db
```

What to look for:

* `backend` shows `Started BackendApplication in X seconds` and the
  list of Flyway migrations applied.
* `frontend` shows the Nginx start-up and real `GET` lines for every
  request you sent during the verification.
* `db` shows `database system is ready to accept connections`.

---

## 5. Continuous integration

There are two workflows in `.github/workflows/`:

| Workflow file  | Triggered by changes in | What it does                                                    |
|----------------|-------------------------|-----------------------------------------------------------------|
| `backend.yml`  | `backend/**`            | Compiles the API, runs unit and integration tests, builds the backend Docker image. |
| `frontend.yml` | `frontend/**`           | Installs dependencies, runs the production Angular build, uploads `dist/` and builds the frontend Docker image. |

Both pipelines also run on `workflow_dispatch`, so they can be
launched by hand from the *Actions* tab in GitHub.

The build does not need any real secret. If you add a deployment job
later, configure the credentials as **repository secrets** in GitHub
and never write them in the repository.

The Git workflow used in this project: feature branches with
descriptive commits in Spanish, `main` always green, no merge of red
builds.

---

## 6. Troubleshooting

| Problem                                                       | What to do                                                                                      |
|---------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| Backend logs show `JWT_SECRET ... too short`                  | Change `JWT_SECRET` in `.env` to at least 32 bytes (`openssl rand -base64 48`).                 |
| `actacofrade_backend` stays `unhealthy`                       | `docker compose logs backend`. Usually wrong DB credentials.                                    |
| `curl http://localhost/` returns `502 Bad Gateway`            | The backend is not healthy yet. Wait a few seconds and check the backend logs.                  |
| Port 80 is already in use                                     | Stop the program that uses it, or change the host mapping in `docker-compose.yml`.              |
| Login returns `401`                                           | The super-admin was not created. Set `SUPERADMIN_*` and run `docker compose down -v` followed by `docker compose up -d`. |
| You changed code and the container does not pick it up        | `docker compose build --no-cache <service>` and then `docker compose up -d`.                    |
| Reset everything to a clean state                             | `docker compose down -v && docker compose build --no-cache && docker compose up -d`.            |
