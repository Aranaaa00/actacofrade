# Guía de despliegue

Esta guía explica cómo desplegar ActaCofrade desde cero en cualquier máquina con Docker, y cómo comprobar que el despliegue funciona realmente.

Para la descripción del proyecto y la arquitectura general, ver [README.md](README.md). La API está documentada en [backend/README.md](backend/README.md).

---

## 1. Requisitos

- Docker Desktop 4.x o Docker Engine ≥ 24, con el plugin de Compose. El comando `docker compose version` debe responder.
- Git.
- Puerto TCP `80` libre en el host.

No hace falta tener Java ni Node.js instalados. Las imágenes Docker compilan todo por su cuenta.

---

## 2. Despliegue desde cero

```bash
# 1. Clonar el repositorio
git clone https://github.com/Aranaaa00/actacofrade.git
cd actacofrade

# 2. Crear el fichero de entorno a partir de la plantilla
cp .env.example .env

# 3. Editar .env. Antes del primer arranque hay que cambiar SÍ o SÍ:
#      POSTGRES_PASSWORD
#      DB_PASSWORD            (mismo valor que POSTGRES_PASSWORD)
#      JWT_SECRET             (>= 32 bytes; openssl rand -base64 48)
#      SUPERADMIN_EMAIL
#      SUPERADMIN_PASSWORD
#      SUPERADMIN_FULL_NAME

# 4. Construir las imágenes y arrancar el stack
docker compose up -d --build

# 5. Esperar a que los tres servicios estén "healthy"
docker compose ps
```

Cuando los tres servicios aparezcan como `Up ... (healthy)`, el despliegue ha terminado. La aplicación está en `http://localhost/`.

Para parar el stack:

```bash
docker compose down            # parar, conservando los datos de la base
docker compose down -v         # parar y borrar también los datos
```

---

## 3. Variables de entorno

Toda la configuración viene del fichero `.env`. Las variables marcadas como **obligatorias** deben tener un valor real (no el placeholder de `.env.example`) antes del primer `docker compose up`.

| Variable                  | Obligatoria | Para qué sirve                                                            |
|---------------------------|:-----------:|---------------------------------------------------------------------------|
| `POSTGRES_DB`             | sí          | Nombre de la base de datos creada en el primer arranque.                  |
| `POSTGRES_USER`           | sí          | Usuario propietario de la base de datos.                                  |
| `POSTGRES_PASSWORD`       | sí          | Contraseña de la base de datos.                                           |
| `DB_URL`                  | no          | URL JDBC. Compose la sobrescribe a `jdbc:postgresql://db:5432/...`.       |
| `DB_USER` / `DB_PASSWORD` | sí          | Datasource del backend (debe coincidir con `POSTGRES_USER` / `POSTGRES_PASSWORD`). |
| `JWT_SECRET`              | sí          | Clave HMAC para firmar JWTs. Mínimo 32 bytes.                             |
| `JWT_EXPIRATION_MS`       | no          | Vida del JWT en milisegundos (por defecto `86400000`, 24 h).              |
| `CORS_ALLOWED_ORIGINS`    | sí          | Orígenes permitidos separados por coma. Ejemplo: `http://localhost`.      |
| `AVATAR_MAX_SIZE`         | no          | Límite de multipart de Spring, p. ej. `2MB`.                              |
| `AVATAR_MAX_BYTES`        | no          | El mismo límite en bytes (debe coincidir con `AVATAR_MAX_SIZE`).          |
| `AVATAR_ALLOWED_TYPES`    | no          | MIME types aceptados como avatar.                                         |
| `SUPERADMIN_EMAIL`        | no          | Si se indica, crea un usuario `SUPER_ADMIN` en el primer arranque.        |
| `SUPERADMIN_PASSWORD`     | no          | Contraseña de ese super-admin.                                            |
| `SUPERADMIN_FULL_NAME`    | no          | Nombre visible del super-admin.                                           |

`.env` está en `.gitignore` y nunca debe subirse al repositorio. Para CI/CD o producción real, los secretos van en el gestor de la plataforma (secrets de GitHub Actions, Docker secrets, etc.).

---

## 4. Verificación

Estas comprobaciones se hacen tras `docker compose up -d`. Todas deben pasar para considerar correcto el despliegue.

### 4.1 Los tres servicios están sanos

```bash
docker compose ps
```

Salida esperada:

```
NAME                   STATUS
actacofrade_db         Up X seconds (healthy)
actacofrade_backend    Up X seconds (healthy)
actacofrade_frontend   Up X seconds (healthy)
```

### 4.2 El frontend responde a través del reverse proxy

```bash
curl -I http://localhost/
```

Esperado: `HTTP/1.1 200 OK` servido por `nginx`.

