# 9. Manual de usuario

## Introducción

Este manual explica cómo usar ActaCofrade. No hace falta tener conocimientos técnicos: si sabes usar el correo electrónico, te basta para manejar la aplicación sin ninguna dificultad.

ActaCofrade es una aplicación web para que las hermandades y cofradías puedan organizar sus actos de forma ordenada. La idea es simple: todo lo que se decide, se asigna o se detecta durante la preparación de un acto —tareas, decisiones, incidencias— queda escrito en un único sitio, con nombre de responsable, fecha y estado. Nada se pierde, nada queda en el aire, y cualquier miembro puede consultar en cualquier momento qué está pendiente y de quién depende.

---

## Roles de usuario

Antes de explicar las pantallas, conviene entender qué puede hacer cada tipo de usuario. El sistema controla el acceso mediante roles, así que no todas las personas ven las mismas opciones.

| Rol | Qué puede hacer |
|---|---|
| **Administrador** | Acceso completo. Crea y elimina actos, gestiona todos los elementos, avanza el estado del flujo y administra los usuarios de la hermandad. |
| **Responsable** | Puede crear y gestionar actos, tareas, decisiones e incidencias, pero no eliminar actos ni gestionar usuarios. |
| **Colaborador** | Puede registrar tareas, decisiones e incidencias dentro de actos existentes, y avanzar el estado de las tareas que tiene asignadas una vez sean aceptadas. |
| **Consulta** | Acceso de solo lectura. Puede ver actos y todo su contenido, pero no modificar nada. |

A lo largo de este manual se indica en cada apartado qué roles tienen acceso a cada función.

---

## Acceso a la aplicación

### Página de inicio

Al abrir la dirección de la aplicación en el navegador, lo primero que aparece es la pantalla de bienvenida. Muestra el escudo de ActaCofrade, una línea descriptiva y un botón para acceder al sistema.

![Pantalla de inicio de ActaCofrade](/docs/assets/landing.png)

Desde aquí se puede acceder al formulario de inicio de sesión pulsando **Acceder al sistema**.

---

### Iniciar sesión

El formulario de acceso pide el correo electrónico y la contraseña. Hay una casilla opcional de **Recordarme** que mantiene la sesión activa entre visitas.

![Formulario de inicio de sesión](/docs/assets/login.png)

Si los datos son correctos, la aplicación redirige directamente al panel principal. Si el correo o la contraseña no son válidos, se muestra un aviso de error debajo del formulario.

> El sistema bloquea temporalmente el acceso tras varios intentos fallidos consecutivos. Si esto ocurre, espera unos minutos antes de volver a intentarlo.

---

### Crear una cuenta

Si todavía no tienes cuenta, el formulario de registro es accesible desde el enlace **¿No tienes cuenta? Solicítala** en la pantalla de inicio de sesión.

![Formulario de registro](/docs/assets/register.png)

El formulario pide:

- **Nombre completo**
- **Correo electrónico**
- **Contraseña** y su confirmación
- **Rol** que quieres tener en la hermandad
- **Hermandad** a la que perteneces. Si vas a crear una hermandad nueva —por ejemplo, si eres el primer administrador—, selecciona el rol Administrador y escribe el nombre de la hermandad directamente. Si te unes a una ya existente, elige tu hermandad del desplegable.

Una vez enviado el formulario, la cuenta queda creada y puedes acceder con tus credenciales.

---

## Navegación

Dentro de la aplicación, la navegación se hace a través del **menú lateral** (sidebar), que aparece en el lado izquierdo de la pantalla. En dispositivos móviles se abre y cierra con el botón de menú de la cabecera.

El menú muestra las secciones disponibles según tu rol. En la parte inferior del menú aparece tu nombre, tu rol y dos botones: uno para editar tu perfil y otro para cerrar sesión.

Las secciones disponibles son:

- **Inicio** — Panel principal con el resumen del estado de la hermandad.
- **Actos** — Con dos subapartados: Registro de Actos y Mis Tareas (esta última no aparece para el rol Consulta).
- **Historial** — Listado de actos ya cerrados.
- **Usuarios** — Solo visible para administradores.
- **Configuración** — Datos de la hermandad.
- **Perfil usuario** - Información del usuario con su rol. Puede editar con el lápiz o cerrar sesión

---

## Panel principal (Dashboard)

Es la primera pantalla que aparece al iniciar sesión. Está pensada para que de un solo vistazo sepas en qué punto está la hermandad.

![Panel principal de ActaCofrade](/docs/assets/dashboard.png)

La pantalla se divide en tres partes:

**Las estadísticas superiores** muestran tres cifras: el número de actos activos (todos los que no están cerrados), el número de elementos pendientes de atención y cuántos actos están listos para cerrar porque ya tienen todo resuelto.

**La lista de actos activos** aparece en el lado izquierdo. Cada entrada muestra el tipo de acto, el nombre y su estado actual. Pulsando sobre el nombre o la flecha se accede directamente al detalle del acto.

**Las alertas recientes** aparecen en el lado derecho. Cada alerta indica un elemento que requiere atención: una tarea que está esperando ser aceptada, una decisión sin revisar o una incidencia abierta. Pulsando sobre la descripción de una alerta se va directamente al acto que la genera.

Los usuarios con rol **Consulta** ven únicamente la lista de actos activos, sin estadísticas ni alertas, porque no tienen asignadas responsabilidades sobre ningún elemento.

---

## Registro de Actos

Accesible desde **Actos → Registro de Actos** en el menú lateral. Aquí se listan todos los actos activos de la hermandad (los que aún no están cerrados).

![Listado de actos activos](/docs/assets/act-list.png)

### Filtros y búsqueda

En la parte superior hay cuatro herramientas para encontrar lo que buscas:

- **Tipo de acto** — Filtra por Cabildo, Cultos, Procesión, Ensayo u Otro.
- **Estado** — Filtra por el estado actual del acto dentro de su flujo de preparación.
- **Fecha** — Filtra por la fecha del acto.
- **Buscador de texto** — Busca por nombre, referencia o ubicación. La búsqueda se aplica mientras escribes, con un pequeño retardo para no lanzar peticiones con cada tecla.

El botón **Limpiar** elimina todos los filtros activos de una vez.

### Las tarjetas de acto

Cada acto aparece como una tarjeta que muestra el tipo (en la banda lateral), el nombre, la fecha, el responsable asignado y una barra de progreso que indica en qué fase del flujo de preparación se encuentra. Al final de cada tarjeta hay dos acciones: **Gestionar** (accede al detalle) y **Clonar** (solo visible para administradores y responsables).

### Crear un acto nuevo

Los administradores y responsables pueden crear un acto nuevo desde el botón **Nuevo acto** en la parte superior derecha. Se abre un modal con el formulario de creación.

![Formulario de creación de acto](/docs/assets/act-editor.png)

Los campos son:

- **Nombre del acto** — Un título descriptivo. Por ejemplo: *Ensayo de Cuadrillas – Semana Santa 2026*.
- **Tipo de acto** — Cabildo, Cultos, Procesión, Ensayo u Otro.
- **Fecha** — El día en que se celebra el acto.
- **Ubicación** — El lugar donde se realiza.
- **Responsable** — El miembro de la hermandad que coordina el acto. Los administradores pueden elegir cualquier miembro; los responsables solo pueden asignarse a sí mismos.
- **Observaciones** — Campo opcional para cualquier detalle adicional.

Al guardar, el acto se crea en estado **Planificación** y aparece en el listado.

---

## Detalle del acto

Es la pantalla central de la aplicación. Se accede pulsando sobre cualquier acto del listado o del dashboard.

![Cabecera del detalle de un acto](/docs/assets/act-detail-header.png)

### Cabecera

La cabecera muestra el nombre del acto, la fecha, la ubicación, el responsable asignado y el estado actual. En la parte derecha hay dos acciones: **Exportar acta** y, si tienes permisos, **Clonar**.

