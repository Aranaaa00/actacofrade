# 6. Desarrollo

## Secuencia de desarrollo

El proyecto no arrancó directamente con código. Antes de tocar el editor necesitaba tener claro cómo iba a quedar la interfaz, así que la primera fase fue dedicada íntegramente a Figma. Diseñé todas las pantallas principales: el dashboard, el listado de actos con filtros, el detalle del acto con sus pestañas, el formulario de creación y el flujo de cierre. Tener ese diseño resuelto de antemano me ahorró muchas idas y venidas después.

Con las pantallas claras, lo siguiente fue montar la infraestructura. Definí el `docker-compose.yml` con los tres servicios (base de datos, backend y frontend), los healthchecks encadenados para que cada contenedor esperase al anterior, y la lógica de que el frontend fuera el único punto de entrada al exterior, haciendo de reverse proxy hacia el backend a través de nginx. Quería que desde el principio el entorno de desarrollo y el de producción fueran lo mismo, sin sorpresas en el despliegue.

Una vez levantado el stack, empecé con el backend. El primer bloque fue el de seguridad y usuarios: el esquema inicial de la base de datos con Flyway, la entidad `User`, los roles, la autenticación JWT con `JwtAuthenticationFilter` y el sistema de registro y login. Sin esto no había nada más que hacer, porque todo lo demás dependía de que existiera un usuario autenticado con un rol claro.

Con la autenticación funcionando pasé al frontend. Monté la estructura base de la SPA en Angular: las rutas con lazy loading, los guards de autenticación y de rol, el interceptor que inyecta el token en cada petición, y las primeras páginas estáticas (landing, login, registro). En este punto ya podía registrarme, iniciar sesión y navegar entre rutas protegidas.

Desde ahí el desarrollo alternó entre backend y frontend en ciclos cortos. Por el lado del backend fui añadiendo los servicios de eventos, tareas, decisiones e incidencias, con toda la lógica de autorización y los flujos de estado. Por el lado del frontend fui construyendo las páginas que consumían esos servicios: el dashboard, el listado de actos con filtros y buscador, el detalle del acto con sus cuatro pestañas, el formulario de elementos y el flujo de cierre.

Una vez que la funcionalidad principal estaba cerrada, me centré en los tests de integración del backend. Escribí tests para todos los controllers con `@SpringBootTest` y un contexto de base de datos real, cubriendo los flujos principales y los casos de error esperados.

La última fase fue el despliegue en Digital Ocean. Creé un Droplet, instalé Docker, cloné el repositorio y levanté el stack con `docker compose up -d --build`. Todo arrancó sin cambios adicionales, que era exactamente el objetivo del enfoque con Docker desde el principio.

---

## Dificultades encontradas

### 1. La gestión de roles sin que nada se escapara

Esta fue la parte más complicada de todo el proyecto, y también la que más veces tuve que revisar. La aplicación tiene cuatro roles (Administrador, Responsable, Colaborador y Consulta) más un rol especial de Super Administrador, y cada uno tiene restricciones distintas según el contexto: no es lo mismo ser Administrador global que ser el Responsable asignado a un acto concreto.

El problema es que las reglas no son planas. Un Colaborador puede crear tareas pero solo asignárselas a sí mismo. Un Responsable puede gestionar un acto solo si es el responsable asignado a ese acto, no todos los de la hermandad. Un usuario con rol Consulta no puede ser asignado a ninguna tarea. Y el Super Administrador existe fuera de cualquier hermandad y solo puede gestionar usuarios y hermandades, sin acceso al contenido de los actos.

Intenté resolver esto con anotaciones de Spring Security (`@PreAuthorize`) al principio, pero la lógica era demasiado contextual. Terminé creando un componente centralizado, `AuthorizationHelper`, que encapsula todas las comprobaciones de autorización con métodos con nombre claro: `canManageAct`, `requireEventManager`, `requireTaskAssigned`, `requireAssignable`. Cada servicio llama al helper antes de ejecutar cualquier operación sensible, y si la comprobación falla lanza `AccessDeniedException`.

En el frontend el mismo problema existía en otra dimensión: el menú lateral, los botones de acción y las rutas tenían que mostrarse o bloquearse según el rol del usuario. Lo resolví con un `roleGuard` en las rutas y comprobaciones explícitas en los componentes a través del `AuthService`, que expone métodos como `isAdmin()`, `canManage()` o `isConsulta()` para evitar lógica duplicada en las plantillas.

