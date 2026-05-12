# 8. Despliegue — Criterio 7 y 8

> Este documento complementa [08-despliegue.md](08-despliegue.md) con las evidencias técnicas exigidas para la evaluación: gestión de artefactos, ficheros del despliegue y verificación de red.

## Índice

- [Gestión de ficheros y artefactos del despliegue](#gestión-de-ficheros-y-artefactos-del-despliegue)
- [La API es accesible a través del reverse proxy](#la-api-es-accesible-a-través-del-reverse-proxy)
- [El backend NO es accesible directamente desde el exterior](#el-backend-no-es-accesible-directamente-desde-el-exterior)

---

## Gestión de ficheros y artefactos del despliegue

### Qué ficheros son necesarios para desplegar

El proyecto solo necesita tres cosas en el servidor para arrancar desde cero:

| Fichero / recurso | Dónde está | Para qué sirve |
|---|---|---|
| `docker-compose.yml` | raíz del repositorio | Orquesta los tres contenedores y la red interna |
| `.env` | raíz del repositorio (no en git) | Variables de entorno con credenciales y configuración |
| `.env.example` | raíz del repositorio | Plantilla documentada de todas las variables |

No hace falta tener Java ni Node.js instalados en el servidor. Las imágenes Docker construyen todo internamente.

El repositorio completo está en: **[github.com/Aranaaa00/actacofrade](https://github.com/Aranaaa00/actacofrade)**

---

### Cómo se generan los artefactos

#### Imagen del backend

El `backend/Dockerfile` usa un build multietapa. En la primera etapa compila el JAR con Maven; en la segunda copia solo el JAR resultante sobre una imagen base de JRE Alpine mínima. El proceso además crea un usuario sin privilegios (`app`) para ejecutar el proceso:

```dockerfile
# backend/Dockerfile

# Build stage: compile the Spring Boot JAR
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage: lightweight JRE image, runs as non-root
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /app/target/*.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

El JAR que genera Maven (`backend/target/*.jar`) nunca se sube al repositorio. Está en `.gitignore` junto con todo el directorio `backend/target/`. Docker lo compila dentro del propio contenedor en cada build.

#### Imagen del frontend

El `frontend/Dockerfile` también usa dos etapas: la primera instala dependencias con `npm ci` y ejecuta el build de producción de Angular; la segunda copia los ficheros estáticos resultantes en una imagen nginx limpia con la configuración personalizada:

```dockerfile
# frontend/Dockerfile

# Build stage
FROM node:22-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# Serve stage
FROM nginx:alpine
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

El directorio `frontend/dist/` y `frontend/node_modules/` no se suben al repositorio. Están en `.gitignore`. Todo se genera dentro del contenedor.

---

### Qué NO debe subirse al repositorio

El `.gitignore` del proyecto excluye explícitamente:

```
# Secretos y entorno
.env
.env.*
!.env.example

# Artefactos del backend
backend/target/

# Artefactos del frontend
frontend/node_modules/
frontend/dist/
frontend/coverage/
```

El único fichero de entorno que sí está en el repositorio es `.env.example`, que contiene la plantilla documentada con todos los valores en blanco o con placeholders explicativos. Nunca contiene secretos reales.

---

### Variables de entorno obligatorias

El `.env` real que vive en el servidor se genera copiando `.env.example` y rellenando estas claves antes del primer arranque. Sin alguna de ellas el stack no se levanta:

| Variable | Para qué sirve |
|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Credenciales de la base de datos PostgreSQL. |
| `DB_USER` / `DB_PASSWORD` | Credenciales que usa el backend para conectarse. Deben coincidir con las anteriores. |
| `JWT_SECRET` | Clave HMAC para firmar los tokens JWT. Mínimo 32 bytes. Se genera con `openssl rand -base64 48`. |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos por CORS, separados por coma. En producción: `https://actacofrade.com,https://www.actacofrade.com`. |
| `SUPERADMIN_EMAIL` / `SUPERADMIN_PASSWORD` / `SUPERADMIN_FULL_NAME` | Datos del usuario Super Admin que se crea automáticamente en el primer arranque si no existe. |

Las variables opcionales (`JWT_EXPIRATION_MS`, `AVATAR_MAX_SIZE`, `AVATAR_MAX_BYTES`, `AVATAR_ALLOWED_TYPES`) tienen valores por defecto razonables en `application.properties`, así que no es necesario tocarlas salvo que se quiera ajustar algo concreto.

La lista completa con descripciones está en [.env.example](../.env.example) y también en la tabla de [DEPLOY.md](../DEPLOY.md).

---

### Imágenes publicadas en Docker Hub

El pipeline de CD (`.github/workflows/cd.yml`) publica automáticamente las imágenes en Docker Hub bajo el namespace `aranaa00` en cada push a `main`. Cada imagen lleva dos etiquetas: `latest` y el SHA del commit, para poder referenciar cualquier versión concreta si hace falta.

```yaml
# .github/workflows/cd.yml (fragmento)
- name: Build and push backend image
  uses: docker/build-push-action@v6
  with:
    context: ./backend
    push: true
    tags: |
      aranaa00/actacofrade-backend:latest
      aranaa00/actacofrade-backend:${{ github.sha }}

- name: Build and push frontend image
  uses: docker/build-push-action@v6
  with:
    context: ./frontend
    push: true
    tags: |
      aranaa00/actacofrade-frontend:latest
      aranaa00/actacofrade-frontend:${{ github.sha }}
```

Las imágenes están disponibles públicamente en:
- `docker.io/aranaa00/actacofrade-backend`
- `docker.io/aranaa00/actacofrade-frontend`

![Imágenes publicadas en Docker Hub](/docs/assets/deploy-dockerhub-images.png)

---

### Persistencia de datos

La base de datos PostgreSQL persiste sus datos en un volumen Docker con nombre fijo:

```yaml
# docker-compose.yml (fragmento)
volumes:
  postgres_data:
    name: actacofrade_pg_data
```

Este volumen es independiente del ciclo de vida de los contenedores. Un `docker compose down` para los contenedores pero **no borra el volumen**: los datos siguen ahí cuando se vuelve a levantar el stack. Solo se pierden si se ejecuta `docker compose down -v` explícitamente, o si se borra el volumen a mano con `docker volume rm actacofrade_pg_data`.

El contenedor de base de datos no mapea ningún puerto al host, por lo que los datos solo son accesibles desde el backend a través de la red interna de Docker. No hay forma de conectarse directamente a PostgreSQL desde fuera del stack.

#### Cómo se conservarían los datos

Para hacer una copia de seguridad del volumen, se exporta un dump SQL del contenedor de la base de datos:

```bash
docker exec actacofrade_db pg_dump -U $POSTGRES_USER $POSTGRES_DB > backup_$(date +%F).sql
```

Y para restaurarlo en otra instalación:

```bash
cat backup.sql | docker exec -i actacofrade_db psql -U $POSTGRES_USER -d $POSTGRES_DB
```

Es lo más simple y portable. No depende del sistema de ficheros del host ni de la versión exacta del volumen, basta con tener el dump SQL.

---

## Verificación de red del despliegue

Las comprobaciones siguientes mezclan dos puntos de vista: las que se ven desde fuera (mi equipo Windows con PowerShell, accediendo al dominio público) y las que requieren estar dentro del servidor (conexión SSH al Droplet de Digital Ocean, Ubuntu 24.04 LTS). En cada bloque indico desde dónde se ejecutó cada comando, para que las salidas reproducidas sean fielmente lo que devolvió mi terminal.

### URL y resolución de nombre

La aplicación está disponible en **https://www.actacofrade.com**. El dominio `actacofrade.com` y su subdominio `www` están delegados a los nameservers de Cloudflare, que actúa como CDN/proxy delante del Droplet de Digital Ocean. Por eso, al resolver el nombre desde cualquier equipo público, lo que se devuelve no es la IP directa del Droplet, sino la IP del proxy de Cloudflare. Esto añade una capa de protección: el origen real (el Droplet) no queda expuesto en DNS.

Comprobación hecha desde mi equipo Windows con PowerShell:

```powershell
nslookup www.actacofrade.com
```

Salida real obtenida:

```
Servidor:  SurcontrolAD.SURCONTROL.local
Address:  10.10.1.10

Respuesta no autoritativa:
Nombre:  www.actacofrade.com
Addresses:  172.67.171.146
          104.21.79.222
```

Las dos direcciones `172.67.171.146` y `104.21.79.222` pertenecen al rango público de Cloudflare, que recibe el tráfico HTTPS y lo reenvía al Droplet. El certificado TLS lo gestiona Cloudflare en el modo "Full", así que la conexión cliente ↔ Cloudflare está cifrada con un certificado de Cloudflare y la conexión Cloudflare ↔ Droplet va por HTTP/HTTPS al puerto 80 publicado por nginx.

### Estado de los contenedores

Esta comprobación se hace en el servidor (Droplet), conectando por SSH y ejecutando dentro del directorio del proyecto:

```bash
docker compose ps
```

La salida típica es:

```
NAME                   IMAGE                               STATUS
actacofrade_db         postgres:15-alpine                  Up X minutes (healthy)
actacofrade_backend    actacofrade-backend                 Up X minutes (healthy)
actacofrade_frontend   actacofrade-frontend                Up X minutes (healthy)
```

Los tres servicios aparecen como `healthy` porque cada uno tiene su propio healthcheck definido en el `docker-compose.yml`:

- `db`: `pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}`
- `backend`: `nc -z localhost 8080`
- `frontend`: `wget --quiet --tries=1 -O /dev/null http://127.0.0.1/healthz`

El healthcheck del frontend usa el endpoint `/healthz` que devuelve `200 ok` directamente desde nginx sin pasar por el backend, por lo que confirma que el servidor web está activo de forma independiente.

![Salida de docker compose ps con los tres servicios healthy](/docs/assets/deploy-compose-ps.png)

---

### Puerto publicado y punto de entrada

Solo el contenedor del frontend mapea un puerto al host:

```yaml
# docker-compose.yml (fragmento)
frontend:
  ports:
    - "80:80"
```

El backend solo usa `expose: 8080`, que declara el puerto internamente pero no lo publica en el host. La base de datos no tiene ningún `ports` ni `expose`.

El único punto de entrada desde el exterior es el puerto 80 del host, que recibe nginx dentro del contenedor del frontend.

---

### El frontend responde en el puerto 80/443

Desde Windows ejecuto `curl.exe` contra el dominio público:

```powershell
curl.exe -sD - -o $env:TEMP\b.txt https://www.actacofrade.com/
```

Salida real de las cabeceras:

```
HTTP/1.1 200 OK
Date: Mon, 11 May 2026 10:38:22 GMT
Content-Type: text/html
Transfer-Encoding: chunked
Connection: keep-alive
Server: cloudflare
Last-Modified: Thu, 07 May 2026 08:41:47 GMT
Vary: Accept-Encoding
Cache-Control: no-store, no-cache, must-revalidate
cf-cache-status: DYNAMIC
CF-RAY: 9fa08dbf2ae6b835-MAD
alt-svc: h3=":443"; ma=86400
```

La respuesta es `200 OK` y el `Content-Type` confirma que se está sirviendo el `index.html` de la SPA Angular. La cabecera `Server: cloudflare` (en vez de `nginx`) y la `CF-RAY` muestran que la respuesta pasa por Cloudflare antes de llegar al cliente. Las cabeceras de seguridad propias (`X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, `Cross-Origin-Opener-Policy`, `Cross-Origin-Resource-Policy`) las añade el `nginx.conf` con la directiva `always`, pero Cloudflare las filtra/reemplaza al servirlas al cliente final. Estas cabeceras se pueden ver directamente si se accede al Droplet por su IP origen sin pasar por el proxy.

También hay un endpoint `/healthz` independiente que nginx responde sin tocar el backend, y que se usa para el healthcheck del contenedor:

```powershell
curl.exe -s https://www.actacofrade.com/healthz
```

Salida real:

```
ok
```

![Respuesta de curl al frontend en producción](/docs/assets/deploy-curl-frontend.png)

---

### La API es accesible a través del reverse proxy

nginx redirige hacia el backend las rutas `/api/`, `/v3/api-docs` y `/swagger-ui` según las reglas del `nginx.conf`:

```nginx
# frontend/nginx.conf (fragmento)
upstream actacofrade_backend {
    server backend:8080;
    keepalive 32;
}

location /api/ {
    proxy_pass http://actacofrade_backend;
}

location /v3/api-docs {
    proxy_pass http://actacofrade_backend;
}

location /swagger-ui {
    proxy_pass http://actacofrade_backend;
}
```

La resolución del nombre `backend` la hace Docker DNS internamente dentro de la red `actacofrade_network`. Desde fuera del stack, ese nombre no existe.

Verificación desde mi equipo de que la API responde a través del proxy:

```powershell
curl.exe -fsS https://www.actacofrade.com/v3/api-docs | Out-File -Encoding ascii $env:TEMP\api.txt
(Get-Content $env:TEMP\api.txt -Raw).Substring(0,300)
```

Salida real (primeros 300 caracteres):

```json
{"openapi":"3.1.0","info":{"title":"ActaCofrade Backend API","description":"API REST para la gestión de actos cofrades, tareas, incidencias,\ndecisiones, usuarios y auditoría dentro de una hermandad.\n\n## Autenticación\n\nLa mayoría de los endpoints requieren un token JWT en la cabecera\n`Autho...
```

El JSON de OpenAPI llega correctamente, lo que confirma que nginx (via Cloudflare) está enrutando las peticiones a `/v3/api-docs` al backend sin problemas.

---

### El backend NO es accesible directamente desde el exterior

Esta es la comprobación más importante del apartado de seguridad de red. El backend nunca debe ser alcanzable desde fuera del stack, porque expondría la API sin pasar por las cabeceras de seguridad que añade nginx ni por el WAF de Cloudflare.

Intento conectarme al puerto 8080 del dominio público:

```powershell
curl.exe -s -v --max-time 5 http://actacofrade.com:8080/
```

Salida:

```
< HTTP/1.1 301 Moved Permanently
< Date: Mon, 11 May 2026 10:37:41 GMT
< Location: https://actacofrade.com/
< Server: cloudflare
< CF-RAY: 9fa08cbd4b328d3c-MAD
```

Lo que devuelve no es la API del backend, es un `301` de Cloudflare redirigiendo a `https://actacofrade.com/`. Esto demuestra dos cosas a la vez:

1. **La aplicación no responde por el puerto 8080**. La respuesta no viene del proceso Java de Spring Boot, viene de Cloudflare (`Server: cloudflare`, `CF-RAY`). Si el backend estuviera publicado, devolvería un JSON de error o similar.
2. **El origen (el Droplet) no es directamente alcanzable por nombre**. Todo el tráfico hacia `actacofrade.com` pasa sí o sí por Cloudflare, que solo reenvía al origen las peticiones HTTPS sobre el puerto 80 publicado. El Droplet ni siquiera publica el 8080 al exterior: en el `docker-compose.yml` el servicio del backend usa `expose: 8080` (visible solo dentro de la red Docker interna), nunca `ports`.

La combinación de `expose` (no `ports`) en el `docker-compose.yml` + la capa de Cloudflare hace que sea imposible llegar al backend sin pasar por nginx.

---

### Resumen de la topología de red

| Servicio | Red interna Docker | Puerto en el host | Accesible desde el exterior |
|---|---|---|---|
| `actacofrade_frontend` (nginx) | `actacofrade_network` | `80` → `80` | Sí, a través del proxy de Cloudflare |
| `actacofrade_backend` (Spring Boot) | `actacofrade_network` | Solo `expose 8080` | No |
| `actacofrade_db` (PostgreSQL) | `actacofrade_network` | Sin puertos | No |

Delante de todo, Cloudflare actúa como CDN/proxy: termina TLS, filtra peticiones y reenvía al Droplet. El único componente publicado en el host es el contenedor del frontend en el puerto 80.

---