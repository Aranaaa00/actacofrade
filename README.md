# ActaCofrade

ActaCofrade es una aplicación web pensada para hermandades y cofradías. Sirve para organizar sus actos: planificar el programa, repartir las tareas entre los miembros, dejar por escrito las decisiones tomadas en cada reunión y guardar el historial de incidencias de cada edición.

## Índice

- [Documentación del TFG](#documentación-del-tfg)
- [Arquitectura](#arquitectura)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Arranque rápido](#arranque-rápido)
- [Resumen de la API](#resumen-de-la-api)
- [Integración continua](#integración-continua)
- [Seguridad](#seguridad)

---

El proyecto tiene dos partes:

- **Backend.** API REST en Spring Boot 4 sobre Java 21. Lleva la lógica de negocio, la base de datos y la autenticación.
- **Frontend.** Aplicación Angular servida por Nginx, que además hace de reverse proxy del backend.

La aplicación está desplegada en producción en **[www.actacofrade.com](https://www.actacofrade.com)**.

Todo el stack se levanta con un único comando:

```bash
docker compose up -d
```

Las instrucciones completas de despliegue, las variables de entorno y las comprobaciones de verificación están en [DEPLOY.md](DEPLOY.md). La documentación detallada de la API vive en [backend/README.md](backend/README.md).

---

## Documentación del TFG

La memoria del proyecto está dividida por capítulos en la carpeta [`docs/`](docs/):

| Capítulo | Contenido |
|---|---|
| [1. Introducción, objetivos y antecedentes](docs/01-introduccion.md) | Origen de la idea, motivación y comparación con herramientas existentes. |
| [2. Descripción del proyecto](docs/02-descripcion.md) | Alcance funcional, roles y casos de uso. |
| [3. Instalación](docs/03-instalacion.md) | Requisitos, variables de entorno y arranque con Docker Compose. |
| [4. Guía de estilos](docs/04-guia-estilos.md) | Convenciones visuales del frontend. |
| [5. Diseño](docs/05-diseno.md) | Modelo de datos, arquitectura y decisiones técnicas. |
| [6. Desarrollo](docs/06-desarrollo.md) | Estructura del código, flujo de trabajo y patrones aplicados. |
| [7. Pruebas](docs/07-pruebas.md) | Estrategia de testing, cobertura y resultados. |
| [8. Despliegue](docs/08-despliegue.md) | Despliegue del entorno completo (resumen de [DEPLOY.md](DEPLOY.md)). |
| [8b. Despliegue — evidencias](docs/08-despliegue-eval.md) | Evidencias técnicas del despliegue: artefactos, red interna y verificaciones. |
| [9. Manual de usuario](docs/09-manual-usuario.md) | Guía de uso de la aplicación por roles. |
| [10. Conclusiones](docs/10-conclusiones.md) | Balance, lecciones aprendidas y líneas futuras. |

Otros materiales de referencia:

- [backend/README.md](backend/README.md) — documentación detallada de la API REST (endpoints, modelo, configuración).
- [frontend/README.md](frontend/README.md) — guía técnica de la SPA Angular (estructura, build, tests, estilos).
- [docs/propuesta.md](docs/propuesta.md) — propuesta original del TFG con la identificación de necesidades.
- [docs/postman/ActaCofrade_API.postman_collection.json](docs/postman/ActaCofrade_API.postman_collection.json) — colección de Postman lista para importar.

---

## Arquitectura

Tres servicios comparten una red Docker privada. Solo el frontend es accesible desde el host. El backend y la base de datos solo se ven desde dentro de esa red.

```text
   Navegador ──► localhost:80
                       │
                       ▼
            ┌──────────────────────┐
            │  frontend  (Nginx)   │
            │  · SPA Angular       │
            │  · /api/* → backend  │
            │  · /swagger-ui*      │
            │  · /v3/api-docs      │
            └─────────┬────────────┘
                      │  red interna
                      ▼
            ┌──────────────────────┐
            │  backend  (Spring)   │
            │  · API REST          │
            │  · Autenticación JWT │
            │  · Migraciones Flyway│
            └─────────┬────────────┘
                      │  red interna
                      ▼
            ┌──────────────────────┐
            │  db  (PostgreSQL 15) │
            │  · volumen: pg_data  │
            └──────────────────────┘
```

| Servicio   | Imagen                          | Puerto interno | Publicado en el host |
|------------|---------------------------------|----------------|----------------------|
| `frontend` | construida localmente (`nginx:alpine`) | 80      | **80**               |
| `backend`  | construida localmente (Temurin JRE 21) | 8080    | no                   |
| `db`       | `postgres:15-alpine`            | 5432           | no                   |

Cómo se comunican entre sí:

- El navegador solo habla con el frontend en `http://localhost`.
- Nginx reenvía `/api/*`, `/swagger-ui*` y `/v3/api-docs*` a `backend:8080`.
- El backend se conecta a la base de datos con `jdbc:postgresql://db:5432/${POSTGRES_DB}`.
- Los nombres `backend` y `db` los resuelve el DNS interno de la red `actacofrade_network`.

---

## Estructura del repositorio

```
.
├── backend/                API REST en Spring Boot (ver backend/README.md)
│   ├── src/                Código Java y tests
│   ├── Dockerfile          Build multi-stage (Maven → Temurin JRE)
│   └── pom.xml
├── frontend/               SPA Angular + Nginx como reverse proxy
│   ├── src/                Código Angular
│   ├── nginx.conf          Configuración del proxy y los estáticos
│   └── Dockerfile          Build multi-stage (Node → Nginx)
├── docs/                   Memoria del TFG y colección de Postman
├── docker-compose.yml      Definición del stack (db + backend + frontend)
├── .env.example            Plantilla del fichero .env
├── DEPLOY.md               Guía completa de despliegue
└── .github/workflows/      Pipelines de CI (backend.yml, frontend.yml)
```

---

## Arranque rápido

```bash
git clone https://github.com/Aranaaa00/actacofrade.git
cd actacofrade
cp .env.example .env
# Editar .env con valores reales (ver DEPLOY.md)
docker compose up -d --build
docker compose ps
```

Cuando los tres servicios aparezcan como `(healthy)`:

| URL                                       | Qué se obtiene                |
|-------------------------------------------|-------------------------------|
| `http://localhost/`                       | SPA Angular (pantalla de login) |
| `http://localhost/api/...`                | API REST                      |
| `http://localhost/swagger-ui.html`        | Documentación interactiva     |
| `http://localhost/v3/api-docs`            | OpenAPI en JSON               |

---

## Resumen de la API

- Ruta base: `/api`.
- Autenticación: cabecera `Authorization: Bearer <token>` obtenida con `POST /api/auth/login`.
- Roles: `SUPER_ADMIN`, `ADMINISTRADOR`, `RESPONSABLE`, `COLABORADOR`, `CONSULTA`.
- Recursos principales: `auth`, `me`, `users`, `roles`, `hermandades`, `events`, `tasks`, `decisions`, `incidents`, `audit`, `dashboard`, `admin-change-requests`.

### Ejemplos de uso

Con el stack levantado en local, las operaciones más comunes se pueden probar directamente con `curl`.

**1. Obtener un token (login)**

```bash
curl -s -X POST http://localhost/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@example.com","password":"tu_password"}'
```

Respuesta `200 OK`:

```json
{
  "userId": 1,
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBleGFtcGxlLmNvbSJ9...",
  "email": "admin@example.com",
  "fullName": "Ana García",
  "roles": ["ADMINISTRADOR"],
  "hermandadNombre": "Hermandad del Gran Poder",
  "hasAvatar": false
}
```

**2. Consultar el dashboard** (requiere token)

```bash
curl -s http://localhost/api/dashboard \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Respuesta `200 OK`:

```json
{
  "recentEvents": [
    {
      "id": 3,
      "title": "Ensayo general Semana Santa",
      "eventType": "ENSAYO",
      "status": "PREPARACION",
      "eventDate": "2025-03-15",
      "pendingTasks": 2,
      "openIncidents": 0
    }
  ],
  "alerts": [
    {
      "type": "TAREA_PENDIENTE",
      "description": "Tarea sin aceptar en Ensayo general Semana Santa",
      "eventId": 3,
      "eventDate": "2025-03-15"
    }
  ],
  "pendingTasksCount": 2,
  "readyToCloseCount": 0
}
```

**3. Crear un acto** (requiere rol `ADMINISTRADOR` o `RESPONSABLE`)

```bash
curl -s -X POST http://localhost/api/events \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
     -H "Content-Type: application/json" \
     -d '{
       "title": "Vía Crucis Cuaresmal",
       "eventType": "CULTOS",
       "eventDate": "2026-03-07",
       "location": "Iglesia de San Salvador",
       "responsibleId": 2,
       "observations": "Preparación a cargo de la Priostía"
     }'
```

Respuesta `201 Created`:

```json
{
  "id": 15,
  "reference": "CULTOS-2026-001",
  "title": "Vía Crucis Cuaresmal",
  "eventType": "CULTOS",
  "status": "PLANIFICACION",
  "eventDate": "2026-03-07",
  "location": "Iglesia de San Salvador",
  "responsibleName": "Carlos Moreno",
  "isLockedForClosing": false,
  "pendingTasks": 0,
  "openIncidents": 0,
  "totalTasks": 0,
  "completedTasks": 0
}
```

Cualquier petición con token inválido o rol insuficiente devuelve `401` o `403` con la estructura de error estándar:

```json
{
  "status": "error",
  "message": "Acceso denegado",
  "data": null,
  "errors": []
}
```

El listado completo de endpoints, la máquina de estados de las tareas y la estructura de errores están detallados en [backend/README.md](backend/README.md). La API también se puede explorar en vivo desde Swagger UI (`http://localhost/swagger-ui.html`) o consultar como JSON crudo en `http://localhost/v3/api-docs`.

---

## Integración continua

El repositorio tiene tres workflows en GitHub Actions, cada uno con un papel distinto:

- [.github/workflows/backend.yml](.github/workflows/backend.yml) — CI del backend: compila, ejecuta los tests unitarios e integración, publica los informes de Surefire y construye la imagen del backend. En cada push a `main` la publica también en **GHCR** como `ghcr.io/aranaaa00/actacofrade-backend:{latest,<sha>}`.
- [.github/workflows/frontend.yml](.github/workflows/frontend.yml) — CI del frontend: `npm ci`, `ng build --configuration production`, sube `dist/` como artefacto y construye la imagen del frontend. En cada push a `main` la publica también en **GHCR** como `ghcr.io/aranaaa00/actacofrade-frontend:{latest,<sha>}`.
- [.github/workflows/cd.yml](.github/workflows/cd.yml) — CD de producción: en cada push a `main` reconstruye ambas imágenes y las publica además en **Docker Hub** bajo el namespace `aranaa00` (`aranaa00/actacofrade-backend` y `aranaa00/actacofrade-frontend`), con `latest` y el SHA del commit. Las credenciales viven en los *repository secrets* `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN`.

Los workflows de CI no necesitan secretos externos: usan imágenes públicas y un `JWT_SECRET` de usar y tirar generado dentro del runner. La lista pública de imágenes GHCR está en <https://github.com/Aranaaa00?tab=packages>.

---

## Seguridad

- Solo se publica el puerto `80` en el host. El backend y la base de datos usan `expose:` y se quedan en la red interna de Docker.
- Todos los secretos se leen desde `.env` (incluido en `.gitignore`) y se inyectan como variables de entorno en los contenedores. Nada va escrito en el código.
- El contenedor del backend se ejecuta con un usuario sin privilegios.
- Nginx añade cabeceras defensivas (`X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`).
- La autenticación se basa en JWT firmados con HS256, y el secreto debe tener al menos 32 bytes. El endpoint de login está limitado por IP y por email para frenar intentos de fuerza bruta.

HTTPS no se termina dentro del stack. En producción, Cloudflare actúa como CDN delante del Droplet y termina TLS antes de reenviar las peticiones al Nginx del contenedor (modo "Full"). Para despliegues sin Cloudflare o sin otro terminador externo, al final de `nginx.conf` hay un bloque comentado con configuración TLS lista para activar (TLSv1.2 + TLSv1.3, HSTS, redirección HTTP→HTTPS).
