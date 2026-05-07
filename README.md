# ActaCofrade

ActaCofrade es una aplicación web pensada para hermandades y cofradías. Sirve para organizar sus actos: planificar el programa, repartir las tareas entre los miembros, dejar por escrito las decisiones tomadas en cada reunión y guardar el historial de incidencias de cada edición.

El proyecto tiene dos partes:

- **Backend.** API REST en Spring Boot 4 sobre Java 21. Lleva la lógica de negocio, la base de datos y la autenticación.
- **Frontend.** Aplicación Angular servida por Nginx, que además hace de reverse proxy del backend.

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
| [9. Manual de usuario](docs/09-manual-usuario.md) | Guía de uso de la aplicación por roles. |
| [10. Conclusiones](docs/10-conclusiones.md) | Balance, lecciones aprendidas y líneas futuras. |

Otros materiales de referencia:

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

El listado completo de endpoints, la máquina de estados de las tareas y la estructura de errores están detallados en [backend/README.md](backend/README.md). La API también se puede explorar en vivo desde Swagger UI (`http://localhost/swagger-ui.html`) o consultar como JSON crudo en `http://localhost/v3/api-docs`.

---

## Integración continua

El repositorio tiene dos workflows en GitHub Actions. Cada uno se ejecuta solo cuando cambia su parte del proyecto:

- [.github/workflows/backend.yml](.github/workflows/backend.yml) — compila el backend, ejecuta los tests unitarios e integración, publica los informes de Surefire y construye la imagen Docker del backend.
- [.github/workflows/frontend.yml](.github/workflows/frontend.yml) — instala dependencias con `npm ci`, ejecuta el `ng build` de producción, sube el `dist/` como artefacto y construye la imagen Docker del frontend.

Las pipelines no necesitan secretos externos: usan únicamente imágenes públicas y un `JWT_SECRET` de usar y tirar generado dentro del runner.

En cada ejecución correcta sobre `main`, ambos workflows publican las imágenes resultantes en el **GitHub Container Registry** usando el `GITHUB_TOKEN` integrado:

- `ghcr.io/aranaaa00/actacofrade-backend:latest`
- `ghcr.io/aranaaa00/actacofrade-frontend:latest`

Cada imagen se etiqueta también con el SHA del commit (`...:<sha>`), así que se puede recuperar cualquier build pasado. La lista completa está en <https://github.com/Aranaaa00?tab=packages>.

---

## Seguridad

- Solo se publica el puerto `80` en el host. El backend y la base de datos usan `expose:` y se quedan en la red interna de Docker.
- Todos los secretos se leen desde `.env` (incluido en `.gitignore`) y se inyectan como variables de entorno en los contenedores. Nada va escrito en el código.
- El contenedor del backend se ejecuta con un usuario sin privilegios.
- Nginx añade cabeceras defensivas (`X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`).
- La autenticación se basa en JWT firmados con HS256, y el secreto debe tener al menos 32 bytes. El endpoint de login está limitado por IP y por email para frenar intentos de fuerza bruta.

HTTPS no se termina dentro del stack a propósito: este despliegue académico se accede por `localhost`. En un escenario real, el TLS lo terminaría un componente delante del Nginx (Traefik, Caddy o un balanceador gestionado).
