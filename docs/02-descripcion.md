# 2. Descripción

## Funcionalidades principales

A continuación se describen las funcionalidades que conforman el núcleo de la aplicación.

---

### 1. Gestión de usuarios con roles

El acceso al sistema está controlado por un sistema de autenticación basado en JWT. Cada usuario pertenece a una hermandad concreta y tiene asignado uno de los siguientes roles, que determinan exactamente qué puede hacer dentro de la aplicación:

- **Administrador:** acceso completo. Crea y elimina actos, gestiona todos los elementos, avanza el estado del flujo y administra los usuarios de la hermandad.
- **Responsable:** puede crear y gestionar actos, tareas, decisiones e incidencias, pero no eliminar actos ni gestionar usuarios.
- **Colaborador:** puede registrar tareas, decisiones e incidencias dentro de actos existentes, y avanzar el estado de las tareas que tiene asignadas.
- **Consulta:** acceso de solo lectura a los actos y todo su contenido, sin posibilidad de modificar nada.

Al registrarse, el usuario elige su rol y la hermandad a la que pertenece. Los administradores pueden crear su hermandad directamente durante el registro. Las contraseñas se almacenan cifradas con bcrypt y toda la comunicación va sobre HTTPS.

---

### 2. Registro y ciclo de vida de los actos

Los actos son la entidad central de la aplicación. Cada acto tiene un tipo (Cabildo, Cultos, Procesión, Estación de Penitencia, Ensayo u Otro), una fecha, una ubicación, un responsable asignado y observaciones opcionales.

Una vez creado, el acto sigue un flujo de estados secuencial: **Planificación → Preparación → Confirmación → Cierre → Cerrado**. El avance entre estados lo realizan administradores y responsables. Este flujo refleja las fases reales de la organización de un acto y permite saber en todo momento en qué punto se encuentra la preparación.

Los actos activos (todos los que no están cerrados) aparecen en el listado principal con filtros por tipo, estado y fecha. Los cerrados pasan al historial, donde quedan disponibles para consulta pero no admiten modificaciones.

---

### 3. Asignación y confirmación de responsabilidades (tareas)

Esta es la funcionalidad diferencial de la aplicación. Cada tarea se asigna a un miembro concreto de la hermandad, con descripción y fecha límite opcionales. El responsable asignado no se convierte en responsable por el hecho de que alguien lo haya apuntado: tiene que aceptarlo explícitamente.

El ciclo completo de una tarea es:

`PLANIFICADA → ACEPTADA → EN PREPARACIÓN → CONFIRMADA → COMPLETADA`

Cuando la tarea se crea, el asignado la recibe en estado `PLANIFICADA`. Si el responsable la acepta, pasa a `ACEPTADA`; si no puede asumirla, la rechaza con un motivo escrito que queda registrado. Una vez aceptada, el propio asignado la avanza a `EN PREPARACIÓN` cuando empieza a trabajar en ella, a `CONFIRMADA` cuando está lista para revisión, y a `COMPLETADA` cuando termina. En cualquier punto del ciclo, el administrador o responsable del acto puede revertir una tarea al estado anterior si es necesario.

Los usuarios tienen una vista personal con todas sus tareas asignadas en cualquier acto, donde pueden aceptar, rechazar o avanzar el estado sin tener que entrar en cada acto por separado.

---

### 4. Registro de decisiones

Las decisiones tomadas durante la preparación de un acto quedan registradas con su descripción, el área de la hermandad que corresponde (Mayordomía, Secretaría, Priostía, Tesorería o Diputación Mayor), el usuario que la revisó y su estado: `PENDIENTE`, `ACEPTADA` o `RECHAZADA`. Esto permite reconstruir qué se acordó en cada momento y quién lo acordó.

---

### 5. Gestión de incidencias

Las incidencias que surgen durante la organización o ejecución de un acto se registran con una descripción, el usuario que las reportó y su estado: `ABIERTA` o `RESUELTA`. Cuando se resuelve una incidencia queda registrada la fecha de resolución y el usuario que la cerró. Las incidencias abiertas bloquean el cierre del acto hasta que se resuelvan.

---

### 6. Control de cierre

