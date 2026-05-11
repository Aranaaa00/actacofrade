# 10. Conclusiones

## Evaluación crítica de los objetivos

Este proyecto nació de algo que había visto de cerca durante años: que en las hermandades todo funciona a base de WhatsApp y de que dos o tres personas se acuerden de todo. Cuando una de esas personas no está, el caos está garantizado. Nadie sabe qué había que hacer, quién lo tenía que hacer ni qué se decidió en la última reunión. La idea de ActaCofrade era simple: que eso dejara de pasar. Que todo quedara escrito.

Y mirando el resultado, creo que eso se consiguió. La aplicación hace lo que se prometía: puedes crear un acto, asignar tareas a personas concretas, anotar las decisiones que se toman, registrar los problemas que van saliendo, y al final cerrar el acto sabiendo que no hay nada sin resolver. Nada en el WhatsApp, nada "pendiente de confirmar", nada que dependa de que alguien se acuerde.

Ahora bien, no todo salió como lo había planeado. Hay una parte del proyecto que subestimé bastante: los permisos de cada tipo de usuario. En la propuesta lo puse como una semana de trabajo y lo describí en cuatro líneas. Con el tiempo que tardé en hacerlo bien, me paarece ahora poco esa estimación. La idea de cuatro tipos de usuario parece sencilla hasta que te pones a pensar en todos los casos: un colaborador puede crear tareas pero solo puede asignárselas a él mismo, no a otra persona. Un responsable puede gestionar un acto si lo tiene asignado, pero no puede tocar el de otro responsable. Un usuario de solo lectura no puede aparecer como responsable de nada. Todo eso parece pequeño por separado pero junto es una cantidad de casos que me comí más de una tarde entera. Fue sin duda la parte que más veces tuve que corregir porque cada vez que tocaba algo, algo que funcionaba antes dejaba de funcionar.

Lo que sí sorprendió para bien fue el flujo de cierre del acto. En la propuesta lo describo como "verificación antes de cerrar" y queda muy vago. Lo que quedó al final es bastante más útil: el sistema te muestra exactamente qué falta, tarea por tarea, incidencia por incidencia. No puedes cerrar si hay algo sin resolver. Ese bloqueo, que parece una tontería, es en realidad lo que más diferencia a esto de seguir usando un grupo de WhatsApp.

Y luego hay algo que no estaba en la propuesta inicial pero que acabó entrando: el historial de cambios. Cada acción que alguien hace sobre un acto queda registrada con su nombre y la fecha. No lo tenía planificado, pero mientras construía la aplicación me di cuenta de que sin eso estaba prometiendo trazabilidad a medias. Si alguien cambia una tarea y nadie sabe quién fue ni cuándo, ¿de qué sirve tener la trazabilidad? Así que lo metí, aunque me costó tiempo que no tenía.

---

## Grado de cumplimiento del alcance propuesto

Todo lo que estaba como obligatorio en la propuesta está hecho. Los usuarios con sus roles, el ciclo de vida de los actos, las tareas con confirmación de quien las tiene asignadas, las decisiones, las incidencias, el control antes de cerrar, la exportación en PDF y CSV, los filtros y el buscador. Todo.

Las dos opcionales también: clonar actos y los avisos automáticos en el panel de inicio.

Donde la propuesta fue más optimista fue en los tiempos. El PDF, por ejemplo, lo tenía estimado en media semana y tardó algo más. Conseguir que el documento quedara decente y bien organizado no es tan rápido como parece. Y el despliegue, que en la propuesta apenas lo mencioné, acabó siendo algo que me llevó rtambién algo de más tiempo montar bien desde cero: el servidor, la automatización para que se construya y publique solo cuando subo código nuevo, los comprobaciones de que todo arranca correctamente...

Lo que desde el principio quedó fuera y sigue fuera: la gestión de hermanos, las cuotas, la contabilidad, las papeletas de sitio. Eso ya lo hacen otras aplicaciones y lo hacen bien. ActaCofrade no intenta competir con eso. Va a por el hueco que esas aplicaciones dejan: organizar los actos, hacer seguimiento de lo pendiente, dejar constancia de lo que se decide. Ese hueco existía antes y sigue sin estar cubierto por nadie más.

---

## Mejoras futuras