### 4.3 La API responde a través del reverse proxy

```bash
curl -fsS http://localhost/v3/api-docs | head -c 200
```

Esperado: un JSON que empieza por `{"openapi":"3...`.

```bash
curl -I http://localhost/swagger-ui.html
```

Esperado: HTTP `200` o `302`. Swagger UI se sirve correctamente.

### 4.4 El puerto del backend NO está expuesto

```bash
curl --max-time 3 -I http://localhost:8080/
```

Esperado: conexión rechazada o timeout. El backend no debe ser alcanzable desde el host.

### 4.5 Smoke test de autenticación

Sustituyendo los valores por las credenciales del super-admin definido en `.env`:

```bash
curl -s -X POST http://localhost/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@example.com","password":"TU_PASSWORD"}'
```

Esperado: una respuesta JSON con un campo `accessToken`.

### 4.6 Carga ligera

Lanzar 50 peticiones al documento OpenAPI e imprimir el código HTTP y el tiempo de cada una:

```bash
for i in $(seq 1 50); do
  curl -s -o /dev/null -w "%{http_code} %{time_total}s\n" \
       http://localhost/v3/api-docs
done
```

Esperado: todas las líneas muestran `200` y el tiempo se mantiene por debajo de ~200 ms en un portátil normal. Es suficiente para confirmar que el reverse proxy y el servidor de aplicaciones aguantan llamadas repetidas con tiempos estables.

### 4.7 Logs de cada servicio

```bash
docker compose logs --tail=50 backend
docker compose logs --tail=20 frontend
docker compose logs --tail=20 db
```

Qué hay que ver:

- `backend`: la línea `Started BackendApplication in X seconds` y la lista de migraciones de Flyway aplicadas.
- `frontend`: el arranque de Nginx y un `GET` real por cada petición lanzada en la verificación.
- `db`: `database system is ready to accept connections`.

---

## 5. Servidor web y reverse proxy

La capa web es **Nginx** (imagen `nginx:alpine`) corriendo dentro del servicio `frontend`. Es el **único** contenedor expuesto al host (puerto `80`). Tanto el backend de Spring Boot como la base de datos PostgreSQL están en la red interna `actacofrade_network` y **no** publican puertos, así que no se pueden alcanzar desde fuera del host Docker.

### 5.1 Qué hace Nginx