Antes de que un acto pueda cerrarse, el sistema realiza una verificación automática sobre todos sus elementos. Si hay tareas que no están completadas, decisiones pendientes de revisión o incidencias abiertas, el cierre queda bloqueado y se muestra la lista exacta de lo que falta resolver, con su tipo y estado actual. Solo cuando todo está en orden se puede cerrar el acto de forma definitiva. El cierre es irreversible.

Este control garantiza que no se pueda dar por terminado un acto con pendientes sin resolver, algo que con WhatsApp o una reunión presencial es imposible de asegurar.

---

### 7. Trazabilidad e historial de cambios

Cada acción que se realiza sobre un acto —crear, editar o eliminar una tarea, decisión o incidencia; avanzar estados; rechazar o completar— queda registrada en un log de auditoría. Cada entrada del log identifica la entidad afectada, la acción realizada, el usuario que la ejecutó, la fecha y los datos modificados en formato JSON. El historial es accesible desde el propio detalle del acto, paginado y en orden cronológico.

Esto resuelve directamente uno de los problemas recurrentes en las hermandades: la imposibilidad de reconstruir qué pasó, cuándo y quién tomó cada decisión.

---

### 8. Exportación de resúmenes en PDF y CSV

Cualquier acto puede exportarse como documento PDF o fichero CSV. El usuario elige las secciones a incluir: tareas y responsables, decisiones tomadas, incidencias registradas y observaciones del acto. Se puede incluir cualquier combinación. El PDF incluye el nombre y los datos de contacto de la hermandad en la cabecera. El CSV genera una tabla plana válida para abrir directamente en Excel. El fichero lo genera el backend y el navegador lo descarga automáticamente.

---

### 9. Clonación de actos

Cualquier acto puede clonarse para crear uno nuevo de forma inmediata. Al clonar, el sistema genera un acto nuevo en estado `PLANIFICACIÓN` con el mismo tipo, ubicación y observaciones que el original, y copia todas las tareas en estado `PLANIFICADA`. Las decisiones e incidencias no se copian, porque son específicas de cada edición.

Es especialmente útil para actos que se repiten cada año, como los ensayos o la estación de penitencia, donde la estructura de tareas y responsables suele ser la misma.

---

### 10. Avisos y alertas automáticas

El sistema detecta automáticamente qué elementos requieren atención: tareas sin aceptar, decisiones sin revisar, incidencias abiertas. Estas alertas se muestran en el panel principal nada más entrar al sistema, con el tipo de elemento, su descripción y un enlace directo al acto que las origina. También se indica cuántos actos están listos para cerrar porque todos sus elementos están resueltos.

El objetivo es que el usuario nunca tenga que ir acto por acto para saber qué está pendiente.

---

## Interfaz de usuario y experiencia de usuario

### Estructura general

La interfaz sigue una distribución con cabecera fija y área de contenido central. La navegación se realiza a través del menú lateral, que muestra las secciones disponibles según el rol del usuario autenticado. La aplicación es una SPA, así que no hay recargas de página al navegar entre secciones.

### Sistema de diseño

Se ha desarrollado un sistema de componentes propio basado en SCSS modular. Los componentes reutilizables incluyen: `Badge` para estados con variantes de color, `Banner` para avisos contextuales, `Tabs` para navegación por pestañas, `Pagination`, `ModalOverlay`, `ConfirmDialog`, `FilterDropdown` y `Datepicker`. Los iconos son de la librería Lucide Angular.

Los badges de estado siguen una paleta semántica constante en toda la aplicación: verde para estados completados o positivos, ámbar para pendientes o en progreso, rojo para rechazados o bloqueantes, y un tono neutro para estados informativos.

### Flujo de uso habitual

El recorrido más habitual dentro de la aplicación es el siguiente:

1. El usuario entra al dashboard y ve los actos activos y las alertas pendientes.
2. Navega al listado de actos para consultar el estado general.
3. Abre el detalle de un acto concreto.
4. Dentro del acto, gestiona tareas, decisiones e incidencias desde las pestañas correspondientes.
5. Cuando todo está resuelto, avanza el estado del acto hasta iniciar el cierre.
6. El sistema verifica los pendientes y, si no hay ninguno, permite cerrar el acto.
7. El acto pasa al historial.

### Retroalimentación al usuario

