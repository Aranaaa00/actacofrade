# 3. Instalación

## Requisitos previos

Para desplegar ActaCofrade solo hace falta tener instalado **Docker**. No es necesario tener Java, Node.js ni ninguna otra herramienta en el sistema: las imágenes del proyecto lo compilan y ejecutan todo internamente.

| Herramienta | Versión mínima | Para qué se usa |
|---|---|---|
| Docker Desktop / Docker Engine | 4.x / 24+ | Construir y ejecutar los tres contenedores |
| Plugin Compose | Incluido en Docker Desktop | Orquestar los servicios con `docker compose` |
| Git | Cualquier versión reciente | Clonar el repositorio |

Para verificar que el entorno está listo antes de empezar:

```bash
docker --version
docker compose version
git --version
```

El puerto **80** del host debe estar libre. Si lo ocupa otro servicio, hay que detenerlo antes del primer arranque.

---

## Arquitectura del sistema

La aplicación se levanta como un stack de tres contenedores Docker que se comunican entre sí a través de una red interna:

- **`actacofrade_db`:** base de datos PostgreSQL 15. No expone ningún puerto al exterior: solo el backend puede acceder a ella.
- **`actacofrade_backend`:** API REST construida con Spring Boot 4 sobre Java 21. Tampoco expone puertos al host directamente.
- **`actacofrade_frontend`:** Angular servido por nginx en el puerto 80. Este contenedor actúa también como reverse proxy, redirigiendo las peticiones a `/api`, `/swagger-ui` y `/v3/api-docs` hacia el backend.

El frontend es el único punto de entrada desde el exterior. El backend y la base de datos están completamente aislados de la red del host por diseño.

---

## Variables de entorno

Toda la configuración se gestiona a través de un fichero `.env` que hay que crear antes del primer arranque. El repositorio incluye un `.env.example` con todos los valores y sus descripciones.

Las variables marcadas como **obligatorias** no tienen valor por defecto y el sistema no arrancará correctamente sin ellas.

| Variable | Obligatoria | Descripción |
|---|:---:|---|
| `POSTGRES_DB` | sí | Nombre de la base de datos que se crea en el primer arranque. |
| `POSTGRES_USER` | sí | Usuario de la base de datos. |
| `POSTGRES_PASSWORD` | sí | Contraseña del usuario de la base de datos. |
| `DB_URL` | no | URL JDBC de conexión. Docker Compose la sobreescribe automáticamente. |
| `DB_USER` | sí | Usuario que usa el backend para conectarse (debe coincidir con `POSTGRES_USER`). |
| `DB_PASSWORD` | sí | Contraseña del usuario del backend (debe coincidir con `POSTGRES_PASSWORD`). |
| `JWT_SECRET` | sí | Clave HMAC para firmar los tokens JWT. Mínimo 32 bytes. |
| `JWT_EXPIRATION_MS` | no | Duración de los tokens en milisegundos. Por defecto `86400000` (24 horas). |
| `CORS_ALLOWED_ORIGINS` | sí | Orígenes permitidos separados por coma. Ejemplo: `http://localhost`. |
| `AVATAR_MAX_SIZE` | no | Límite de tamaño del multipart de Spring. Por defecto `2MB`. |
| `AVATAR_MAX_BYTES` | no | Mismo límite en bytes para la validación extra. Por defecto `2097152`. |
| `AVATAR_ALLOWED_TYPES` | no | MIME types aceptados como avatar. |
| `SUPERADMIN_EMAIL` | no | Si se indica, crea un usuario `SUPER_ADMIN` en el primer arranque. |
| `SUPERADMIN_PASSWORD` | no | Contraseña del superadministrador. |
| `SUPERADMIN_FULL_NAME` | no | Nombre completo del superadministrador. |

El `JWT_SECRET` debe ser una cadena aleatoria de al menos 32 bytes. Se puede generar con:

```bash
openssl rand -base64 48
```

El fichero `.env` está en el `.gitignore` del proyecto y nunca debe subirse al repositorio.

---

## Instalación con Docker Compose

### Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/Aranaaa00/actacofrade.git
cd actacofrade
```

### Paso 2 — Crear el fichero de configuración

```bash
cp .env.example .env
```

A continuación editar `.env` y ajustar al menos estos valores:

```dotenv
POSTGRES_PASSWORD=una-contraseña-segura
DB_PASSWORD=una-contraseña-segura   # mismo valor que POSTGRES_PASSWORD

JWT_SECRET=reemplazar-con-cadena-aleatoria-minimo-32-bytes

SUPERADMIN_EMAIL=admin@hermandad.es
SUPERADMIN_PASSWORD=contraseña-del-admin
SUPERADMIN_FULL_NAME=Nombre Apellidos

