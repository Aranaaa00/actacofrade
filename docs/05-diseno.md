# 5. Diseño

## Índice

- [Diagrama entidad-relación](#diagrama-entidad-relación)
- [Diagrama de casos de uso](#diagrama-de-casos-de-uso)
- [Diagramas de flujo](#diagramas-de-flujo)
- [Arquitectura de la aplicación](#arquitectura-de-la-aplicación)
- [Diseño de la API](#diseño-de-la-api)

---

## Diagrama entidad-relación

El esquema de la base de datos suma doce tablas que han ido creciendo a lo largo de diecisiete migraciones de Flyway. Todo gira en torno a dos ejes: la hermandad como unidad organizativa y el acto como elemento central de trabajo. El diagrama se generó con [dbdiagram.io](https://dbdiagram.io) a partir del esquema real de PostgreSQL.

![Diagrama entidad-relación de la base de datos](/docs/assets/diagrama-er.png)

---

## Diagrama de casos de uso

La aplicación tiene seis actores organizados en jerarquía de herencia: cada rol acumula las capacidades del anterior, desde el Visitante sin autenticar hasta el Administrador con control total. El Super Admin es independiente y solo opera a nivel global con las solicitudes de cambio de administrador. El diagrama se elaboró con [draw.io](https://app.diagrams.net).

![Diagrama de casos de uso](/docs/assets/diagrama-casos-uso.png)

---

## Diagramas de flujo

Se han elaborado tres diagramas para los procesos más relevantes del sistema: el ciclo de vida de una tarea, el flujo de cierre de un acto y la autenticación JWT. Los tres se generaron con [mermaid.live](https://mermaid.live).

### Ciclo de vida de una tarea

![Diagrama de flujo — ciclo de vida de una tarea](/docs/assets/diagrama-flujo-tarea.png)

### Flujo de cierre de un acto

![Diagrama de flujo — cierre de un acto](/docs/assets/diagrama-flujo-cierre.png)

### Flujo de autenticación

![Diagrama de flujo — autenticación JWT](/docs/assets/diagrama-flujo-auth.png)

---

## Arquitectura de la aplicación

### Frontend — Angular + nginx

La SPA está construida con Angular 19 en modo standalone, sin `NgModules`. Cada componente declara sus propias dependencias en el array `imports`, lo que hace que cada fichero sea autocontenido. El routing usa lazy loading en todas las rutas para que el bundle inicial sea lo más pequeño posible y la carga inicial sea rápida incluso en conexiones lentas.

nginx sirve los ficheros estáticos compilados y actúa al mismo tiempo como reverse proxy: redirige las peticiones a `/api/`, `/swagger-ui/` y `/v3/api-docs/` hacia el contenedor del backend a través de la red interna de Docker. El `nginx.conf` también configura las cabeceras de seguridad HTTP que se añaden en cada respuesta: `X-Frame-Options` para bloquear ataques de clickjacking, `X-Content-Type-Options` para que el navegador no intente adivinar el tipo MIME del contenido, `Referrer-Policy` para controlar qué información se filtra en las cabeceras de referencia y `Permissions-Policy` para deshabilitar explícitamente el acceso a APIs sensibles del navegador como geolocalización o micrófono. Además habilita compresión gzip, lo que reduce considerablemente el peso de transferencia de los assets estáticos.

Este contenedor es el **único punto de entrada desde el exterior**: expone el puerto 80 del host. El backend ni siquiera tiene un puerto mapeado, solo un `expose: 8080` interno que únicamente es visible dentro de la red Docker. Esto garantiza que la API no sea alcanzable directamente desde fuera del stack bajo ninguna circunstancia.

### Backend — Spring Boot + Java 21

API REST completamente stateless construida con Spring Boot 4 sobre Java 21. Spring Security está configurado en modo `STATELESS`: no se emiten cookies ni se mantiene ningún estado de sesión en el servidor. Cada petición llega con un token JWT en la cabecera `Authorization`, el filtro `JwtAuthenticationFilter` lo valida y reconstituye el contexto de seguridad para esa petición concreta.

El origen CORS permitido se inyecta desde la variable de entorno `CORS_ALLOWED_ORIGINS`, por lo que el mismo binario funciona sin cambios tanto en desarrollo local como en producción. Las contraseñas se almacenan cifradas con BCrypt. El endpoint de login tiene un rate limiter propio (`LoginRateLimiter`) que bloquea una clave `ip:email` durante 15 minutos tras cinco intentos fallidos en una ventana de cinco minutos.

El esquema de la base de datos lo gestiona **Flyway** automáticamente en cada arranque. Cada cambio estructural es un fichero SQL versionado en `db/migration`. A lo largo del proyecto se acumularon 17 migraciones: desde el esquema inicial con los enums de PostgreSQL hasta la adición del sistema de solicitudes de cambio de administrador o las cascadas de borrado. Flyway aplica en orden las migraciones pendientes antes de que la aplicación arranque, por lo que cualquier entorno nuevo parte siempre del mismo estado exacto sin intervención manual.

La lógica de autorización contextual se centraliza en el componente `AuthorizationHelper`, que encapsula las comprobaciones de permisos más complejas: si un usuario puede gestionar un acto concreto, si tiene el rol necesario para aceptar una tarea, si puede ser asignado o no. Cualquier fallo lanza `AccessDeniedException`, que el `GlobalExceptionHandler` convierte en un `403 Forbidden` con la estructura de error estándar.

### Base de datos — PostgreSQL 15

El contenedor de base de datos no expone ningún puerto al host y solo es accesible desde el backend a través de la red interna de Docker. Los estados y categorías del dominio —tipos de acto, estados de tareas, estados de decisiones e incidencias, áreas de la hermandad— están definidos como enums nativos de PostgreSQL. Esto garantiza la integridad referencial a nivel de base de datos sin depender únicamente de las validaciones de la aplicación.

### Arranque y orden de dependencias

El orden de arranque lo controlan los healthchecks encadenados del `docker-compose.yml`. La base de datos tiene un healthcheck con `pg_isready` que el backend espera antes de arrancar. El backend a su vez tiene un healthcheck con `nc -z localhost 8080` que el frontend espera antes de iniciarse. Esto evita los errores de conexión en frío que aparecen inevitablemente cuando los contenedores se levantan en paralelo sin coordinación.

---

## Diseño de la API

La estructura de la API respeta el estilo REST: los recursos que tienen sentido de forma independiente tienen rutas propias, y los que pertenecen siempre a un acto —tareas, decisiones, incidencias, historial— se anidan bajo él. El prefijo de todas las rutas es `/api`. La documentación interactiva la genera SpringDoc a partir de las anotaciones OpenAPI del código y está disponible en `/swagger-ui/` con el stack levantado, lo que facilita mucho probar los endpoints sin necesidad de un cliente externo.

Todas las respuestas de error —validación, recurso no encontrado, conflicto, acceso denegado, error interno— salen del `GlobalExceptionHandler` con la misma estructura JSON:

```json
{
  "status": "error",
  "message": "Descripción del error",
  "data": null,
  "errors": []
}
```

Esto evita que el frontend tenga que tratar formatos distintos según el tipo de error.

### Autenticación — `/api/auth`

Únicos endpoints accesibles sin token. Son los que el usuario toca antes de tener sesión: registro, login y la lista de hermandades para el formulario de registro.

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `POST` | `/api/auth/register` | Registra un usuario. Si el rol es `ADMINISTRADOR` y la hermandad no existe, la crea en el mismo paso. Devuelve un JWT inmediatamente. | `201 Created` → `AuthResponse` |
| `POST` | `/api/auth/login` | Autentica con email y contraseña. Rate limiter activo: 5 intentos en 5 minutos, bloqueo de 15 minutos. | `200 OK` → `AuthResponse` |
| `GET` | `/api/auth/hermandades` | Lista las hermandades existentes para que el usuario no administrador pueda seleccionar la suya al registrarse. | `200 OK` → `HermandadOption[]` |

### Usuario autenticado — `/api/me`

Operaciones del usuario sobre su propia cuenta. No requieren ningún rol concreto más allá de estar autenticado. El avatar se almacena en base de datos como array de bytes y se sirve directamente desde aquí con el Content-Type correcto.

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `GET` | `/api/me` | Devuelve el perfil completo del usuario autenticado. | `200 OK` → `UserResponse` |
| `PUT` | `/api/me` | Actualiza nombre o email del usuario. | `200 OK` → `UserResponse` |
| `DELETE` | `/api/me` | Elimina la cuenta del usuario autenticado. | `204 No Content` |
| `PATCH` | `/api/me/password` | Cambia la contraseña. Requiere enviar la contraseña actual para confirmar. | `204 No Content` |
| `POST` | `/api/me/avatar` | Sube o reemplaza el avatar (`multipart/form-data`, máx. 2 MB, formatos: PNG, JPEG, WebP, GIF). | `200 OK` → `UserResponse` |
| `DELETE` | `/api/me/avatar` | Elimina el avatar del usuario. | `204 No Content` |
| `GET` | `/api/me/avatar/{userId}` | Descarga el avatar de un usuario de la misma hermandad. | `200 OK` → `image/*` |

### Dashboard — `/api/dashboard`

Un único endpoint que agrega las métricas relevantes para la hermandad del usuario autenticado: cuántos actos están activos, cuántas tareas siguen sin aceptar, cuántas decisiones están pendientes de revisión y cuántas incidencias siguen abiertas. El frontend lo usa para construir los contadores de la pantalla de inicio.

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `GET` | `/api/dashboard` | Métricas agregadas de la hermandad: actos activos, tareas sin aceptar, decisiones sin revisar e incidencias abiertas. | `200 OK` → `DashboardResponse` |

### Actos — `/api/events`

El recurso central de la aplicación. Los endpoints de lectura están disponibles para todos los roles; los de escritura y transición de estado requieren al menos Responsable o Administrador.

| Método | Ruta | Descripción | Roles mínimos | Respuesta |
|--------|------|-------------|---------------|-----------|
| `GET` | `/api/events` | Lista todos los actos visibles para el usuario autenticado. | Todos | `200 OK` → `EventResponse[]` |
| `GET` | `/api/events/filter` | Filtra y pagina actos no cerrados por tipo, estado, fecha y texto libre (cruza título, referencia y ubicación). Usa JPA Specifications para combinar filtros libremente. | Todos | `200 OK` → `Page<EventResponse>` |
| `GET` | `/api/events/history` | Histórico paginado de actos cerrados con filtros por tipo, responsable y rango de fechas. | Todos | `200 OK` → `Page<EventResponse>` |
| `GET` | `/api/events/available-dates` | Devuelve las fechas con al menos un acto programado. Lo usa el selector de fecha del frontend para resaltar los días con eventos. | Todos | `200 OK` → `String[]` |
| `GET` | `/api/events/{id}` | Detalle completo de un acto por su ID. | Todos | `200 OK` → `EventResponse` |
| `POST` | `/api/events` | Crea un nuevo acto. | Admin, Responsable | `201 Created` → `EventResponse` |
| `PUT` | `/api/events/{id}` | Actualiza los datos de un acto existente. | Admin, Responsable | `200 OK` → `EventResponse` |
| `DELETE` | `/api/events/{id}` | Elimina un acto de forma permanente con todas sus tareas, decisiones e incidencias. | Admin | `204 No Content` |
| `PATCH` | `/api/events/{id}/advance-status` | Avanza el acto al siguiente estado del flujo: Planificación → Preparación → Confirmación → Cierre. | Admin, Responsable | `200 OK` → `EventResponse` |
| `PATCH` | `/api/events/{id}/close` | Cierra definitivamente el acto tras verificar que no quedan elementos pendientes. Falla si hay tareas, decisiones o incidencias sin resolver. | Admin, Responsable | `200 OK` → `EventResponse` |
| `PATCH` | `/api/events/{id}/toggle-lock` | Bloquea o desbloquea el acto para iniciar el proceso de cierre. Cuando está bloqueado no se pueden añadir nuevos elementos. | Admin, Responsable | `200 OK` → `EventResponse` |
| `POST` | `/api/events/{id}/clone` | Clona el acto y crea uno nuevo en estado `PLANNING` copiando la estructura de tareas. Útil para reutilizar la organización de actos recurrentes. | Admin, Responsable | `201 Created` → `EventResponse` |
| `POST` | `/api/events/{id}/export` | Exporta el acto a PDF o CSV. El cuerpo de la petición indica el formato y las secciones a incluir (tareas, decisiones, incidencias, historial). | Admin, Responsable, Colaborador | `200 OK` → `application/pdf` o `text/csv` |

### Tareas — `/api/events/{eventId}/tasks`

Las tareas se anidan bajo el acto al que pertenecen. Cada tarea sigue un ciclo de estados propio que los miembros avanzan de forma explícita. Los Colaboradores solo pueden asignarse tareas a sí mismos.

| Método | Ruta | Descripción | Roles mínimos | Respuesta |
|--------|------|-------------|---------------|-----------|
| `GET` | `.../tasks` | Lista todas las tareas del acto. | Todos | `200 OK` → `TaskResponse[]` |
| `GET` | `.../tasks/{taskId}` | Detalle de una tarea por ID. | Todos | `200 OK` → `TaskResponse` |
| `POST` | `.../tasks` | Crea una tarea. Los Colaboradores solo pueden asignársela a sí mismos. | Admin, Responsable, Colaborador | `201 Created` → `TaskResponse` |
| `PUT` | `.../tasks/{taskId}` | Actualiza los datos de una tarea. | Admin, Responsable, Colaborador | `200 OK` → `TaskResponse` |
| `DELETE` | `.../tasks/{taskId}` | Elimina una tarea. | Admin, Responsable | `204 No Content` |
| `PATCH` | `.../tasks/{taskId}/accept` | El responsable asignado acepta la tarea → `ACCEPTED`. | Admin, Responsable | `200 OK` → `TaskResponse` |
| `PATCH` | `.../tasks/{taskId}/start-preparation` | Inicia la preparación → `IN_PREPARATION`. | Admin, Responsable, Colaborador | `200 OK` → `TaskResponse` |
| `PATCH` | `.../tasks/{taskId}/confirm` | Confirma que la tarea está lista para revisión → `CONFIRMED`. | Admin, Responsable, Colaborador | `200 OK` → `TaskResponse` |
| `PATCH` | `.../tasks/{taskId}/complete` | Marca la tarea como completada → `COMPLETED`. | Admin, Responsable, Colaborador | `200 OK` → `TaskResponse` |
| `PATCH` | `.../tasks/{taskId}/reject` | Rechaza la tarea. El motivo escrito es obligatorio y queda registrado → `REJECTED`. | Admin, Responsable | `200 OK` → `TaskResponse` |
| `PATCH` | `.../tasks/{taskId}/reset` | Resetea la tarea a `PLANNED` para reiniciar el ciclo desde el principio. | Admin, Responsable | `200 OK` → `TaskResponse` |

### Decisiones — `/api/events/{eventId}/decisions`

Las decisiones también son recursos anidados bajo el acto. El flujo es más sencillo que el de las tareas: se registran como pendientes y después se marcan como aceptadas o rechazadas por un Administrador o Responsable.

| Método | Ruta | Descripción | Roles mínimos | Respuesta |
|--------|------|-------------|---------------|-----------|
| `GET` | `.../decisions` | Lista todas las decisiones del acto. | Todos | `200 OK` → `DecisionResponse[]` |
| `GET` | `.../decisions/{decisionId}` | Detalle de una decisión por ID. | Todos | `200 OK` → `DecisionResponse` |
| `POST` | `.../decisions` | Registra una decisión indicando su área y título. | Admin, Responsable, Colaborador | `201 Created` → `DecisionResponse` |
| `PUT` | `.../decisions/{decisionId}` | Actualiza los datos de una decisión. | Admin, Responsable | `200 OK` → `DecisionResponse` |
| `DELETE` | `.../decisions/{decisionId}` | Elimina una decisión. | Admin, Responsable | `204 No Content` |
| `PATCH` | `.../decisions/{decisionId}/accept` | Marca la decisión como `ACEPTADA`. | Admin, Responsable | `200 OK` → `DecisionResponse` |
| `PATCH` | `.../decisions/{decisionId}/reject` | Marca la decisión como `RECHAZADA`. | Admin, Responsable | `200 OK` → `DecisionResponse` |

### Incidencias — `/api/events/{eventId}/incidents`

Las incidencias recogen cualquier problema o imprevisto que surja durante la organización del acto. Solo tienen dos estados: abiertas o resueltas. Las incidencias abiertas bloquean el cierre del acto.

| Método | Ruta | Descripción | Roles mínimos | Respuesta |
|--------|------|-------------|---------------|-----------|
| `GET` | `.../incidents` | Lista todas las incidencias del acto. | Todos | `200 OK` → `IncidentResponse[]` |
| `GET` | `.../incidents/{incidentId}` | Detalle de una incidencia por ID. | Todos | `200 OK` → `IncidentResponse` |
| `POST` | `.../incidents` | Registra una incidencia con título y descripción. | Admin, Responsable, Colaborador | `201 Created` → `IncidentResponse` |
| `DELETE` | `.../incidents/{incidentId}` | Elimina una incidencia. | Admin, Responsable | `204 No Content` |
| `PATCH` | `.../incidents/{incidentId}/resolve` | Marca la incidencia como resuelta → `RESOLVED`. | Admin, Responsable | `200 OK` → `IncidentResponse` |
| `PATCH` | `.../incidents/{incidentId}/reopen` | Reabre una incidencia ya resuelta → `OPEN`. | Admin, Responsable | `200 OK` → `IncidentResponse` |

### Historial de auditoría — `/api/events/{eventId}/history`

Cada acción relevante sobre el acto y sus elementos queda registrada automáticamente en el log de auditoría: quién la realizó, qué cambió y cuándo. El historial es de solo lectura y se devuelve paginado en orden cronológico.

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `GET` | `.../history` | Log de auditoría del acto paginado en orden cronológico. | `200 OK` → `Page<AuditLogResponse>` |

### Mis tareas — `/api/my-tasks`

Vista personal que agrega todas las tareas asignadas al usuario autenticado en cualquier acto de la hermandad, con filtros y paginación. Es el punto desde el que un colaborador puede ver su carga de trabajo sin tener que entrar acto por acto.

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `GET` | `/api/my-tasks` | Lista las tareas propias con filtros por tipo de acto, estado y texto libre. Paginado. | `200 OK` → `Page<MyTaskResponse>` |
| `GET` | `/api/my-tasks/stats` | Contadores de las tareas propias agrupados por estado. | `200 OK` → `MyTaskStatsResponse` |

### Usuarios — `/api/users`

Gestión de los miembros de la hermandad. La mayor parte de endpoints requiere rol Administrador. El endpoint `/assignable` es la excepción: también está disponible para Responsables, porque necesitan ver la lista al asignar tareas.

| Método | Ruta | Descripción | Roles | Respuesta |
|--------|------|-------------|-------|-----------|
| `GET` | `/api/users` | Lista todos los usuarios de la hermandad del administrador autenticado. | Admin | `200 OK` → `UserResponse[]` |
| `POST` | `/api/users` | Crea un usuario directamente en la hermandad, sin pasar por el registro público. | Admin | `201 Created` → `UserResponse` |
| `GET` | `/api/users/assignable` | Lista los usuarios que pueden ser asignados a tareas (excluye rol Consulta). | Admin, Responsable | `200 OK` → `UserResponse[]` |
| `GET` | `/api/users/{id}` | Obtiene el perfil de un usuario por ID. | Admin | `200 OK` → `UserResponse` |
| `GET` | `/api/users/stats` | Conteo de usuarios de la hermandad agrupado por rol. | Admin | `200 OK` → `RoleStatsResponse` |
| `PUT` | `/api/users/{id}` | Actualiza nombre, email o rol de un usuario. | Admin | `200 OK` → `UserResponse` |
| `PATCH` | `/api/users/{id}/toggle-active` | Activa o desactiva un usuario (borrado lógico). El usuario desactivado no puede iniciar sesión. | Admin | `200 OK` → `UserResponse` |
| `DELETE` | `/api/users/{id}` | Elimina un usuario permanentemente. | Admin | `204 No Content` |

### Hermandad — `/api/hermandades`

Cada usuario pertenece a exactamente una hermandad, y estos tres endpoints cubren todo lo que hay que hacer con ella: consultarla, actualizarla o eliminarla por completo. La lectura está abierta a todos los roles porque cualquier miembro puede necesitar consultar los datos de su hermandad; la escritura y el borrado son exclusivos del Administrador. El borrado en particular es irreversible y elimina en cascada todos los usuarios, actos y datos asociados.

| Método | Ruta | Descripción | Roles | Respuesta |
|--------|------|-------------|-------|-----------|
| `GET` | `/api/hermandades/me` | Devuelve los datos de la hermandad del usuario autenticado. | Todos | `200 OK` → `HermandadResponse` |
| `PUT` | `/api/hermandades/me` | Actualiza los datos y la configuración de la hermandad. | Admin | `200 OK` → `HermandadResponse` |
| `DELETE` | `/api/hermandades/me` | Elimina la hermandad y todos sus datos de forma permanente e irreversible. | Admin | `204 No Content` |

### Roles — `/api/roles`

Solo un endpoint, pero lo suficientemente útil como para merecer su propio recurso. El catálogo de roles es fijo —Administrador, Responsable, Colaborador y Consulta— y el frontend lo consume para construir los selectores de rol en los formularios de creación y edición de usuarios, sin tener que hardcodear los valores en el cliente.

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `GET` | `/api/roles` | Devuelve el catálogo de roles disponibles para asignar a usuarios. | `200 OK` → `RoleResponse[]` |

### Solicitudes de cambio de administrador — `/api/admin-change-requests`

Este módulo resuelve un caso concreto: qué pasa cuando el administrador de una hermandad quiere transferir ese rol a otro miembro. La solicitud la crea el propio administrador con un mensaje justificativo, y la aprobación o el rechazo los gestiona el Super Administrador del sistema, que es independiente de cualquier hermandad y existe únicamente para este tipo de operaciones globales.

| Método | Ruta | Descripción | Roles | Respuesta |
|--------|------|-------------|-------|-----------|
| `POST` | `/api/admin-change-requests` | Crea una solicitud de cambio con un mensaje justificativo. | Autenticado (no Super Admin) | `201 Created` → `AdminChangeRequestResponse` |
| `GET` | `/api/admin-change-requests` | Lista todas las solicitudes, tanto pendientes como ya resueltas. | Super Admin | `200 OK` → `AdminChangeRequestResponse[]` |
| `GET` | `/api/admin-change-requests/{id}` | Detalle de una solicitud por ID. | Super Admin | `200 OK` → `AdminChangeRequestResponse` |
| `GET` | `/api/admin-change-requests/{id}/candidates` | Lista los usuarios de la hermandad que pueden recibir el rol de administrador. | Super Admin | `200 OK` → `UserResponse[]` |
| `PATCH` | `/api/admin-change-requests/{id}/approve` | Aprueba la solicitud y transfiere el rol al usuario seleccionado. | Super Admin | `200 OK` → `AdminChangeRequestResponse` |
| `PATCH` | `/api/admin-change-requests/{id}/reject` | Rechaza la solicitud. | Super Admin | `200 OK` → `AdminChangeRequestResponse` |