Todas las acciones con consecuencias (crear, editar, eliminar, cerrar) muestran un toast de confirmación o error. Las acciones destructivas o irreversibles pasan por un diálogo de confirmación previo. Los formularios validan en tiempo real y muestran mensajes de error específicos por campo. El botón de cierre permanece deshabilitado mientras haya elementos pendientes, evitando errores por descuido.

### Accesibilidad

La interfaz usa etiquetas semánticas (`section`, `article`, `header`, `nav`, `table`, `dl`, `search`), atributos `aria-label`, `aria-hidden` y roles ARIA donde corresponde. Las tablas tienen `caption` para lectores de pantalla. La navegación por teclado está soportada. Los contrastes de color cumplen con el nivel AA de las WCAG 2.1.

### Responsive

La aplicación está diseñada para funcionar en pantallas de escritorio y dispositivos móviles. Las tarjetas de actos, las tablas y los formularios adaptan su disposición mediante SCSS.

---

## Usuarios objetivo y casos de uso

### Usuarios objetivo

La aplicación está pensada para hermandades y cofradías de tamaño medio o grande, con equipos de entre 15 y 50 miembros activos involucrados en la organización de sus actos. No requiere formación técnica previa: cualquier persona que maneje un correo electrónico puede utilizarla sin dificultad.

Dentro de una hermandad, los perfiles que usarían la aplicación son:

**Secretario o administrador de la hermandad.** Es el usuario con más responsabilidad dentro del sistema. Crea los actos, asigna responsables, gestiona los usuarios y configura los datos de la hermandad. También es quien tiene autoridad para cerrar un acto de forma definitiva.

**Responsable de sección.** Gestiona tareas dentro de su área, añade decisiones e incidencias, y hace seguimiento de lo que está pendiente. En hermandades grandes puede haber varios responsables asignados a distintos actos.

**Colaborador.** Miembro activo que participa en la preparación de los actos. Puede crear y actualizar tareas, decisiones e incidencias, y avanzar el estado de las tareas que tiene asignadas.

**Miembro de consulta.** Hermano o miembro de junta que necesita consultar el estado de los actos sin participar directamente en la gestión. Solo puede leer.

---

### Casos de uso representativos

**Organización de una estación de penitencia**

El secretario o responsable crea el acto "Estación de Penitencia 2026" con fecha, ubicación y su propia asignación como responsable. Añade las tareas habituales: coordinación de portadores, gestión del paso de palio, revisión de enseres. Cada tarea se asigna a un colaborador concreto o ellos mismos crean si ven oportuno.

Los colaboradores reciben sus tareas en estado `PLANIFICADA` y se las aceptan o rechazan desde "Mis tareas" o en el propio acto. A medida que avanzan los preparativos, van actualizando el estado de cada tarea. Las incidencias que surgen (un portador que causa baja, un elemento del paso que necesita reparación) se registran en el acto con su estado y responsable.

Cuando se acerca la fecha, el secretario revisa el dashboard y ve que quedan dos tareas sin confirmar y una incidencia abierta. Entra al acto, resuelve los pendientes y cierra el acto cuando todo está en orden. El acto pasa al historial y queda disponible para consulta o para clonar en la siguiente edición.

**Seguimiento de un ensayo**

Un responsable crea el acto "Ensayo de Cuadrilla — Marzo" y añade las tareas típicas: reserva de espacio, avisos a los portadores, coordinación de relevos. Las tareas se asignan, los colaboradores las aceptan y actualizan su progreso. Al terminar el ensayo, el responsable registra las incidencias detectadas (portadores que no se presentaron, ajustes de cuadrilla necesarios) para tenerlas en cuenta en el siguiente ensayo.

Al año siguiente, el mismo acto se clona y sirve como punto de partida, con las mismas tareas y responsables asignados por defecto.

**Consulta del historial por un nuevo miembro**

Un miembro recién incorporado quiere entender cómo se organizó la última estación de penitencia. Entra al historial, filtra por tipo "Estación de Penitencia" y abre el acto cerrado. Puede revisar todas las tareas, decisiones tomadas e incidencias que ocurrieron, con sus fechas y responsables. No necesita preguntar a nadie: toda la información está registrada y accesible.

