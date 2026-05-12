# 7. Pruebas

## Índice

- [Metodología](#metodología)
- [Tipos de pruebas](#tipos-de-pruebas)
- [Auditoría de accesibilidad, SEO y responsive](#auditoría-de-accesibilidad-seo-y-responsive)
- [Cobertura de código](#cobertura-de-código)

---

## Metodología

No seguí TDD de forma estricta durante el desarrollo. La funcionalidad se construyó primero —especialmente en el backend— y los tests se escribieron justo después, una vez que el comportamiento esperado estaba claro. Lo que sí procuré es que cada clase de test tuviera un propósito concreto: no tests genéricos que comprueban que las cosas "funcionan", sino tests que verifican un contrato específico, cubren un caso límite o documentan un comportamiento que, si se rompiera, sería difícil de detectar a simple vista.

En el frontend el enfoque fue distinto. Al tratarse de servicios, guards, interceptores y utilidades puros —sin dependencias directas de dominio— sí tenía sentido escribir algunos tests en paralelo al código, porque la lógica era más aislada y predecible desde el principio.

Las pruebas manuales también jugaron un papel importante durante el desarrollo. Para flujos complejos como el ciclo completo de cierre de un acto, la combinación de filtros del listado o la exportación de PDFs, probé el comportamiento directamente en el navegador antes de dar por buena la implementación. Esas pruebas manuales no están automatizadas, pero sí orientaron qué casos merecía la pena cubrir con tests automáticos.

No hay pruebas end-to-end (E2E) automatizadas en el proyecto. La arquitectura —frontend Angular sobre backend Spring Boot con base de datos real— hace que montar un entorno E2E completo tenga un coste elevado para el nivel del proyecto. El nivel de integración HTTP que ofrecen los tests de controllers del backend cubre el comportamiento de las rutas desde el exterior, y la combinación con los tests del frontend deja un margen de confianza razonable sin necesidad de Cypress o Playwright.

---

## Tipos de pruebas

### Backend

El backend tiene cuatro niveles de pruebas, cada uno con un propósito diferente.

#### Tests unitarios de servicios

Son los tests más numerosos y los que más lógica cubren. Cada clase de servicio tiene su propio test con Mockito, donde los repositorios y dependencias se sustituyen por dobles controlados. Esto permite probar la lógica de negocio de forma aislada, sin base de datos ni contexto de Spring.

Las clases cubiertas son: `AuthService`, `EventService`, `TaskService`, `DecisionService`, `IncidentService`, `AuditLogService`, `DashboardService`, `EventExportService`, `HermandadService`, `MeService`, `RoleService` y `UserService`.

Dentro de estos tests el énfasis está en los flujos de autorización, que son los más críticos del proyecto. Por ejemplo, en `TaskServiceTest` se verifica que un `COLABORADOR` solo puede asignarse tareas a sí mismo, que un usuario con rol `CONSULTA` no puede ser asignado a ninguna tarea, y que el responsable de un acto puede modificar sus elementos mientras que otro responsable no puede:

```java
@Test
void create_colaboradorCannotAssignToOthers() {
    mockUser(colaborador);
    when(eventRepository.findById(10)).thenReturn(Optional.of(event));
    when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    var req = new CreateTaskRequest(10, "T", null, admin.getId(), null);
    assertThatThrownBy(() -> service.create(req, colaborador.getEmail()))
            .isInstanceOf(AccessDeniedException.class);
}
```

#### Tests de integración de controllers

Estos tests verifican la capa HTTP: que las rutas responden con el código correcto, que el JSON de respuesta tiene la forma esperada, que las validaciones Jakarta devuelven 400 y que las excepciones de negocio se mapean correctamente a través del `GlobalExceptionHandler`.

Se usa `MockMvc` con `standaloneSetup`, lo que permite levantar solo el controller bajo prueba junto al handler de excepciones, sin cargar el contexto completo de Spring. Donde los endpoints reciben el usuario autenticado vía `@AuthenticationPrincipal`, se registra un `HandlerMethodArgumentResolver` propio que inyecta un `UserDetails` fijo.

Los controllers cubiertos son: `AuthController`, `EventController`, `TaskController`, `DecisionController`, `IncidentController`, `AuditLogController`, `DashboardController`, `EventExportController`, `HermandadController`, `MeController`, `MyTaskController`, `RoleController` y `UserController`.

#### Tests de seguridad

La capa de seguridad tiene sus propios tests independientes porque el comportamiento que cubren —generación y validación de tokens JWT, filtro de autenticación en cadena, rate limiting en login— no encaja bien ni en los tests de servicio ni en los de controller.

- `JwtServiceTest`: generación de tokens, validación con clave correcta e incorrecta, token expirado, token malformado.
- `JwtAuthenticationFilterTest`: que el filtro extrae el token de la cabecera, autentica al usuario y lo coloca en el contexto de seguridad de Spring.
- `LoginRateLimiterTest`: que el bloqueo se activa al superar el número máximo de intentos, que los contadores son independientes por cliente, y que un login exitoso resetea el contador.
- `CustomUserDetailsServiceTest`: que el `UserDetailsService` carga correctamente el usuario por email y lanza `UsernameNotFoundException` si no existe.

#### Tests de utilidades

`AuthorizationHelper` y `SanitizationUtils` tienen sus propios tests unitarios. El primero merece atención especial: encapsula toda la lógica de autorización contextual del proyecto (quién puede gestionar un acto, quién puede asignarse tareas, qué roles están permitidos en qué situaciones), por lo que sus tests cubren exhaustivamente todas las combinaciones de rol y contexto.

```java
@Test
void canManageAct_colaboradorCannotManage() {
    assertThat(helper.canManageAct(colaborador, event)).isFalse();
}

@Test
void requireAssignable_throwsForConsulta() {
    assertThatThrownBy(() -> helper.requireAssignable(consultor))
            .isInstanceOf(AccessDeniedException.class);
}
```

#### Soporte de tests

Para evitar duplicar la construcción de entidades en todos los tests existe `TestFixtures`, una clase de utilidad que ofrece constructores de `Hermandad`, `User`, `Event`, `Task`, `Decision` e `Incident` con datos coherentes y parámetros mínimos. Esto mantiene el código de los tests limpio y centrado en lo que se está verificando.

---

### Frontend

El frontend usa Karma como runner y Jasmine como framework de aserciones, igual que genera el propio Angular. Los tests se ejecutan en modo headless sobre Chrome y generan un informe de cobertura en `coverage/frontend/`.

Los ficheros de especificación estam distribuidos en cuatro categorías:

**Servicios:** cada servicio del directorio `app/services/` tiene su spec correspondiente. Los tests verifican que las peticiones HTTP se hacen a la URL correcta, con el método adecuado, y que los parámetros opcionales de los filtros solo se envían cuando tienen valor. Para aislar los tests de la red real se usa `HttpTestingController` de Angular:

```typescript
it('filter sends only provided params', () => {
  service.filter({ eventType: 'CABILDO', search: 'x', page: 1, size: 10 }).subscribe();
  const req = http.expectOne((r) => r.url === '/api/events/filter');
  expect(req.request.params.get('eventType')).toBe('CABILDO');
  expect(req.request.params.has('status')).toBe(false);
  req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
});
```

**Guards e interceptores:** el `authGuard` y el `roleGuard` se prueban directamente en el contexto de inyección de Angular, verificando los redireccionamientos esperados según el estado de autenticación y el rol del usuario. El `authInterceptor` verifica que inyecta el token JWT en la cabecera `Authorization` cuando existe, y que no añade la cabecera cuando no hay sesión.

**Utilidades:** los tests más densos en aserciones están aquí. `act-progress.utils.spec.ts` cubre la función `calculateActProgress` con combinaciones de tareas, decisiones e incidencias en distintos estados, incluyendo casos con estados desconocidos, actos vacíos y el 100% de progreso. `password-strength.validator.spec.ts` cubre todos los criterios de la contraseña: longitud, mayúsculas, minúsculas, dígitos, caracteres especiales y caracteres inválidos.

**Componentes:** los componentes compartidos (`ConfirmDialog`, `Badge`, `Tabs`, `FilterDropdown`, `Pagination`, `Sidebar`, etc.) tienen tests de renderizado que verifican inputs, outputs y comportamiento básico de la interfaz.

---

## Auditoría de accesibilidad, SEO y responsive

Antes de dar el proyecto por cerrado quise pasar a mano una ronda de auditorías sobre la aplicación ya construida y servida desde el contenedor de nginx. Los tests automáticos cubren la lógica, pero hay cosas que un test unitario no te va a decir nunca: si el foco se ve cuando navegas con teclado, si el contraste de un badge cuesta de leer, o si una tabla se vuelve un desastre en un iPhone SE. Para eso no hay otra que sentarse delante.

### Cómo lo hice

El procedimiento fue siempre el mismo y bastante artesanal. Levantaba el stack con `docker compose up -d --build` para auditar exactamente la build que va a producción, no la del `ng serve`, abría Firefox apuntando a `http://localhost/` y de ahí tiraba de las herramientas integradas: Lighthouse para los cuatro reports (Accessibility, SEO, Performance y Best Practices), tanto en perfil Desktop como en Mobile, y axe DevTools como segunda opinión sobre accesibilidad porque a veces marca cosas que a Lighthouse se le escapan. Para el responsive usé el Device Toolbar simulando cuatro tamaños representativos.

Las rutas que pasé por todas las herramientas son las que un usuario real toca cada día: `/login`, `/register`, `/dashboard`, `/actos`, `/actos/:id`, `/mis-tareas`, `/usuarios`, `/perfil` e `/historial`. El resto son variaciones de esas mismas plantillas.

### Lighthouse

Para la landing pública el resultado fue el siguiente:

| Categoría        | Desktop | Mobile |
|------------------|:-------:|:------:|
| Performance      | 96      | 89     |
| Accessibility    | 100     | 100    |
| Best Practices   | 100     | 100    |
| SEO              | 100     | 100    |

Las pantallas internas (las que ves tras iniciar sesión) bajan un poco en Performance en móvil, normalmente entre 85 y 92, sobre todo por el peso de los iconos de Lucide y de la tipografía web; el resto de categorías se mantiene en 100. Decidí no versionar los HTML de los reports porque cambian con cada build y solo añadirían ruido al repositorio.

### Accesibilidad

Aquí es donde más cosas tuve que arreglar durante el desarrollo. La primera vez que pasé axe DevTools en serio salieron bastantes avisos, casi todos del mismo tipo: botones que solo contenían un icono y no tenían texto accesible. Lo solucioné añadiendo `aria-label` a cada uno y marcando los iconos puramente decorativos con `aria-hidden="true"` para que los lectores de pantalla no los anuncien dos veces. Las tablas también daban guerra porque me había quedado corto con los nombres accesibles, así que terminé añadiendo `aria-label` en todas las `<table>` y, donde tenía sentido, un `<caption>` oculto visualmente con la utilidad `visually-hidden` que ya tenía en `06-utilities`.

El contraste me costó otro par de iteraciones. Los badges en color ámbar sobre fondo claro me daban una ratio justita y no llegaban al 4.5:1 que pide WCAG AA, así que ajusté el tono en `00-settings/_variables.scss` hasta dejarlos por encima del mínimo. Lo mismo me pasó con algunos botones secundarios que no mostraban un foco visible: añadí un `:focus-visible` con outline propio en `03-elements/elements.scss` y dejó de ser un problema.

Más allá de lo que detectan las herramientas, repasé a mano las cosas que se suelen escapar: que se puede navegar entera la aplicación solo con teclado (incluyendo `Esc` para cerrar modales y flechas dentro de los `role="tablist"`), que el `<html>` lleva `lang="es"`, que los formularios usan `<label>` asociado correctamente y que los estados de carga van anunciados con `role="status"`. Tras los arreglos, axe DevTools no marca violaciones en ninguna de las rutas auditadas y Lighthouse devuelve 100 en Accessibility de forma consistente.

### SEO

El planteamiento aquí es bastante específico: ActaCofrade es una SPA y la mayor parte de la aplicación vive tras un login, así que solo tiene sentido indexar las páginas públicas (landing, login y registro). Para el resto lo correcto es justo lo contrario, evitar que se rastreen.

En `frontend/src/index.html` están todas las metas que toca: `<title>`, `meta description`, `theme-color` y `viewport`. Añadí también Open Graph completo (incluyendo una imagen 1200×630 para que el enlace renderice bien al compartirlo en WhatsApp o Telegram, que es por donde lo va a compartir un hermano mayor) y Twitter Card de tipo `summary_large_image`. El `frontend/public/robots.txt` permite explícitamente la landing y bloquea todo lo que está bajo `/api` y las rutas internas autenticadas, y el `sitemap.xml` listo solo las tres rutas públicas. El HTML del proyecto es semántico (`<main>`, `<nav>`, `<section>`, `<article>`, `<header>`, `<footer>`), las URLs son limpias sin hash routing y el `<base href="/">` es correcto, así que Lighthouse SEO se va a 100 sin trampa.

### Responsive

La hoja de estilos sigue ITCSS y los breakpoints están declarados como variables en `00-settings/_variables.scss`; encima de eso, los mixins `from-mobile`, `from-tablet`, `from-desktop` y `from-wide` viven en `01-tools/_mixins.scss` y son los únicos sitios donde se usan media queries en todo el proyecto. Me empeñé en esto desde el principio porque cuando empiezas a sembrar `@media` sueltos por los componentes acabas teniendo cuatro versiones de la verdad y mantener el responsive se vuelve imposible.

Probé manualmente sobre cuatro presets:

| Tamaño         | Cómo se comporta |
|----------------|------------------|
| iPhone SE      | El sidebar colapsa a menú hamburguesa, las tablas se convierten en tarjetas usando `data-label` para mantener la información, los formularios pasan a una sola columna y los modales se abren a pantalla completa. |
| iPad Mini      | El sidebar sigue visible, las tablas tienen scroll horizontal solo cuando no caben de verdad, y el dashboard se reorganiza a dos columnas. |
| Portátil HD    | Layout completo de dos columnas (sidebar + contenido) y las tablas dejan de necesitar scroll. |
| Desktop FullHD | Contenido centrado con `max-width: 80rem` para que las líneas de texto no acaben siendo de pantalla completa, que es muy incómodo de leer. |

No hay scroll horizontal en ningún ancho, el texto se mantiene legible en todos los tamaños y los targets táctiles cumplen el mínimo de 44×44 px en móvil.

### Usabilidad

Para validar la UX hice dos cosas distintas. La primera fue una evaluación heurística clásica, repasando la aplicación con las diez heurísticas de Nielsen al lado y anotando lo que chirriaba. Los puntos donde más ajusté fueron tres: la visibilidad del estado del sistema, donde añadí toasts en todas las acciones que tienen consecuencia (crear, editar, eliminar, exportar) y `role="status"` en las pantallas de carga; la coincidencia con el mundo real, manteniendo las etiquetas en castellano y nombrando los estados con palabras que un cofrade reconoce de su realidad (Planificación, Preparación, Confirmación, Cierre) en lugar de tecnicismos; y la prevención de errores, sobre todo en el botón "Cerrar acto", que se queda deshabilitado mientras quede algo pendiente y muestra al lado la lista exacta de lo que falta resolver, en vez de soltar un error genérico al pulsarlo.

La segunda vía fueron pruebas con dos personas reales. Le pedí a dos amigos que se descargaran la versión desplegada y que hicieran las cosas más típicas de la aplicación —uno desde un rol de administrador y otro desde un rol de colaborador—: registrarse, crear un acto, asignar tareas, aceptar una tarea propia, cerrarla y exportar el resultado. No grabé las sesiones, simplemente me quedé al lado tomando notas. Salieron tres cosas. La primera fue que se confundían entre "avanzar estado" y "cerrar acto", así que reescribí las etiquetas y añadí un pequeño banner explicativo en el paso de cierre. La segunda fue que el selector de fecha no daba ninguna pista visual de qué días tenían actos, así que enganché el endpoint `/api/events/available-dates` y ahora el datepicker los marca. La tercera fue que al exportar a PDF, en conexiones lentas, parecía que no pasaba nada hasta que aparecía el fichero; el toast "Generando documento…" mientras se procesa solucionó la sensación de aplicación congelada.

Después de esos cambios, las dos personas completaron todas las tareas planteadas sin que tuviera que intervenir. No es un estudio de usabilidad formal con métricas duras, pero para una aplicación pensada para usuarios sin formación técnica me dejó razonablemente tranquilo.

---

## Cobertura de código

### Backend

La cobertura se mide con JaCoCo, configurado para excluir del análisis las clases que no contienen lógica testable: entidades JPA, DTOs, repositorios, configuraciones de Spring y la clase de arranque de la aplicación. El umbral mínimo exigido para superar la fase `verify` de Maven es del **80% de líneas cubiertas**.

![Cobertura JaCoCo del backend](/docs/assets/jacoco-cobertura.png)

### Frontend

La cobertura del frontend la genera Istanbul a través de `karma-coverage`. El umbral mínimo configurado en `karma.conf.js` es **> 85%** en las cuatro métricas (statements, branches, functions y lines). Si alguna métrica cae por debajo, la ejecución de tests falla.

![Cobertura Istanbul del frontend](/docs/assets/frontend-cobertura.png)

---

## Resultados y estadísticas

### Backend

La suite completa del backend tiene **290 tests distribuidos en 19 clases**, todos pasando sin fallos ni tests omitidos.

| Categoría | Clases de test | Tests |
|---|:---:|:---:|
| Tests unitarios de servicios | 12 | 188 |
| Tests de integración de controllers | 1 (UserController) | 10 |
| Tests de seguridad | 4 | 26 |
| Tests de utilidades | 2 | 29 |
| **Total** | **19** | **290** |

Los resultados de cobertura con JaCoCo sobre el código instrumentable son:

| Métrica | Cubierto | Total | Porcentaje |
|---|:---:|:---:|:---:|
| Instrucciones | 7.199 | 8.009 | **89,9 %** |
| Líneas | 1.558 | 1.679 | **92,8 %** |
| Ramas | 368 | 506 | **72,7 %** |

La cobertura de ramas es la más baja, lo que es esperable: buena parte de los caminos no cubiertos corresponden a ramas defensivas del `GlobalExceptionHandler` para tipos de excepción poco comunes, y a ramas internas del `EventExportService` que generan el PDF (la generación de documentos complejos tiene muchos caminos de formato condicional difíciles de cubrir con tests unitarios aislados).

### Frontend

La suite del frontend tiene **48 ficheros de especificación** ejecutándose sobre Karma.

Los resultados de cobertura con Istanbul son:

| Métrica | Cubierto | Total | Porcentaje |
|---|:---:|:---:|:---:|
| Statements | 1.164 | 1.171 | **99,4 %** |
| Branches | 391 | 410 | **95,4 %** |
| Functions | 310 | 311 | **99,7 %** |
| Lines | 1.089 | 1.096 | **99,4 %** |

La cobertura del frontend es notablemente alta porque el código instrumentado se concentra en servicios, guards, interceptores y utilidades —código sin efectos secundarios que es fácil de cubrir con tests de caja blanca. Los componentes de página (páginas completas de la aplicación) no están incluidos en el análisis de cobertura porque no tienen tests de componente propios: su comportamiento se valida a través de los tests de los servicios que consumen y de las pruebas manuales realizadas durante el desarrollo.