CORS_ALLOWED_ORIGINS=http://localhost
```

### Paso 3 — Construir las imágenes y levantar el stack

```bash
docker compose up -d --build
```

Docker descarga las imágenes base, compila el JAR de Spring Boot, construye el bundle de Angular y arranca los tres contenedores. La primera vez puede tardar varios minutos dependiendo de la conexión y del hardware.

### Paso 4 — Verificar que los servicios están sanos

```bash
docker compose ps
```

El resultado esperado es que los tres servicios aparezcan en estado `Up ... (healthy)`:

```
NAME                   STATUS
actacofrade_db         Up X seconds (healthy)
actacofrade_backend    Up X seconds (healthy)
actacofrade_frontend   Up X seconds (healthy)
```

Cuando los tres están `healthy`, la aplicación está disponible en `http://localhost/`.

---

## Verificación del despliegue

Una vez levantado el stack, se pueden ejecutar las siguientes comprobaciones para confirmar que todo funciona correctamente:

**El frontend responde:**

```bash
curl -I http://localhost/
```

Respuesta esperada: `HTTP/1.1 200 OK` servido por nginx.

**La API responde a través del proxy:**

```bash
curl -fsS http://localhost/v3/api-docs | head -c 200
```

Respuesta esperada: un JSON que empieza por `{"openapi":"3...`. La documentación interactiva de la API está disponible en `http://localhost/swagger-ui.html`.

**El backend NO es accesible directamente desde el host (verificación de seguridad):**

```bash
curl --max-time 3 -I http://localhost:8080/
```

Respuesta esperada: conexión rechazada o timeout. El puerto 8080 del backend no debe estar expuesto.

**Autenticación con el superadministrador:**

```bash
curl -s -X POST http://localhost/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@hermandad.es","password":"contraseña-del-admin"}'
```

Respuesta esperada: un JSON con el campo `accessToken` que contiene el JWT de sesión.

---

## Gestión del stack

Para detener los contenedores manteniendo los datos de la base de datos:

```bash
docker compose down
```

Para detener los contenedores y eliminar también los datos:

```bash
docker compose down -v
```

Para ver los logs de cada servicio:

```bash
docker compose logs --tail=50 backend
docker compose logs --tail=20 frontend
docker compose logs --tail=20 db
```

Al revisar los logs del backend, las dos líneas que indican un arranque correcto son:

- `Started BackendApplication in X seconds` — el servidor está listo.
- Las migraciones de Flyway aplicadas correctamente — el esquema de base de datos está al día.

---

## Ejecución en local sin Docker

Para desarrollo activo, el backend y el frontend se pueden ejecutar directamente en el sistema sin contenedores. En este caso sí son necesarios Java 21, Maven y Node.js 22.

**Backend:**

```bash
cd backend
./mvnw spring-boot:run
```

El backend arranca en `http://localhost:8080`. Necesita una instancia de PostgreSQL accesible con las credenciales definidas en `application.properties` o las variables de entorno correspondientes.

**Frontend:**

```bash
cd frontend
npm install
npm start
```

El servidor de desarrollo de Angular arranca en `http://localhost:4200`. El fichero `proxy.conf.json` redirige automáticamente las peticiones a `/api` hacia `http://localhost:8080`, por lo que el frontend y el backend se comunican sin necesidad de configurar CORS manualmente.

---

## Posibles problemas y soluciones

| Problema | Causa probable | Solución |
|---|---|---|
| El backend aparece `unhealthy` | Credenciales de base de datos incorrectas | Revisar que `DB_USER` y `DB_PASSWORD` coinciden con `POSTGRES_USER` y `POSTGRES_PASSWORD`. |
| Error `JWT_SECRET ... too short` en los logs | La clave JWT es demasiado corta | Generar una clave nueva con `openssl rand -base64 48` y reiniciar. |
| `502 Bad Gateway` al abrir el navegador | El backend aún no está listo | Esperar unos segundos y volver a intentarlo. El frontend espera a que el backend esté `healthy` antes de arrancar. |
| El puerto 80 ya está en uso | Otro servicio ocupa el puerto | Detener el proceso que lo usa o cambiar el mapeo de puertos en `docker-compose.yml`. |
| Login devuelve `401` aunque las credenciales son correctas | El superadministrador no se creó | Verificar que `SUPERADMIN_*` están definidos en `.env`, ejecutar `docker compose down -v` y volver a levantar. |
| Los cambios en el código no se reflejan | La imagen está cacheada | Reconstruir con `docker compose build --no-cache <servicio>` y reiniciar. |
