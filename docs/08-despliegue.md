# 8. Despliegue

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

La idea es sencilla: dos workflows de CI comprueban que el código compila y los tests pasan, y un tercero de CD construye las imágenes y las publica en **Docker Hub**. Solo cuando el código está validado llega algo a producción.

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

## La aplicación en producción

La aplicación está disponible en: **[ActaCofrade](https://www.actacofrade.com)**

![Landing page de ActaCofrade en producción](/docs/assets/deploy-landing-produccion.png)