Justo debajo, si hay elementos pendientes de atención, aparece un aviso en negro con el recuento y un enlace directo al primer elemento pendiente.

### Barra de progreso

Debajo de la cabecera hay una barra de progreso que refleja el avance real del acto. Los pasos son: **Planificación → Preparación → Confirmación → Cierre → Cerrado**. El avance entre estados lo realizan administradores y responsables desde el botón **Cerrar acto** que aparece en el pie de página cuando el acto está listo.

### Las pestañas

El cuerpo del detalle está organizado en cuatro pestañas.

---

#### Pestaña Tareas

Muestra todas las tareas asociadas al acto en formato de tabla.

![Pestaña de tareas en el detalle del acto](/docs/assets/act-detail-tasks.png)

Cada fila muestra: el nombre de la tarea (con descripción o motivo de rechazo si aplica), la persona asignada, el estado actual, la fecha límite y las acciones disponibles.

**Ciclo de vida de una tarea:**

Una tarea recorre los siguientes estados desde que se crea hasta que se completa:

`PLANIFICADA → ACEPTADA → EN PREPARACIÓN → CONFIRMADA → COMPLETADA`

- Cuando se crea, la tarea queda en **Planificada** y espera que el responsable asignado la acepte.
- El responsable puede **Aceptar** o **Rechazar** (con un motivo escrito obligatorio).
- Si la acepta, el estado pasa a **Aceptada**. A partir de ahí, el responsable puede avanzarla a **En Preparación** cuando empiece a trabajar.
- Cuando está lista para revisión, la confirma y pasa a **Confirmada**.
- El último paso es marcarla como **Completada**.

En cualquier punto del ciclo, un administrador o el responsable del acto puede **Revertir** la tarea al estado anterior si algo ha cambiado.

Las tareas rechazadas quedan con el motivo visible en la tabla. No bloquean el cierre del acto porque se considera que están resueltas.

**Crear una tarea nueva:**

Si tienes permisos de escritura (Colaborador, Responsable o Administrador), aparece el botón **+ Añadir tarea** en la parte superior de la pestaña. Al pulsarlo se abre el formulario de elemento.

![Formulario para añadir tarea](/docs/assets/element-form-task.png)

El formulario pide:
- **Título de la tarea**
- **Asignado a** — El miembro responsable de ejecutarla. Los colaboradores solo pueden asignarse a sí mismos.
- **Fecha límite** — Opcional.
- **Notas** — Detalles adicionales. Opcional.

La tarea se crea en estado **Planificada** y el responsable asignado la verá en su sección de **Mis Tareas**.

---

#### Pestaña Decisiones

Registra las decisiones tomadas durante la preparación del acto.

![Pestaña de decisiones en el detalle del acto](/docs/assets/act-detail-decisions.png)

Cada decisión tiene un título descriptivo, el área de la hermandad a la que corresponde (Mayordomía, Secretaría, Priostía, Tesorería o Diputación Mayor), la persona que la revisó y su estado: **Pendiente**, **Aceptada** o **Rechazada**.

Las decisiones pendientes bloquean el cierre del acto. Para resolverlas, un administrador o el responsable del acto puede **Aceptarlas** o **Rechazarlas** directamente desde la fila.

Para registrar una decisión nueva, usa el botón **+ Añadir decisión** (visible si tienes permisos de escritura). El formulario es igual que el de tareas, con un campo adicional para el área de la hermandad.

---

#### Pestaña Incidencias

Registra los problemas o situaciones inesperadas que surgen durante la preparación o ejecución del acto.

![Pestaña de incidencias en el detalle del acto](/docs/assets/act-detail-incidents.png)

Cada incidencia muestra su descripción, quién la reportó, su estado (**Abierta** o **Resuelta**) y, si ya está resuelta, quién la cerró. Las incidencias abiertas bloquean el cierre del acto hasta que se resuelvan.

Para resolver una incidencia, pulsa **Resolver** en la fila correspondiente. La fecha de resolución y tu nombre quedan registrados automáticamente.