### 2. El detalle del acto y la barra de progreso en tiempo real

La pantalla de detalle del acto es la más densa de toda la aplicación: cuatro pestañas (Tareas, Decisiones, Incidencias, Historial), acciones sobre cada elemento, el flujo de cierre embebido, la exportación y la barra de progreso que tiene que reflejar el estado real en cada momento.

La dificultad no estaba en construir cada pieza por separado, sino en que todo tuviera que funcionar de forma cohesionada. Cuando el usuario acepta una tarea, la barra de progreso tiene que subir inmediatamente. Cuando rechaza una incidencia, el botón de cierre tiene que activarse si ya no queda nada pendiente. Esto implica que después de cada acción el frontend recalcula el progreso localmente, sin hacer una nueva petición al servidor solo para actualizar el porcentaje.

El cálculo de progreso no es una media simple: cada estado tiene un peso distinto dentro del ciclo. Una tarea en `IN_PREPARATION` contribuye el 50%, en `CONFIRMED` el 75%, en `COMPLETED` o `REJECTED` el 100%. Las decisiones e incidencias son binarias: o están resueltas (100%) o no (0%). La función `calculateActProgress` agrega todos esos pesos y devuelve el porcentaje total, los elementos pendientes y el total.

```typescript
// act-progress.utils.ts
const TASK_PROGRESS_WEIGHTS: Record<string, number> = {
  'PLANNED': 0,
  'ACCEPTED': 0.25,
  'IN_PREPARATION': 0.5,
  'CONFIRMED': 0.75,
  'COMPLETED': 1,
  'REJECTED': 1,
};

export function calculateActProgress(
  tasks: TaskResponse[],
  decisions: DecisionResponse[],
  incidents: IncidentResponse[]
): ActProgress {
  const total = tasks.length + decisions.length + incidents.length;
  if (total === 0) return { total: 0, pending: 0, percent: 0 };

  const sum =
    tasks.reduce((acc, t) => acc + (TASK_PROGRESS_WEIGHTS[t.status] ?? 0), 0) +
    decisions.reduce((acc, d) => acc + (DECISION_PROGRESS_WEIGHTS[d.status] ?? 0), 0) +
    incidents.reduce((acc, i) => acc + (INCIDENT_PROGRESS_WEIGHTS[i.status] ?? 0), 0);

  const pending =
    tasks.filter(t => isTaskPending(t.status)).length +
    decisions.filter(d => isDecisionPending(d.status)).length +
    incidents.filter(i => isIncidentPending(i.status)).length;

  return { total, pending, percent: (sum / total) * 100 };
}
```

El otro problema en esta pantalla fue la gestión del modal de cierre embebido. El componente `CloseEvent` funciona tanto como página independiente (ruta `/events/:id/close`) como componente embebido dentro del detalle del acto. Esto requirió distinguir el modo de funcionamiento vía `@Input()` y usar un `@Output()` para comunicar al padre que el cierre se había completado y actualizar el estado local.

### 3. Los filtros combinados y el buscador

En el listado de actos el usuario puede filtrar por tipo, estado y fecha, y además usar un buscador de texto libre que cruza título, referencia y ubicación. El reto fue que cualquier combinación de estos cuatro parámetros tenía que funcionar correctamente, sin casos que devolvieran resultados incorrectos ni errores cuando algún filtro estaba vacío.

En el backend lo resolví con JPA Specifications. Cada criterio es una `Specification<Event>` independiente que devuelve una conjunción vacía cuando el parámetro es nulo, y el servicio las encadena con `Specification.where().and()`. Así se pueden combinar libremente sin lógica condicional en el servicio.

En el frontend la dificultad fue distinta: el buscador de texto no podía lanzar una petición por cada tecla. Usé un `Subject<string>` con `debounceTime(300)` y `distinctUntilChanged()` para que la búsqueda solo se disparara cuando el usuario hubiera dejado de escribir durante 300 ms y el valor hubiera cambiado. Cada vez que cualquier filtro cambia (tipo, estado, fecha o texto), se reinicia la paginación a la primera página y se recarga el listado.

---

## Decisiones técnicas clave

### Angular standalone components

Desde el principio opté por el modelo de componentes standalone de Angular, sin `NgModules`. Cada componente declara directamente sus dependencias en el array `imports`, lo que hace que cada fichero sea autocontenido y fácil de entender sin tener que rastrear en qué módulo está registrado. El routing usa lazy loading en todas las rutas para que el bundle inicial sea lo más pequeño posible.