| Responsabilidad           | Cómo                                                                  |
|---------------------------|-----------------------------------------------------------------------|
| Estáticos (Angular)       | Sirve `/usr/share/nginx/html` (la salida del `ng build` de producción). |
| Routing de la SPA         | `try_files $uri $uri/ /index.html` para que el router de Angular renderice los enlaces profundos. |
| Caché larga de hashes     | `*.js`, `*.css`, fuentes, imágenes → `Cache-Control: public, immutable, 1y`. |
| Shell siempre fresco      | `index.html` → `Cache-Control: no-store`.                             |
| Compresión                | `gzip` sobre tipos de texto (`application/json`, `text/css`, ...).    |
| Reverse proxy             | `/api/`, `/v3/api-docs`, `/swagger-ui`, `/swagger-ui.html` → `backend:8080` por la red interna, con keepalive HTTP/1.1 (`upstream` + `keepalive 32`). |
| Cabeceras forwarded       | `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host`, `X-Forwarded-Port` para que Spring Boot construya URLs absolutas correctas (Swagger UI, redirecciones). |
| Endurecimiento            | `server_tokens off`, `client_max_body_size 4m`, bloqueo de dotfiles, cabeceras de seguridad (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy`, `Cross-Origin-Opener-Policy`, `Cross-Origin-Resource-Policy`). |
| Endpoint de salud         | `GET /healthz` devuelve `200 ok` y lo usa el healthcheck Docker del servicio `frontend`. |

La configuración completa está en [`frontend/nginx.conf`](frontend/nginx.conf) y la copia en `/etc/nginx/conf.d/default.conf` el [`frontend/Dockerfile`](frontend/Dockerfile).

### 5.2 HTTPS

HTTPS **no** se termina en esta imagen a propósito:

- El entregable es un stack Docker autocontenido pensado para correr en un portátil, en CI o detrás de un ingress propio de la organización (Nginx en el host, Traefik, Caddy o un balanceador cloud). Esos terminadores ya gestionan certificados, OCSP stapling y renovación, así que duplicarlo dentro del contenedor solo añade superficie de ataque y complica el desarrollo local.
- Para verificación local (`http://localhost`) los navegadores no exigen TLS, y los emisores ACME (Let's Encrypt) no pueden validar `localhost`.
- La configuración está **lista para HTTPS**: al final de `nginx.conf` hay un bloque `server { listen 443 ssl http2; ... }` comentado con selección moderna de protocolos y cifrados (`TLSv1.2`, `TLSv1.3`), cabecera `HSTS` y redirección HTTP→HTTPS. Activarlo solo requiere tres pasos: montar un volumen con el certificado, publicar el puerto `443` en `docker-compose.yml` y descomentar el bloque.
- `X-Forwarded-Proto` ya se propaga, así que el backend genera URLs `https://` correctas en el momento en que haya un terminador TLS delante de Nginx.

`Strict-Transport-Security` no se envía sobre HTTP plano deliberadamente: enviarlo haría que los navegadores rechazaran el sitio si HTTPS aún no está disponible. Forma parte del snippet listo para HTTPS.

### 5.3 Evidencias

Tras `docker compose up -d`, estos comandos son los que demuestran que el reverse proxy funciona.

**Fichero de configuración**

```bash
cat frontend/nginx.conf
```

**`curl -I` contra el proxy** (estas peticiones llegan a Nginx, no al backend directamente: el backend no tiene puerto en el host):

```bash
# Shell del frontend
curl -I http://localhost/
# -> HTTP/1.1 200 OK, Server: nginx, Cache-Control: no-store en /index.html

# API a través del proxy (documento OpenAPI servido por el backend)
curl -I http://localhost/v3/api-docs
# -> HTTP/1.1 200 OK, Content-Type: application/json

# Swagger UI a través del proxy
curl -I http://localhost/swagger-ui.html
# -> HTTP/1.1 302 Found, Location: /swagger-ui/index.html

# Salud del proxy
curl -I http://localhost/healthz
# -> HTTP/1.1 200 OK

# El backend NO se alcanza directamente desde el host
curl --max-time 3 -I http://localhost:8080/   # connection refused / timeout
```

**Logs reales del proxy con esas peticiones**

```bash
docker compose logs --tail=20 frontend
```

Debería verse una línea de access-log por cada `curl`, p. ej.:

```
192.168.x.x - - [..] "HEAD / HTTP/1.1" 200 0 "-" "curl/8.x"
192.168.x.x - - [..] "HEAD /v3/api-docs HTTP/1.1" 200 0 "-" "curl/8.x"
192.168.x.x - - [..] "HEAD /swagger-ui.html HTTP/1.1" 302 0 "-" "curl/8.x"
```

Nginx escribe `access.log` en `/dev/stdout` y `error.log` en `/dev/stderr` (configuración por defecto de la imagen `nginx:alpine`), así que todo el tráfico del proxy queda capturado por Docker y se ve con `docker compose logs`.

---

## 6. Integración continua

Hay dos workflows en `.github/workflows/`:

| Workflow       | Se dispara con cambios en | Qué hace                                                        |
|----------------|---------------------------|-----------------------------------------------------------------|
| `backend.yml`  | `backend/**`              | Compila la API, ejecuta tests unitarios e integración y construye la imagen Docker del backend. |
| `frontend.yml` | `frontend/**`             | Instala dependencias, lanza el build de producción de Angular, sube `dist/` y construye la imagen Docker del frontend. |

Ambos también responden a `workflow_dispatch`, así que se pueden lanzar a mano desde la pestaña *Actions* de GitHub.

El build no necesita secretos reales. Si más adelante se añade un job de despliegue, las credenciales se configuran como **repository secrets** y nunca se escriben en el repositorio.

Flujo de Git del proyecto: ramas de feature con commits descriptivos en español, `main` siempre verde y nada que se mezcle con tests en rojo.

---

## 7. Resolución de problemas

| Problema                                                       | Qué hacer                                                                                       |
|----------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| Los logs del backend muestran `JWT_SECRET ... too short`       | Cambiar `JWT_SECRET` en `.env` a un valor de al menos 32 bytes (`openssl rand -base64 48`).     |
| `actacofrade_backend` se queda en `unhealthy`                  | `docker compose logs backend`. Casi siempre son credenciales de base de datos incorrectas.      |
| `curl http://localhost/` devuelve `502 Bad Gateway`            | El backend aún no está sano. Esperar unos segundos y revisar los logs.                          |
| El puerto 80 ya está en uso                                    | Detener el proceso que lo ocupa o cambiar el mapeo en `docker-compose.yml`.                     |
| El login devuelve `401`                                        | El super-admin no se llegó a crear. Definir las variables `SUPERADMIN_*` y reiniciar con `docker compose down -v` seguido de `docker compose up -d`. |
| Tras cambiar código, el contenedor sigue con la versión vieja  | `docker compose build --no-cache <servicio>` y luego `docker compose up -d`.                    |
| Empezar de cero                                                | `docker compose down -v && docker compose build --no-cache && docker compose up -d`.            |