---

#### Pestaña Historial

Muestra un registro cronológico de todas las acciones que se han realizado sobre el acto: creación y modificación de tareas, decisiones e incidencias, cambios de estado, rechazos y cierres.

![Pestaña de historial de cambios](/docs/assets/act-detail-history.png)

Cada entrada indica el tipo de entidad afectada (tarea, decisión, incidencia), la acción realizada, quién la ejecutó y cuándo. El historial es paginado y no se puede modificar: es un registro de solo lectura.

---

### Exportar el acta

Desde el enlace **Exportar acta →** en la cabecera del detalle se abre el modal de exportación.

![Modal de exportación de acta](/docs/assets/export-modal.png)

Se puede elegir el formato (**PDF** o **CSV**) y las secciones que se quieren incluir: tareas, decisiones, incidencias y observaciones del acto. Se puede marcar cualquier combinación.

El PDF incluye el nombre y los datos de contacto de la hermandad en la cabecera. El CSV genera una tabla plana apta para abrir directamente en Excel. Al pulsar **Exportar**, el navegador descarga el fichero automáticamente.

---

### Clonar un acto

Disponible para administradores y responsables desde el enlace **Clonar →** en la cabecera del detalle, o desde el botón **Clonar** en la tarjeta del listado.

Al clonar un acto, el sistema crea uno nuevo en estado **Planificación** con el mismo tipo, ubicación y observaciones, y copia todas las tareas en estado **Planificada**. Las decisiones e incidencias no se copian porque son específicas de cada edición.

Es la opción más cómoda para actos que se repiten cada año, como los ensayos o la estación de penitencia, donde la estructura de tareas suele ser similar.

---

### Cerrar un acto

El botón **Cerrar acto** aparece en el pie de página del detalle, visible únicamente para administradores y el responsable del acto.

Al pulsarlo se abre el modal de cierre.

![Modal de cierre de acto](/docs/assets/close-event.png)

Antes de permitir el cierre, el sistema verifica automáticamente que no queden elementos sin resolver. Si hay tareas sin completar (ni rechazar), decisiones pendientes o incidencias abiertas, el modal las lista y el botón de confirmación permanece deshabilitado hasta que se resuelvan.

Cuando todo está en orden, aparece el mensaje *Todos los elementos están resueltos* y se puede confirmar el cierre. Una vez cerrado, el acto pasa al historial y no admite modificaciones. **El cierre es irreversible.**

---

## Mis Tareas

Accesible desde **Actos → Mis Tareas** en el menú lateral. Visible para todos los roles excepto Consulta.

![Vista de Mis Tareas](/docs/assets/my-tasks.png)

Esta sección reúne en un único sitio todas las tareas que tienes asignadas en cualquier acto de la hermandad, sin necesidad de ir acto por acto.

En la parte superior aparecen tres contadores: tareas por confirmar, confirmadas y rechazadas.

Hay filtros por tipo de acto, estado de la tarea y un buscador de texto. Cada tarjeta muestra el nombre de la tarea, el tipo de acto al que pertenece, el estado actual y las acciones disponibles según ese estado:

- Si la tarea está en **Planificada**, puedes **Aceptarla** o **Rechazarla** directamente desde aquí. Si rechazas, debes escribir un motivo.
- Si está en **Aceptada**, puedes marcarla como **En Preparación**.
- Si está en **En Preparación**, puedes **Confirmarla**.
- Si está en **Confirmada**, puedes **Completarla**.

En cualquier caso, hay un enlace **Ver más info →** que lleva directamente a la fila de la tarea dentro del acto correspondiente, con el detalle completo.

---

## Historial

Accesible desde **Historial** en el menú lateral. Muestra todos los actos cerrados de la hermandad organizados en una línea de tiempo por fecha de celebración.

![Vista de historial de actos](/docs/assets/act-history.png)

Los filtros disponibles son: tipo de acto, responsable asignado y rango de fechas (desde / hasta). También hay un buscador de texto. Los actos aparecen agrupados por fecha, con el tipo y el estado de cada uno.