### Spring Boot 4 con Spring Security stateless

El backend no mantiene ningún estado de sesión en el servidor. Cada petición llega con un token JWT en la cabecera `Authorization`, el filtro `JwtAuthenticationFilter` lo valida y reconstituye el contexto de seguridad para esa petición. Spring Security está configurado en modo `STATELESS`, sin cookies ni sesiones HTTP. Esto simplifica el escalado y evita toda una categoría de problemas de sincronización de sesiones.

La configuración de CORS se gestiona en `SecurityConfig` y el origen permitido se inyecta desde variables de entorno, de modo que el mismo `docker-compose.yml` sirve para desarrollo y para producción sin tocar el código.

### Flyway para migraciones

El esquema de la base de datos se gestiona íntegramente con Flyway. Cada cambio estructural es un fichero SQL versionado en `src/main/resources/db/migration`. En el historial del proyecto se pueden ver las 17 versiones que fue acumulando el esquema: desde el esquema inicial con los enums de PostgreSQL (V1) hasta la adición del sistema de solicitudes de cambio de administrador (V16) o las cascadas de borrado (V17). Flyway aplica automáticamente las migraciones pendientes en el arranque, así que cualquier entorno nuevo parte siempre del mismo estado.

### Docker Compose como único punto de entrada

La arquitectura de despliegue está diseñada para que el frontend sea el único contenedor expuesto al exterior. El backend escucha en el puerto 8080 pero solo lo expone internamente con `expose`, sin mapearlo al host. nginx actúa como reverse proxy y redirige las rutas `/api/`, `/swagger-ui/` y `/v3/api-docs/` hacia el backend a través de la red interna de Docker. Esto garantiza que la API nunca sea accesible directamente desde el exterior.

### JPA Specifications para filtros dinámicos

En lugar de escribir múltiples métodos en los repositorios para cada combinación de filtros posible, se usa el patrón Specification de Spring Data JPA. Cada criterio de filtrado es una función que recibe los parámetros y devuelve un predicado JPA, o una conjunción vacía si el parámetro es nulo. El servicio los compone en cadena.

### sessionStorage en lugar de localStorage

La sesión del usuario (token JWT y datos del perfil) se almacena en `sessionStorage` en lugar de `localStorage`. Esto significa que la sesión se borra automáticamente cuando el usuario cierra el navegador o la pestaña, reduciendo el riesgo de que una sesión quede activa en un dispositivo compartido. El `AuthService` migra al arranque los datos que pudieran haberse guardado en `localStorage` en versiones anteriores.

### Sanitización en doble capa

Todos los textos libres que el usuario introduce se sanitizan tanto en el frontend (eliminando etiquetas HTML antes de enviar al servidor) como en el backend (con `SanitizationUtils.sanitize` antes de persistir). Esto evita que contenido HTML o fragmentos de script lleguen a la base de datos independientemente de si el cliente aplica la sanitización o no.

---

## Herramientas de control de versiones

El proyecto usa **Git** como sistema de control de versiones, con el repositorio alojado en **GitHub** en `github.com/Aranaaa00/actacofrade`. El flujo de trabajo fue de una sola rama principal (`main`) durante el desarrollo individual, con commits atómicos que agrupan cada funcionalidad o corrección completa. El fichero `.gitignore` excluye el `.env` con las credenciales, los directorios `target/` y `node_modules/`, y los ficheros generados por el IDE.

---

## Fragmentos de código relevantes

### Filtro JWT

El filtro de autenticación se ejecuta en cada petición. Extrae el token del header `Authorization`, valida la firma y carga el usuario en el contexto de seguridad. Si el token es inválido o el usuario no existe, la petición continúa sin autenticación y Spring Security la rechaza en la capa de autorización.

```java
// JwtAuthenticationFilter.java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }

    try {
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (jwtService.isTokenValid(token, userDetails)) {
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
    } catch (JwtException | UsernameNotFoundException e) {
        log.debug("Token inválido o usuario no encontrado: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
}
```

### Lógica de autorización centralizada

El `AuthorizationHelper` concentra todas las comprobaciones de acceso. El método `canManageAct` determina si un usuario puede gestionar un acto concreto: solo si es Administrador o si es el Responsable asignado a ese acto.