Si le diera más tiempo al proyecto, hay tres cosas que metería.

**Que te avise por correo cuando te asignan algo.** Ahora mismo, si alguien te pone una tarea, te enteras solo si abres la aplicación. Si no la abres, no lo sabes, y volvemos al punto de partida: alguien tiene que avisarte por fuera. Con un correo automático cuando te asignan algo o cuando se acerca la fecha límite, el sistema pasa a ser algo que te avisa, no algo que tienes que ir a consultar. Es probablemente lo que más cambiaría el uso real en el día a día.

**Ver el historial de cada tarea o decisión por separado.** Ahora el historial existe, pero es el del acto completo. Si quiero saber qué pasó con una tarea concreta tengo que buscarla entre todo lo demás. Lo que faltaría es que al abrir una tarea puedas ver directamente su recorrido: cuándo se creó, quién la aceptó, en qué fecha cambió de estado. Los datos ya están guardados, solo falta mostrarlo de forma más cómoda.

**Poder estar en más de una hermandad con la misma cuenta.** Hay personas que participan en dos hermandades distintas. Ahora mismo tendrían que crearse dos cuentas, una para cada hermandad, que es un poco absurdo. Sería la mejora más técnicamente costosa de las tres porque toca bastante de cómo está montado el sistema por dentro, pero también sería la que más sentido tendría para que la aplicación funcione bien en el mundo cofrade real, donde no es raro que alguien esté en más de una.

---

## Lecciones aprendidas

Esto es lo que me llevo del proyecto y lo que no olvidaré en el siguiente.

**Diseñar las pantallas antes de ponerte a programar es tiempo bien gastado.** Antes de escribir código dediqué un tiempo a diseñar en Figma cómo iba a quedar todo. Al principio me daban ganas de saltármelo e ir directo al editor, porque diseñar no parece "avanzar". Pero cuando llegué a construir cada pantalla, ya sabía exactamente qué tenía que hacer y cómo tenía que quedar. Tuve que hacer algunos retoques de como quedo todo diseñado en Figma, pero ya cada ideaa de cada mockups estaba bastante clara. Finalmente quedó tod bastante parecido a como lo diseñé. Si me hubiera puesto a programar sin ese paso previo, habría perdido mucho más tiempo dando marcha atrás.

**Lo que escribes en dos líneas puede costarte dos semanas.** Los permisos de usuario son el ejemplo más claro de esto. En la propuesta quedaban en una tabla pequeña y parecían algo menor. Al implementarlos fue la parte más complicada de todo el proyecto. Cuatro roles, cada uno con sus reglas, y cada regla con sus excepciones. Y cuando arreglabas un caso, aparecía otro que no habías pensado. Me enseñó a no fiarme de lo que parece simple antes de meterme en ello.

**Los tests me iba verificando todo el proceso.** Al principio los hacía porque había que hacerlos, sin más. Pero llegó un momento en el que entendí para qué sirven de verdad. Cada vez que tocaba algo que ya funcionaba para añadir una cosa nueva, los tests me avisaban al momento si había roto algo por el camino. Sin ellos, me habría enterado mucho más tarde, probando a mano y preguntándome por qué algo que antes iba bien de repente no iba. Pasó varias veces y cada vez agradecí tenerlos.

**Cuando llevas un rato bloqueado, lo mejor es levantarte.** Hay momentos en los que llevas tiempo con algo que no funciona y en vez de salir del bucle sigues añadiendo cosas encima. Eso casi nunca funciona. Lo que sí me funcionó fue parar, hacer otra cosa, y volver con la cabeza más limpia. No es fácil cuando ya llevas tiempo con algo y quieres acabarlo, pero casi siempre que lo hice llegué a la solución mucho antes que si hubiera seguido empujando.

**Que el proyecto tenga un problema real detrás lo cambia todo.** ActaCofrade no surgió de buscar una idea para el TFG. Surgió de ver el problema de cerca, de conocer las hermandades por dentro. Eso hizo que en muchos momentos del desarrollo tuviera claro qué decisión tomar, porque podía preguntarme si lo que estaba haciendo servía para algo o no. Cuando el proyecto lo entiendes de verdad, trabajar en él es diferente. No lo haces para que funcione, lo haces para que ayude.