Pulsando sobre el nombre de cualquier acto cerrado se accede a su detalle en modo de solo lectura.

---

## Usuarios y Roles

Accesible desde **Usuarios** en el menú lateral. Solo visible para **administradores**.

![Gestión de usuarios y roles](/docs/assets/users.png)

### Listado de usuarios

En la parte superior aparecen cuatro contadores: administradores, responsables, colaboradores y usuarios de consulta. Debajo hay un listado paginado con todos los miembros de la hermandad, que muestra el nombre, el correo, el rol, el estado (activo o inactivo) y la fecha del último acceso.

Se puede filtrar por rol, por estado y buscar por texto.

### Acciones sobre usuarios

Desde la columna de acciones de cada fila se puede:

- **Editar** — Cambia el nombre, el correo o el rol del usuario.
- **Desactivar / Reactivar** — Un usuario desactivado no puede acceder a la aplicación. Se puede volver a activar en cualquier momento.
- **Eliminar** — Solo disponible si el usuario ya está desactivado. La eliminación es permanente.

No es posible desactivarse ni eliminarse a uno mismo.

### Crear un usuario nuevo

El botón **Crear usuario** en la parte superior derecha abre el mismo formulario de registro, embebido como modal dentro de la pantalla de usuarios. Se pueden crear usuarios con cualquier rol y asignarlos directamente a la hermandad sin que tengan que pasar por el proceso de registro público.

### Matriz de permisos

Al final de la página de usuarios hay una tabla que muestra qué puede hacer cada miembro según su rol: ver, crear, gestionar, cerrar actos, eliminar, etc. Es útil para tener una referencia rápida de los accesos sin tener que consultar nada por separado.

---

## Configuración

Accesible desde **Configuración** en el menú lateral. Visible para todos los roles, aunque solo los administradores pueden modificar la información.

![Pantalla de configuración de la hermandad](/docs/assets/settings.png)

La pantalla está dividida en dos bloques.

**El formulario principal** permite al administrador editar los datos de la hermandad: nombre, descripción, año de fundación, localidad, dirección de la sede, correo de contacto y teléfono de contacto. Esta información se usa en la cabecera de los documentos PDF exportados. Los usuarios sin rol de administrador ven los datos en modo de solo lectura con un aviso informativo.

**El panel lateral** muestra información del sistema: número de miembros activos, número de actos registrados, fecha de creación de la hermandad en la plataforma y fecha de la última modificación.

### Disolver la hermandad

En la parte inferior del panel lateral aparece una zona de peligro exclusiva para el administrador: la opción de **disolver la hermandad**. Antes de ejecutarla, el sistema pide confirmación explícita con un diálogo. La operación borra de forma permanente la hermandad y todos sus datos: actos, tareas, decisiones, incidencias y miembros. **No tiene vuelta atrás.**

---

## Mi perfil

Accesible pulsando el icono de lápiz en la parte inferior del menú lateral. Abre un modal sobre cualquier pantalla.

![Modal de perfil de usuario](/docs/assets/profile-modal.png)

Desde aquí puedes:

- **Cambiar la foto de perfil** — Se puede subir una imagen arrastrándola sobre el área de avatar o pulsando en la zona de carga. Formatos aceptados: PNG, JPG, WEBP o GIF, hasta 2 MB. Una vez seleccionada la imagen, hay que confirmarla explícitamente con el botón **Guardar foto**.
- **Actualizar nombre y correo** — Edita tu nombre completo y tu dirección de correo electrónico.
- **Cambiar contraseña** — Requiere introducir la contraseña actual como confirmación.

---

## Solicitar cambio de administrador

Si eres responsable o colaborador y necesitas que el administrador de tu hermandad cambie (por ejemplo, porque la persona que tiene el rol de administrador ya no está disponible), puedes enviar una solicitud al equipo de ActaCofrade desde el menú lateral, en la opción **Cambio de admin**.

![Modal de solicitud de cambio de administrador](/docs/assets/contact-modal.png)

