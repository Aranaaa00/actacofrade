# 8. Despliegue

> Las evidencias técnicas de evaluación (artefactos, verificación de red y capturas) están en [08-despliegue-eval.md](08-despliegue-eval.md).

## Índice

- [Dónde está desplegado](#dónde-está-desplegado)
- [CI/CD con GitHub Actions](#cicd-con-github-actions)
- [Proceso de despliegue](#proceso-de-despliegue)
- [Comprobaciones post-despliegue](#comprobaciones-post-despliegue)

---

## Dónde está desplegado

Para el despliegue en producción opté por **Digital Ocean** con un Droplet básico. No era la opción más sencilla sobre el papel, pero sí la que más control me daba: acceso SSH directo al servidor, sin capas intermedias, y el mismo entorno Docker que tenía en local. Plataformas como Railway o Render las descarté porque quería que el stack se comportase exactamente igual en el servidor que en mi máquina, y eso solo lo garantizaba con una VPS donde puedo hacer lo que necesite.

El Droplet elegido es un plan compartido de CPU básica con 2 GB de RAM y 60 GB de disco SSD, corriendo Ubuntu 24.04 LTS. Para una aplicación de uso restringido a hermandades es más que suficiente.

![Panel del Droplet en Digital Ocean](/docs/assets/deploy-droplet-panel.png)


En el Droplet instalé Docker Engine directamente sobre Ubuntu, siguiendo la guía oficial. Sin Docker Desktop, solo el motor y el plugin de Compose. Para comprobar que quedó bien instalado:

```bash
docker --version
docker compose version
```

![Salida de docker --version y docker compose version en el Droplet](/docs/assets/deploy-docker-version.png)

El dominio `actacofrade.com` apunta directamente a la IP pública del Droplet con un registro en el proveedor del dominio. No hay balanceador de carga ni CDN por delante: las peticiones llegan al puerto 80 del servidor, que es donde escucha Nginx dentro del contenedor del frontend.

---

## CI/CD con GitHub Actions

Desde el principio quería que cada vez que subiera código a `main`, las imágenes de producción se regenerasen automáticamente. Lo monté con **GitHub Actions**, usando tres ficheros en `.github/workflows/`.

La idea es sencilla: dos workflows de CI comprueban que el código compila y los tests pasan, y publican las imágenes en **GHCR** (`ghcr.io/aranaaa00/...`) como artefactos verificados. Un tercer workflow de CD construye las imágenes definitivas y las publica también en **Docker Hub** (`aranaa00/...`), que es desde donde el servidor las consume. Solo cuando el código está validado llega algo a producción.

### Los pipelines de CI

`backend.yml` y `frontend.yml` se disparan en cada push o pull request a `main` cuando hay cambios en sus respectivas carpetas. En el caso del backend, se ejecuta `./mvnw clean verify`, que compila, lanza todos los tests y genera el informe de cobertura. Si algo falla, el merge queda bloqueado. En el frontend se lanza `npm ci` seguido de `ng build --configuration production` para asegurarse de que el build de producción no tiene errores.

Estos workflows son una red de seguridad para que no dejen llegar código roto a la rama principal.

### El pipeline de CD

`cd.yml` se lanza automáticamente en cada push a `main` y también puede dispararse manualmente desde la interfaz de GitHub. Su trabajo es construir las imágenes definitivas y publicarlas en Docker Hub bajo el namespace `aranaa00`:

```yaml
jobs:
  backend-image:
    steps:
      - name: Build and test backend
        run: ./mvnw -B -ntp clean verify

      - name: Build and push backend image
        uses: docker/build-push-action@v6
        with:
          tags: |
            aranaa00/actacofrade-backend:latest
            aranaa00/actacofrade-backend:${{ github.sha }}

  frontend-image:
    steps:
      - name: Production build
        run: npx ng build --configuration production

      - name: Build and push frontend image
        uses: docker/build-push-action@v6
        with:
          tags: |
            aranaa00/actacofrade-frontend:latest
            aranaa00/actacofrade-frontend:${{ github.sha }}
```

Antes de publicar nada, el backend vuelve a compilar y a pasar sus tests. Si algo falla ahí, la imagen no se publica. Además de la etiqueta `latest`, cada imagen lleva el SHA del commit, por si en algún momento hace falta volver a una versión anterior sin depender del tag genérico.

Las credenciales de Docker Hub y el `JWT_SECRET` que necesitan los tests están guardados como secretos en el repositorio de GitHub, nunca en el código.

![Pipeline de CD ejecutándose en GitHub Actions](/docs/assets/deploy-github-actions-cd.png)

![Imágenes publicadas en Docker Hub](/docs/assets/deploy-dockerhub-images.png)

---

## Proceso de despliegue

El pipeline de CD genera las imágenes, pero la actualización del servidor la hago de forma manual conectándome por SSH. Lo hago así a propósito: prefiero no tener actualizaciones automáticas que reinicien contenedores mientras alguien pueda estar usando la aplicación.

### Primer despliegue desde cero

**1. Conectar al servidor:**

```bash
ssh root@<IP_DEL_DROPLET>
```

**2. Clonar el repositorio:**

```bash
git clone https://github.com/Aranaaa00/actacofrade.git
cd actacofrade
```

**3. Crear el fichero de configuración:**

```bash
cp .env.example .env
nano .env
```

Hay que rellenar estas variables antes del primer arranque. Sin ellas el sistema no funciona:

```
POSTGRES_PASSWORD=<contraseña_segura>
DB_PASSWORD=<mismo_valor_que_POSTGRES_PASSWORD>
JWT_SECRET=<cadena_aleatoria_minimo_32_bytes>
CORS_ALLOWED_ORIGINS=https://actacofrade.com,https://www.actacofrade.com
SUPERADMIN_EMAIL=<email_del_superadmin>
SUPERADMIN_PASSWORD=<contraseña_del_superadmin>
SUPERADMIN_FULL_NAME=<nombre_del_superadmin>
```

El `JWT_SECRET` se genera fácilmente con:

```bash
openssl rand -base64 48
```

El `.env` nunca se sube al repositorio. Está en el `.gitignore` y los secretos se quedan solo en el servidor.

**4. Levantar el stack:**

```bash
docker compose up -d --build
```

Este es el comando principal. Construye las imágenes a partir del código, crea los tres contenedores y los levanta en segundo plano. La primera vez lleva varios minutos porque Maven descarga las dependencias del backend y npm instala los paquetes del frontend. Las siguientes veces es mucho más rápido porque Docker aprovecha la caché.

![Ejecución de docker compose up -d --build](/docs/assets/deploy-compose-up.png)

**5. Comprobar que todo arrancó bien:**

```bash
docker compose ps
```

Los tres servicios tienen que estar `healthy`:

```
NAME                   STATUS
actacofrade_db         Up X seconds (healthy)
actacofrade_backend    Up X seconds (healthy)
actacofrade_frontend   Up X seconds (healthy)
```

El backend necesita entre 30 y 60 segundos para arrancar porque Spring Boot levanta el contexto completo y Flyway aplica las migraciones de base de datos. Si aparece como `starting` es normal, hay que esperar y repetir el comando.

![Salida de docker compose ps con los tres servicios healthy](/docs/assets/deploy-compose-ps.png)

**6. Revisar los logs:**

```bash
docker compose logs --tail=50 backend
```

Hay que buscar la línea `Started BackendApplication in X seconds` y el listado de migraciones de Flyway. Si aparecen, todo está bien:

```
Found 17 migration(s) resolved to the target version.
Successfully applied 17 migration(s) ...
Started BackendApplication in 18.432 seconds
```

![Logs del backend con Flyway y arranque de Spring Boot](/docs/assets/deploy-logs-backend.png)

---

### Cómo se actualiza el servidor

Cuando hay una nueva versión publicada en Docker Hub, actualizar el servidor es tan simple como:

```bash
cd actacofrade
git pull
docker compose up -d --build
```

Docker Compose detecta qué contenedores han cambiado y solo reconstruye esos. Los datos de la base de datos no se tocan porque viven en el volumen `actacofrade_pg_data`, que es completamente independiente del ciclo de vida de los contenedores.

---

## Comprobaciones post-despliegue

Una vez arrancado el stack, hago estas verificaciones rápidas para confirmar que todo llega bien desde fuera:

```bash
# El frontend responde
curl -I http://actacofrade.com

# La API responde a través del reverse proxy
curl -fsS https://www.actacofrade.com/v3/api-docs | head -c 200

# El backend NO debe ser accesible directamente desde fuera
curl --max-time 3 -I http://<IP_DEL_DROPLET>:8080/

# Smoke test: que el login funciona
curl -s -X POST https://www.actacofrade.com/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@actacofrade.com","password":"TU_PASSWORD"}'
```

El tercer comando tiene que fallar. El backend no publica ningún puerto al exterior: solo es accesible desde dentro de la red interna de Docker, a través del proxy inverso de Nginx. Eso es exactamente lo que se busca.

---

## Servidor web: Nginx como reverse proxy

Toda la configuración de Nginx está en un único fichero: [`frontend/nginx.conf`](frontend/nginx.conf). El Dockerfile lo copia a `/etc/nginx/conf.d/default.conf` en el momento del build, así que no hay que tocar nada en el servidor para que quede configurado correctamente. Lo primero que declara el fichero es el bloque `upstream` con el backend:

```nginx
upstream actacofrade_backend {
    server backend:8080;
    keepalive 32;
}
```

`backend` es el nombre del servicio en el compose; el DNS interno de la red `actacofrade_network` lo resuelve automáticamente a la IP del contenedor. El `keepalive 32` mantiene un pool de hasta 32 conexiones TCP abiertas y reutilizables entre Nginx y el backend, evitando el coste de abrir y cerrar una nueva conexión TCP en cada petición proxiada.

### Rutas que van al backend

Cuatro bloques `location` desvían el tráfico al backend; todo lo demás lo sirve Nginx directamente desde el build de Angular:

```nginx
location /api/ {
    proxy_pass http://actacofrade_backend;
}

location /v3/api-docs {
    proxy_pass http://actacofrade_backend;
}

location /swagger-ui {
    proxy_pass http://actacofrade_backend;
}

location = /swagger-ui.html {
    proxy_pass http://actacofrade_backend;
}
```

El SPA routing funciona gracias a `try_files $uri $uri/ /index.html`: cualquier ruta de Angular que no exista como fichero estático vuelve a `index.html` y el router del cliente la resuelve en el navegador.

### Cabeceras de seguridad, caché y compresión

Nginx añade en cada respuesta un conjunto de cabeceras defensivas con la directiva `always`, de forma que se emiten incluso en respuestas de error:

```nginx
add_header X-Frame-Options          "DENY"                            always;
add_header X-Content-Type-Options   "nosniff"                         always;
add_header Referrer-Policy          "strict-origin-when-cross-origin" always;
add_header Permissions-Policy       "geolocation=(), microphone=(), camera=()" always;
add_header Cross-Origin-Opener-Policy   "same-origin"                 always;
add_header Cross-Origin-Resource-Policy "same-origin"                 always;
```

`server_tokens off` elimina la versión de Nginx de la cabecera `Server`. Los ficheros estáticos con hash en el nombre (todos los `.js` y `.css` que genera Angular en producción) llevan `Cache-Control: public, immutable` con un año de expiración. El `index.html` siempre va con `no-store` para que el navegador pida la versión más reciente en cada visita. La compresión gzip está activa sobre los tipos de texto habituales (`text/css`, `application/json`, `application/javascript`...), lo que reduce considerablemente el peso de transferencia de los assets estáticos.

### HTTPS

El stack no termina TLS internamente. En producción, Cloudflare actúa como CDN delante del Droplet: recibe las peticiones HTTPS del cliente, las descifra y las reenvía al puerto 80 del servidor. La conexión cliente ↔ Cloudflare va cifrada con certificado de Cloudflare; la conexión Cloudflare ↔ Droplet también va cifrada en modo "Full". El resultado es que actacofrade.com funciona completamente en HTTPS sin gestionar ningún certificado en el servidor.

Para activar TLS directamente en Nginx (sin Cloudflare o detrás de otro terminador), al final del `nginx.conf` hay un bloque `server { listen 443 ssl http2; ... }` comentado con cifrados modernos (`TLSv1.2 + TLSv1.3`), cabecera `HSTS` con preload y redirección HTTP→HTTPS. Activarlo solo requiere tres pasos: montar el certificado en un volumen, publicar el puerto 443 en el compose y descomentar ese bloque.

### Evidencias

Petición al frontend a través del proxy:

```bash
curl -I https://www.actacofrade.com/
```

```
HTTP/1.1 200 OK
Content-Type: text/html
Cache-Control: no-store, no-cache, must-revalidate
```

La API llega a través del proxy (el backend no tiene ningún puerto publicado en el host):

```bash
curl -I https://www.actacofrade.com/v3/api-docs
```

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```bash
curl -I https://www.actacofrade.com/swagger-ui.html
```

```
HTTP/1.1 302 Found
Location: /swagger-ui/index.html
```

El endpoint de salud del proxy responde de forma independiente al backend:

```bash
curl https://www.actacofrade.com/healthz
```

```
ok
```

![Respuesta de curl al frontend y a la API a través del proxy](/docs/assets/deploy-curl-frontend.png)

**Logs del proxy con peticiones reales:**

```bash
docker compose logs --tail=20 frontend
```

Nginx escribe su `access_log` en `/dev/stdout`, así que `docker compose logs` captura todo el tráfico en tiempo real. Una sesión típica muestra una línea por petición:

```
172.68.x.x - - [..] "GET / HTTP/1.0" 200 689 "-" "Mozilla/5.0 ..."
172.68.x.x - - [..] "GET /api/dashboard HTTP/1.0" 200 423 "-" "..."
172.68.x.x - - [..] "HEAD /v3/api-docs HTTP/1.1" 200 0 "-" "curl/8.x"
172.68.x.x - - [..] "GET /healthz HTTP/1.1" 200 2 "-" "curl/8.x"
```

---

## Servidor de aplicaciones: Spring Boot

El backend es un JAR de Spring Boot que arranca dentro del contenedor `actacofrade_backend` con `java -jar app.jar`. No necesita ningún servidor de aplicaciones externo: Tomcat está embebido en el propio JAR. El contenedor usa `expose: 8080` (no `ports`), así que el puerto solo es visible desde dentro de la red Docker interna, nunca desde el host.

### Configuración

Toda la configuración está en [`backend/src/main/resources/application.properties`](backend/src/main/resources/application.properties). Los valores sensibles se inyectan desde las variables de entorno definidas en el compose; el fichero solo declara los valores por defecto para desarrollo local:

```properties
# Conexión a base de datos
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/actacofrade}
spring.datasource.username=${DB_USER:actacofrade_user}
spring.datasource.password=${DB_PASSWORD:actacofrade_password}

# Flyway gestiona el esquema; Hibernate solo valida, nunca modifica
spring.jpa.hibernate.ddl-auto=validate

# JWT (el secreto se inyecta siempre desde .env, sin valor por defecto)
jwt.secret=${JWT_SECRET}
jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}

# CORS
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:4200}

# Subida de avatares
spring.servlet.multipart.max-file-size=${AVATAR_MAX_SIZE:2MB}
spring.servlet.multipart.max-request-size=${AVATAR_MAX_SIZE:2MB}

# OpenAPI / Swagger UI
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.tryItOutEnabled=true
```

`spring.jpa.hibernate.ddl-auto=validate` es la opción más segura en producción: Hibernate comprueba que el esquema real de la base de datos coincide con las entidades, pero no toca nada. Cualquier cambio estructural pasa siempre por Flyway, que aplica las migraciones pendientes antes de que Spring levante el contexto completo. Con 17 migraciones acumuladas, este diseño garantiza que cualquier entorno —local, CI o producción— parte siempre del mismo estado exacto sin intervención manual.

El `JWT_SECRET` no tiene ningún valor por defecto a propósito: si no se proporciona, el backend rechaza arrancar. Esto evita que alguien levante la aplicación en producción con un secreto de demo olvidado.

### Arranque y logs

Los primeros logs del backend siempre muestran las migraciones de Flyway seguidas del mensaje de arranque de Spring Boot:

```
Found 17 migration(s) resolved to the target version.
Successfully applied 3 migration(s) to schema "public" (execution time 00:00.234s)
Started BackendApplication in 18.432 seconds (process running for 20.1)
```

Si hay algún problema de configuración —`JWT_SECRET` demasiado corto, credenciales de base de datos incorrectas— el arranque falla antes de llegar a esa línea y el healthcheck de Docker marca el servicio como `unhealthy`.

![Logs del backend con Flyway y arranque de Spring Boot](/docs/assets/deploy-logs-backend.png)

### Prueba de funcionamiento

Con el stack levantado, el documento OpenAPI confirma que el backend responde a través del proxy:

```bash
curl -fsS https://www.actacofrade.com/v3/api-docs | head -c 200
```

```json
{"openapi":"3.1.0","info":{"title":"ActaCofrade Backend API","description":"API REST para la gestión de actos cofrades...
```

El flujo completo de autenticación:

```bash
curl -s -X POST https://www.actacofrade.com/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@actacofrade.com","password":"TU_PASSWORD"}'
```

```json
{
  "userId": 1,
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "admin@actacofrade.com",
  "fullName": "Admin ActaCofrade",
  "roles": ["ADMINISTRADOR"],
  "hermandadNombre": "Hermandad de Prueba",
  "hasAvatar": false
}
```

### Prueba de carga ligera

Para confirmar que el servidor aguanta peticiones repetidas sin degradarse, lancé 50 peticiones seguidas al documento OpenAPI midiendo el código de respuesta y el tiempo de cada una:

```bash
for i in $(seq 1 50); do
  curl -s -o /dev/null -w "%{http_code} %{time_total}s\n" \
       https://www.actacofrade.com/v3/api-docs
done
```

Resultado en producción (Droplet, 2 GB RAM, CPU básica compartida):

```
200 0.048s
200 0.041s
200 0.039s
200 0.044s
...
200 0.053s
```

Las 50 peticiones devolvieron `200` y los tiempos se mantuvieron entre 35 y 60 ms. Spring Boot usa Tomcat embebido con un pool de 200 hilos por defecto; para una carga de este nivel no hay ningún tipo de saturación. El primer arranque en frío puede tardar entre 15 y 20 segundos (Flyway aplica las migraciones y Spring levanta todo el contexto), pero una vez iniciado los tiempos de respuesta son completamente estables.

![Salida del bucle de 50 peticiones con tiempos de respuesta](/docs/assets/deploy-carga-ligera.png)

---

## La aplicación en producción

La aplicación está disponible en: **[ActaCofrade](https://www.actacofrade.com)**

![Landing page de ActaCofrade en producción](/docs/assets/deploy-landing-produccion.png)