```java
// AuthorizationHelper.java
public boolean canManageAct(User user, Event event) {
    return isAdmin(user) || isResponsible(user.getId(), event);
}

public boolean actsAsCollaboratorInEvent(User user, Event event) {
    return !canManageAct(user, event);
}

public void requireEventManager(Event event, User currentUser) {
    if (!canManageAct(currentUser, event)) {
        throw new AccessDeniedException("Solo puedes gestionar los actos de los que eres responsable");
    }
}
```

### Verificación de pendientes antes del cierre

Antes de cerrar un acto, el servicio consulta en la base de datos cuántas tareas, decisiones e incidencias siguen sin resolver. Si hay cualquier pendiente, lanza una excepción con el detalle de lo que falta. El cierre solo procede cuando los tres contadores son cero.

```java
// EventService.java
public EventResponse close(Integer id, String authenticatedEmail) {
    // ...
    long pendingTasks     = eventRepository.countPendingTasksByEventId(id);
    long openIncidents    = eventRepository.countOpenIncidentsByEventId(id);
    long pendingDecisions = eventRepository.countPendingDecisionsByEventId(id);

    if (pendingTasks > 0 || openIncidents > 0 || pendingDecisions > 0) {
        throw new IllegalStateException(
            "No se puede cerrar el acto: quedan " + pendingTasks
            + " tareas sin completar, " + pendingDecisions
            + " decisiones pendientes y " + openIncidents + " incidencias abiertas");
    }

    event.setStatus(EventStatus.CLOSED);
    event.setUpdatedAt(LocalDateTime.now());
    eventRepository.save(event);
    return toResponse(event);
}
```

### Filtros dinámicos con JPA Specifications

Cada criterio de filtrado es una `Specification<Event>` independiente. Si el parámetro llega nulo, la spec devuelve una conjunción vacía (equivalente a no aplicar filtro). El servicio los encadena sin lógica condicional.

```java
// EventSpecification.java
public static Specification<Event> searchByText(String search) {
    return (root, query, cb) -> {
        if (search == null || search.isBlank()) return cb.conjunction();
        String pattern = "%" + search.toLowerCase() + "%";
        return cb.or(
            cb.like(cb.lower(root.get("title")),     pattern),
            cb.like(cb.lower(root.get("reference")), pattern),
            cb.like(cb.lower(root.get("location")),  pattern)
        );
    };
}

// EventService.java — composición de specs
Specification<Event> spec = Specification
    .where(EventSpecification.hasHermandad(hermandadId))
    .and(EventSpecification.isNotClosed())
    .and(EventSpecification.hasEventType(typeFilter))
    .and(EventSpecification.hasStatus(statusFilter))
    .and(EventSpecification.hasEventDate(eventDate))
    .and(EventSpecification.searchByText(search));
```

### Buscador con debounce en el frontend

El buscador del listado de actos usa un `Subject` de RxJS con `debounceTime` y `distinctUntilChanged` para no lanzar una petición por cada tecla. La búsqueda solo se dispara cuando el usuario ha dejado de escribir 300 ms y el valor es distinto al anterior.

```typescript
// act-list.ts
private readonly searchSubject = new Subject<string>();

ngOnInit(): void {
  this.searchSubscription = this.searchSubject.pipe(
    debounceTime(300),
    distinctUntilChanged()
  ).subscribe((query) => {
    this.searchQuery = sanitizeText(query);
    this.currentPage = 1;
    this.loadEvents();
  });
}
```

### Rate limiting de login

El endpoint de login tiene un limitador de intentos en memoria. Permite hasta 5 intentos fallidos en una ventana de 5 minutos; si se superan, bloquea la IP durante 15 minutos. El bloqueo se gestiona con un `ConcurrentHashMap` y registros de tipo `record` para el estado de cada cliente.

```java
// LoginRateLimiter.java
public boolean tryAcquire(String clientKey) {
    Instant now = Instant.now();
    Attempt attempt = attempts.compute(clientKey, (key, existing) -> {
        if (existing == null) return new Attempt(1, now, null);
        if (existing.lockedUntil != null && existing.lockedUntil.isAfter(now)) return existing;
        if (existing.windowStart.plusSeconds(windowSeconds).isBefore(now)) return new Attempt(1, now, null);
        int next = existing.count + 1;
        if (next > maxAttempts)
            return new Attempt(next, existing.windowStart, now.plusSeconds(lockSeconds));
        return new Attempt(next, existing.windowStart, null);
    });
    return attempt.lockedUntil == null;
}
```