Se abre un modal donde puedes escribir el motivo de la solicitud (máximo 2000 caracteres). La solicitud queda registrada y será revisada por el super administrador del sistema.

---

## Panel del super administrador

Solo accesible para el rol **Super Administrador**, que existe fuera de cualquier hermandad y gestiona la plataforma a nivel global.

![Panel del super administrador](/docs/assets/super-admin.png)

Este panel muestra las solicitudes de cambio de administrador recibidas de las hermandades. Para cada solicitud se puede ver el motivo y elegir un nuevo administrador de entre los miembros elegibles de esa hermandad. El super administrador puede aprobar o rechazar cada solicitud desde el panel de detalle que aparece al seleccionarla.

---

## Casos de uso habituales

### Caso 1: Preparar un ensayo de cuadrillas desde cero

Una hermandad tiene que organizar un ensayo de cuadrillas tres semanas antes de la Semana Santa. El secretario, que tiene rol de administrador, entra en la aplicación y sigue estos pasos:

1. Va a **Actos → Registro de Actos** y pulsa **Nuevo acto**.
2. Rellena el formulario: nombre (*Ensayo de Cuadrillas – marzo 2026*), tipo (*Ensayo*), fecha, ubicación (la sede de la hermandad) y asigna como responsable al mayordomo. Guarda.
3. Abre el acto recién creado y va a la pestaña **Tareas**.
4. Añade una tarea: *Preparar cuadrilla de costaleros* — asignada al capataz, con fecha límite tres días antes del ensayo.
5. Añade otra tarea: *Confirmar asistencia de músicos* — asignada al director de banda.
6. Añade una decisión en la pestaña **Decisiones**: *Cambiar recorrido interno por obras en la nave lateral* — área Mayordomía.
7. Cierra el navegador. El capataz y el director de banda recibirán sus tareas en **Mis Tareas** la próxima vez que entren.

El capataz entra a la aplicación y en **Mis Tareas** ve la tarea asignada. La acepta, y cuando empieza a trabajar en ella la avanza a **En Preparación**. El día antes del ensayo la confirma y finalmente la completa.

Cuando todas las tareas están completadas o rechazadas y la decisión está revisada, el administrador puede cerrar el acto desde el botón **Cerrar acto** del detalle.

---

### Caso 2: Gestionar una incidencia durante la estación de penitencia

A mitad de la estación de penitencia, el secretario recibe un aviso de que un costalero ha tenido que abandonar y hay que rehacer una cuadrilla. Entra en la aplicación desde el móvil y lo registra:

1. Abre el acto de la estación de penitencia desde el dashboard.
2. Va a la pestaña **Incidencias** y pulsa **+ Añadir incidencia**.
3. Describe la incidencia: *Baja por lesión en segunda levantá — rehecha cuadrilla con sustituto*.
4. Guarda. La incidencia queda registrada con su nombre y la hora.

Cuando todo se resuelve y el acto termina, el secretario abre la incidencia y pulsa **Resolver**. La fecha de resolución y su nombre quedan registrados. El acto ya puede cerrarse.

---

### Caso 3: Clonar el ensayo del año pasado

La hermandad va a repetir el ensayo general que organizó el año anterior. En lugar de crearlo desde cero, el responsable:

1. Va al **Historial** y localiza el ensayo del año pasado.
2. Abre el detalle del acto y pulsa **Clonar →**.
3. El sistema crea un acto nuevo en estado Planificación con el mismo tipo, ubicación y todas las tareas copiadas en estado Planificada.
4. El responsable edita la fecha, revisa las tareas y reasigna las que hayan cambiado de responsable.

En cinco minutos tiene el acto preparado sin tener que volver a escribirlo todo.

---

### Caso 4: Exportar el acta de un cabildo

Terminado el cabildo, el secretario quiere compartir un resumen con todos los hermanos por WhatsApp:

1. Abre el detalle del cabildo y pulsa **Exportar acta →**.
2. Selecciona formato **PDF** y marca las secciones: tareas, decisiones e incidencias.
3. Pulsa **Exportar**. El PDF se descarga automáticamente.
4. El fichero incluye en la cabecera el nombre y los datos de contacto de la hermandad, y lista todas las tareas con sus responsables, las decisiones tomadas y las incidencias que surgieron.

Lo sube a un grupo de WhatsApp y cualquier hermano puede consultarlo sin necesidad de entrar en la aplicación.

---

### Caso 5: Un colaborador gestiona sus tareas del día

Un colaborador entra a la aplicación para ver qué tiene pendiente:

1. El dashboard le muestra las alertas activas. Ve que tiene una tarea en *Planificada* esperando su aceptación.
2. Va a **Mis Tareas** para verlas todas de golpe.
3. Acepta la tarea pendiente. Otra que ya estaba en marcha la avanza a **En Preparación**.
4. Usa el enlace **Ver más info →** en una tarjeta para ver el contexto completo dentro del acto.

Todo desde una sola pantalla, sin tener que abrir acto por acto.

---

## Preguntas frecuentes

**¿Puedo usar ActaCofrade desde el móvil?**
Sí. La interfaz se adapta a pantallas de cualquier tamaño. El menú lateral se convierte en un panel deslizante accesible desde el botón de cabecera. Las tablas se reorganizan verticalmente para que sea cómodo leerlas desde un teléfono.

**¿Qué pasa si rechazo una tarea sin querer?**
El administrador o el responsable del acto puede **revertir** cualquier tarea al estado anterior desde la columna de acciones en la pestaña de Tareas. Las tareas rechazadas no se borran: quedan registradas con el motivo del rechazo en el historial.

**¿Puedo editar un acto una vez creado?**
Sí, mientras no esté cerrado. Entra en el detalle del acto y, si tienes los permisos necesarios (administrador o responsable), podrás editar sus datos principales desde el mismo flujo de edición.

**¿Por qué el botón de cerrar acto está deshabilitado?**
El sistema bloquea el cierre cuando quedan elementos sin resolver. Al pulsar el botón aparece el listado exacto de lo que falta: tareas sin completar, decisiones pendientes o incidencias abiertas. Resuelve cada uno de ellos y el botón se activará automáticamente.

**¿Qué diferencia hay entre el Historial y el Registro de Actos?**
El Registro de Actos muestra los actos activos, es decir, los que están en alguna fase de preparación (Planificación, Preparación, Confirmación o Cierre). El Historial muestra los actos ya cerrados de forma definitiva, que ya no admiten cambios pero pueden consultarse.

**¿Se puede recuperar un acto cerrado?**
No. El cierre es una operación irreversible y así está diseñado a propósito: la idea es que un acto cerrado sea un registro permanente e inmutable. Si necesitas trabajar de nuevo con la misma estructura, usa la opción de **Clonar** para crear un acto nuevo a partir del cerrado.

**¿Qué pasa si el administrador de mi hermandad ya no está disponible?**
Si tienes acceso a la aplicación pero no eres administrador, puedes enviar una solicitud de cambio de administrador desde la opción **Cambio de admin** en el menú lateral. El equipo de ActaCofrade revisará la solicitud y actualizará el rol.

**¿Puedo cambiar mi propio rol?**
No directamente. Solo el administrador de la hermandad puede cambiar el rol de un usuario desde la sección **Usuarios**. Si crees que tu rol no es el correcto, habla con el administrador de tu hermandad.

**¿Qué ocurre si desactivo a un usuario?**
Un usuario desactivado no puede iniciar sesión ni acceder a la aplicación. Sus datos (nombre, tareas asignadas, decisiones revisadas) se mantienen en el historial. Se puede volver a activar en cualquier momento.

**¿Cuál es la diferencia entre PDF y CSV en la exportación?**
El PDF está pensado para compartir: incluye la cabecera con los datos de la hermandad y presenta la información de forma legible. El CSV está pensado para procesar los datos: genera una tabla plana que se puede abrir en Excel o Google Sheets para hacer cálculos o análisis propios.
