# Feature Specification: Reserva de Pasajes de Bus VRAEM

**Feature Branch**: `001-reserva-pasajes-bus`

**Created**: 2026-07-15

**Status**: Draft

**Input**: User description: "Sistema de reserva de pasajes de bus para rutas Ayacucho - VRAEM (San Francisco, Sivia, Santa Rosa, Kimbiri, Pichari). Usuario pasajero puede: registrarse e iniciar sesión (email y contraseña); ver lista de viajes disponibles (ruta, fecha, hora, precio); elegir un viaje y ver un mapa simple de asientos (libre/ocupado); seleccionar uno o más asientos e ingresar nombre y DNI del pasajero; pagar el total (mediante yape o plin); ver su boleto con código de reserva único. Administrador puede: iniciar sesión con rol ADMIN; crear, editar y eliminar viajes (ruta, fecha, hora, bus, precio, número de asientos); ver lista de reservas y su estado (pendiente/pagado). Datos: Usuario (dni, nombre, email, contraseña encriptada, rol), Camioneta (id, placa, ruta), Viaje (id, origen, destino, fecha, hora, camioneta, precio), Asiento (id, viaje, número, estado libre/reservado/pagado), Reserva (id, usuario, viaje, asientos elegidos, monto total, estado, fecha), Pago (id, reserva, método, estado, referencia). Reglas: un asiento no puede ser reservado por dos personas al mismo tiempo; una reserva sin pago confirmado en cierto tiempo debe liberar el asiento; solo el ADMIN puede crear o modificar viajes."

## Clarifications

### Session 2026-07-15

- Q: ¿El DNI ingresado (por el usuario al registrarse o por cada pasajero al reservar) debe validarse solo por formato, o el sistema debe verificarlo contra un servicio externo (p. ej. RENIEC)? → A: Solo validación de formato (8 dígitos numéricos), sin verificación externa.
- Q: ¿Un pasajero puede ver el detalle (nombre/DNI) de reservas de otros pasajeros, o solo de las suyas propias? → A: Un pasajero solo ve el detalle (incluido el DNI) de sus propias reservas; el ADMIN ve todas.
- Q: ¿El DNI debe ser único entre cuentas de Usuario (como el email), o no tiene restricción de unicidad? → A: El DNI MUST ser único entre cuentas de Usuario.

### Session 2026-07-15 (revisión post-implementación)

Tras probar el sistema implementado, se identificaron vacíos de la spec original y se resolvieron las siguientes decisiones antes de corregir el código:

- Q: ¿La selección de asientos debe seguir permitiendo elegir varios asientos en una sola reserva, o cambiar a un flujo de un asiento a la vez? → A: Se mantiene la selección múltiple (FR-006/FR-007 sin cambios); se añade una confirmación explícita antes de enviar la reserva.
- Q: ¿El pago puede cubrir solo un asiento de la reserva (pago parcial) o siempre el monto total? → A: El pago sigue cubriendo siempre el monto total de la reserva completa (sin cambios al modelo Pago↔Reserva 1 a 1).
- Q: ¿"Chofer" y "Ruta" deben ser entidades nuevas con CRUD propio en el panel ADMIN? → A: No. "Chofer" se agrega como un campo de texto simple del Viaje (sin gestión propia). "Ruta" sigue siendo el origen/destino ya existente de cada Viaje; no se crea una entidad `Ruta` separada.
- Q: ¿Cómo se entrega la contraseña temporal al recuperar acceso, sin servidor de correo? → A: Se muestra una única vez en pantalla tras enviar el formulario de recuperación.

### Session 2026-07-15 (segunda revisión — reemplaza decisiones de la revisión anterior)

El usuario probó el sistema de nuevo y pidió revertir dos decisiones tomadas en la revisión anterior, además de agregar gestión visible de viajes y usuarios en el panel ADMIN:

- Q: ¿Se mantiene la selección múltiple de asientos (decisión de la revisión anterior) o se cambia a un asiento por clic con confirmación? → A: Se cambia a **un asiento por clic**: el pasajero hace clic en un asiento libre, confirma con nombre/DNI, y esa reserva queda vinculada a exactamente ese asiento. Para reservar varios asientos, repite el flujo una vez por asiento. **Reemplaza** la decisión de la revisión anterior de mantener selección múltiple.
- Q: ¿Sigue siendo configurable el número de asientos por viaje (decisión de la revisión anterior) o pasa a ser fijo? → A: Pasa a ser **fijo en 4 asientos** por camioneta (1 adelante junto al chofer, 3 juntos atrás), representados visualmente. Ya no se pregunta "número de asientos" al crear/editar un viaje. **Reemplaza** la Assumption "Mapa de asientos" de la especificación original (que decía "sin modelar la distribución física real del bus").
- Q: ¿El ADMIN necesita una pantalla para ver/editar/eliminar viajes ya creados? → A: Sí. Se agrega "Gestionar viajes" en el panel ADMIN (antes no existía ningún enlace en la interfaz hacia las funciones de editar/eliminar, aunque las rutas ya existían).
- Q: ¿El ADMIN necesita ver la lista de usuarios registrados? → A: Sí. Se agrega una vista de usuarios (DNI, nombre, email, rol) en el panel ADMIN.

### Session 2026-07-16 (tercera revisión — navegación, pago, seguridad de cuenta y archivado)

El usuario probó el sistema por tercera vez y reportó 13 puntos pendientes. Antes de programar se confirmaron 3 decisiones bloqueantes:

- Q: ¿Cuántos intentos fallidos de login consecutivos bloquean una cuenta? → A: **5 intentos**. Un login exitoso o una recuperación de contraseña exitosa resetean el contador a 0.
- Q: ¿La recuperación de contraseña debe enviar un correo real ahora que se pidió explícitamente? → A: No hay credenciales SMTP disponibles todavía; se mantiene el mecanismo actual (mostrar la clave temporal en pantalla), documentado como limitación temporal hasta contar con un proveedor de correo real.
- Q: ¿Cómo decide el ADMIN archivar un viaje lleno? → A: Botón manual del ADMIN en "Gestionar viajes", habilitado únicamente cuando el viaje está COMPLETO (sin asientos libres).

Se confirmó además que los siguientes puntos reportados ya estaban satisfechos por el diseño existente y no requirieron cambios: reservar tantos pasajes como se quiera (cada Reserva es de un asiento, se repite el flujo por cada uno); el estado "pendiente" hasta que el ADMIN confirme el pago (comportamiento ya vigente); y que el ADMIN nunca pueda editar ni eliminar los datos de un pasajero de una reserva ya creada (esa funcionalidad nunca existió y el archivado de viajes no la introduce).

### Session 2026-07-16 (rechazo de pago)

El usuario pidió agregar la contraparte de "Confirmar pago": la posibilidad de que el ADMIN rechace un pago cuando la referencia no es válida u otro motivo, liberando el asiento y avisando al pasajero.

- Q: ¿El ADMIN debe poder escribir un motivo de rechazo visible para el pasajero? → A: Sí, motivo de texto libre y opcional; si se deja vacío, el pasajero ve un mensaje genérico.

### Session 2026-07-16 (historial, navegación de boleto, venta en efectivo, referencia y sesión única)

Cuarta ronda de requisitos (numerados 6-15 por el usuario). Los puntos 14 y 15 ya estaban completamente satisfechos por el rechazo de pago de la sesión anterior; se confirman sin cambios de código. Decisiones confirmadas antes de programar:

- Q: ¿Cómo se determina que una reserva es "histórica" (viaje ya realizado)? → A: Se calcula al vuelo (sin campo `archivada` ni tarea programada): una reserva es histórica cuando está `PAGADO` y la fecha de su viaje ya pasó. Mantiene el esquema mínimo.
- Q: ¿Quién queda como "usuario comprador" en una venta presencial en efectivo, si el cliente no tiene cuenta? → A: La cuenta del propio ADMIN que realiza la venta.
- Q: ¿Cómo se bloquean las sesiones duplicadas de una misma cuenta? → A: Mecanismo estándar de Spring Security (una sola sesión activa por **cuenta**, no por dispositivo real, ya que el fingerprinting de dispositivo no es confiable en la web); el nuevo login expulsa a la sesión anterior. Se confirmó que esto permite que cuentas distintas (p. ej. un ADMIN y un PASAJERO) sigan usando el mismo dispositivo simultáneamente, ya que la restricción es por cuenta y no por navegador.

### Session 2026-07-16 (consolidación: método de pago del ADMIN en la pantalla compartida de asiento)

Quinta ronda (requisitos 16-19). Refina la venta presencial en efectivo (FR-036, sesión anterior): en vez de una pantalla dedicada solo para el ADMIN, el selector de método de pago se integra en la misma pantalla de "confirmar asiento" que ya usan los pasajeros (accesible por el ADMIN vía "Ver viajes publicados" en su panel), evitando dos caminos distintos para la misma acción.

- Q: ¿Se mantiene la pantalla dedicada "Vender en efectivo" además del nuevo selector en la pantalla compartida? → A: No. Se elimina la pantalla dedicada y se consolida en un único flujo de reserva (Principio VIII, alcance estricto: sin funcionalidad duplicada).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Comprar un pasaje de principio a fin (Priority: P1)

Un pasajero se registra o inicia sesión, consulta los viajes disponibles hacia su destino en el VRAEM, elige un viaje, hace clic en un asiento libre del mapa visual de la camioneta (4 asientos: 1 adelante, 3 atrás), confirma con el nombre y DNI del pasajero que viajará, paga mediante Yape o Plin, y obtiene su boleto con un código de reserva único. Si quiere reservar varios asientos, repite el flujo una vez por asiento.

**Why this priority**: Es el flujo de negocio central del sistema; sin esta capacidad completa de extremo a extremo el sistema no genera ningún valor. Corresponde al flujo end-to-end priorizado por el proyecto (login → reservar → pagar → ver boleto).

**Independent Test**: Puede probarse completamente registrando un usuario nuevo, reservando un asiento libre de un viaje existente, pagando con una referencia de Yape/Plin, y verificando que se genera un boleto con código de reserva único y que el asiento pasa de "libre" a "reservado"/"pagado".

**Acceptance Scenarios**:

1. **Given** un pasajero no registrado, **When** se registra con email y contraseña e inicia sesión, **Then** accede a la lista de viajes disponibles.
2. **Given** un pasajero autenticado viendo el mapa visual de asientos de un viaje (1 adelante, 3 atrás), **When** hace clic en un asiento libre, **Then** el sistema le pide confirmar la reserva de ese asiento e ingresar nombre y DNI del pasajero, mostrando el monto a pagar (precio del viaje).
3. **Given** la confirmación de un asiento con datos de pasajero completos, **When** el pasajero acepta, **Then** el sistema registra la reserva en estado "pendiente" para ese asiento y lo bloquea para que nadie más pueda seleccionarlo, y lleva al pasajero a la pantalla de pago.
4. **Given** una reserva "pendiente", **When** el pasajero paga indicando método (Yape o Plin) y referencia de la operación, **Then** el sistema registra el pago asociado a la reserva, pendiente de confirmación del ADMIN.
5. **Given** una reserva cuyo pago fue confirmado, **When** el pasajero consulta su boleto, **Then** ve un código de reserva único junto con ruta, fecha, hora, asiento y datos del pasajero.
6. **Given** dos pasajeros que intentan hacer clic y confirmar el mismo asiento libre casi al mismo tiempo, **When** ambos confirman, **Then** solo uno logra reservarlo y el otro recibe un aviso de que el asiento ya no está disponible.

---

### User Story 2 - Publicar y mantener viajes disponibles (Priority: P2)

Un administrador inicia sesión con rol ADMIN y crea, edita o elimina viajes (ruta, fecha, hora, camioneta, chofer, precio; el número de asientos es fijo en 4), de modo que existan viajes vigentes para que los pasajeros puedan reservar, y puede gestionarlos desde una pantalla dedicada en su panel.

**Why this priority**: Sin viajes publicados por el administrador no hay nada que los pasajeros puedan reservar; es el prerrequisito de contenido para la Historia 1, pero no es tan crítico como el flujo de compra en sí.

**Independent Test**: Puede probarse iniciando sesión como ADMIN, creando un viaje nuevo, verificando que aparece de inmediato en la lista de viajes disponibles para el pasajero, editando su precio u horario desde "Gestionar viajes", y confirmando que los cambios se reflejan.

**Acceptance Scenarios**:

1. **Given** un usuario con rol ADMIN autenticado, **When** crea un viaje indicando origen, destino, fecha, hora, camioneta (bus), chofer y precio, **Then** el viaje aparece de inmediato en la lista de viajes disponibles para los pasajeros, con sus 4 asientos generados automáticamente (1 adelante, 3 atrás), y el ADMIN ve un mensaje de éxito confirmando la creación.
2. **Given** un viaje existente sin reservas pagadas, **When** el ADMIN lo edita desde "Gestionar viajes" (fecha, hora, precio, camioneta o chofer), **Then** los cambios se reflejan inmediatamente para los pasajeros.
3. **Given** un usuario autenticado sin rol ADMIN, **When** intenta crear, editar o eliminar un viaje, **Then** el sistema rechaza la acción.
4. **Given** un viaje con reservas pagadas asociadas, **When** el ADMIN intenta eliminarlo, **Then** el sistema impide la eliminación y explica el motivo.
5. **Given** un ADMIN autenticado, **When** abre "Gestionar viajes" en su panel, **Then** ve todos los viajes (pasados y futuros) con acciones para editarlos, eliminarlos, o ver las reservas de ese viaje especifico.

---

### User Story 3 - Supervisar el estado de las reservas (Priority: P3)

Un administrador consulta el listado de reservas realizadas por los pasajeros y su estado (pendiente/pagado) para hacer seguimiento operativo de la venta de pasajes.

**Why this priority**: Aporta visibilidad de negocio sobre lo ya construido en las Historias 1 y 2, pero el sistema puede operar (venderse pasajes) sin este panel; es una mejora de control, no del flujo transaccional en sí.

**Independent Test**: Puede probarse generando reservas de prueba en distintos estados (pendiente y pagado) e iniciando sesión como ADMIN para verificar que el listado muestra correctamente cada reserva y su estado actual.

**Acceptance Scenarios**:

1. **Given** existen reservas en distintos estados, **When** el ADMIN abre el listado de reservas, **Then** ve cada reserva con su viaje, pasajero (nombre y DNI), comprador (nombre y email), asiento, monto, estado (pendiente/pagado) y fecha.
2. **Given** una reserva pendiente cuyo plazo de pago venció, **When** el ADMIN consulta el listado, **Then** la reserva aparece con un estado que refleja que fue liberada/expirada y sus asientos vuelven a estar libres para otros pasajeros.
3. **Given** un ADMIN autenticado, **When** abre el listado de reservas desde un viaje especifico en "Gestionar viajes", **Then** ve solo las reservas de ese viaje.
4. **Given** un ADMIN autenticado, **When** abre la vista de usuarios en su panel, **Then** ve la lista de todos los usuarios registrados con DNI, nombre, email y rol.

---

### Edge Cases

- ¿Qué ocurre si dos pasajeros seleccionan el mismo asiento casi simultáneamente? El sistema debe garantizar que solo una de las reservas tenga éxito y notificar al resto que el asiento ya no está disponible.
- ¿Qué ocurre si un pasajero no completa el pago dentro del plazo definido? La reserva pasa a un estado de expirada/cancelada y los asientos que tenía bloqueados vuelven a estar libres.
- ¿Qué ocurre si el ADMIN intenta eliminar un viaje con reservas pagadas? El sistema impide la eliminación.
- ¿Qué ocurre si un pasajero hace clic en un asiento que justo dejó de estar libre (otro lo reservó primero)? El sistema lo redirige al mapa de asientos con un aviso de que ya no está disponible.
- ¿Qué ocurre si el ADMIN crea un viaje con una fecha ya pasada? El sistema rechaza el formulario e indica que la fecha no puede ser anterior a hoy.
- ¿Qué ocurre si un pasajero intenta pagar asientos que ya no están disponibles (por haber expirado su selección)? El sistema rechaza el pago y solicita reiniciar la selección de asientos.
- ¿Qué ocurre si se ingresa un DNI o email con formato inválido durante el registro? El sistema rechaza el registro e indica el campo inválido.
- ¿Qué ocurre si un pasajero intenta registrarse con un email ya usado por otra cuenta? El sistema rechaza el registro e indica que el email ya está en uso.
- ¿Qué ocurre si un pasajero intenta registrarse con un DNI ya usado por otra cuenta? El sistema rechaza el registro e indica que el DNI ya está en uso.
- ¿Qué ocurre si un pasajero intenta ver el detalle de una reserva que no le pertenece? El sistema deniega el acceso.
- ¿Qué ocurre si el nombre ingresado en el registro contiene números o símbolos, o la contraseña no cumple la complejidad mínima? El sistema rechaza el registro e indica el campo inválido.
- ¿Qué ocurre si un viaje ya no tiene asientos libres? El listado de viajes lo muestra como "COMPLETO" y no permite entrar a seleccionar asientos.
- ¿Qué ocurre si un pasajero pide recuperar su contraseña con un email que no existe? El sistema indica que no existe una cuenta con ese email, sin generar ninguna contraseña.
- ¿Qué ocurre si un usuario ingresa una contraseña incorrecta 5 veces seguidas? La cuenta queda bloqueada (incluso si luego ingresa la contraseña correcta) hasta que recupera su contraseña, momento en el que el bloqueo se levanta automáticamente.
- ¿Qué ocurre si el ADMIN intenta archivar un viaje que todavía tiene asientos libres? El sistema rechaza la acción e indica que solo se pueden archivar viajes sin asientos libres.
- ¿Qué ocurre con las reservas y pasajeros de un viaje archivado? Se conservan intactos y siguen siendo consultables desde "Ver archivados"; el archivado nunca borra ni permite editar datos de pasajeros.
- ¿Qué ocurre si el ADMIN rechaza el pago de una reserva pendiente? La reserva pasa a estado "rechazada", el asiento vuelve a "libre" para que otro pasajero pueda reservarlo, y el pasajero ve el motivo (si el ADMIN lo indicó) al consultar esa reserva.
- ¿Qué ocurre si el ADMIN vende un asiento presencialmente (efectivo, Yape o Plin) desde la pantalla de confirmar asiento? La reserva y el pago quedan confirmados ("pagado") de inmediato, sin pasar por el estado "pendiente" de verificación, sin importar el método elegido, ya que el ADMIN recibe/verifica el pago directamente.
- ¿Qué ocurre si el ADMIN intenta confirmar una reserva sin elegir un método de pago? El sistema rechaza el formulario e indica que debe seleccionar un método.
- ¿Qué ocurre si un usuario ya tiene una sesión activa e inicia sesión de nuevo con la misma cuenta (mismo u otro dispositivo)? La sesión anterior se invalida automáticamente; si esa sesión anterior intenta hacer algo, es redirigida al login con un aviso de que se cerró por un nuevo inicio de sesión.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir que un visitante se registre como pasajero indicando DNI, nombre, email y contraseña, y MUST rechazar registros con email o DNI ya existente en otra cuenta, o con DNI/email de formato inválido. El DNI MUST validarse únicamente por formato (8 dígitos numéricos); el sistema no verifica el DNI contra ningún servicio externo (p. ej. RENIEC). El nombre MUST contener solo letras y espacios, con un mínimo de 2 caracteres. La contraseña MUST tener un mínimo de 8 caracteres, con al menos una letra mayúscula y un número.
- **FR-002**: El sistema MUST almacenar la contraseña de cada usuario de forma encriptada (no en texto plano) y MUST permitir iniciar sesión con email y contraseña.
- **FR-003**: El sistema MUST distinguir entre usuarios con rol PASAJERO y rol ADMIN, y MUST restringir las acciones de gestión de viajes exclusivamente al rol ADMIN.
- **FR-004**: El sistema MUST mostrar a los pasajeros autenticados la lista de viajes disponibles con ruta (origen/destino), chofer, fecha, hora y precio. Un viaje sin asientos libres MUST mostrarse como "COMPLETO", sin permitir continuar a la selección de asientos.
- **FR-005**: El sistema MUST mostrar, para un viaje elegido, una representación visual de la camioneta con sus 4 asientos fijos para pasajeros (1 adelante, junto al chofer; 3 juntos atrás), indicando claramente cuáles están libres y cuáles ocupados (reservados o pagados), con colores distintos para cada estado.
- **FR-006**: El sistema MUST permitir a un pasajero hacer clic en un asiento libre del mapa visual para reservarlo; antes de confirmar, MUST pedir el nombre y DNI del pasajero que lo ocupará y una confirmación explícita mostrando el asiento elegido (posición y número) y el monto a pagar. El DNI MUST validarse solo por formato (8 dígitos numéricos), sin verificación externa. Cada reserva cubre exactamente un asiento; para reservar varios, el pasajero repite el flujo una vez por asiento.
- **FR-007**: El monto total de una reserva MUST ser el precio del viaje (una reserva cubre un unico asiento).
- **FR-008**: El sistema MUST impedir que un mismo asiento sea asignado a más de una reserva activa (pendiente o pagada) al mismo tiempo, incluso ante solicitudes simultáneas.
- **FR-009**: El sistema MUST permitir al pasajero pagar el monto total indicando el método (Yape o Plin) y una referencia/número de operación, y MUST registrar el pago asociado a la reserva.
- **FR-010**: El sistema MUST crear la reserva en estado "pendiente" al momento de la selección de asientos y del envío del pago, y MUST bloquear (marcar como reservados) los asientos elegidos mientras la reserva esté pendiente.
- **FR-011**: El sistema MUST liberar automáticamente los asientos de una reserva "pendiente" cuyo pago no fue confirmado dentro del plazo definido (ver Assumptions), devolviéndolos al estado "libre" y marcando la reserva como expirada.
- **FR-012**: El sistema MUST cambiar el estado de la reserva y de sus asientos a "pagado" una vez confirmado el pago correspondiente.
- **FR-013**: El sistema MUST generar un código de reserva único para cada reserva y MUST permitir al pasajero consultar su boleto (ruta, fecha, hora, asientos, pasajeros, código de reserva) una vez pagada.
- **FR-014**: El sistema MUST permitir que el rol ADMIN cree viajes indicando ruta (origen/destino), fecha (no anterior a hoy), hora, camioneta (bus), chofer y precio, y MUST generar automáticamente los 4 asientos fijos del viaje (1 adelante, 3 atrás). El viaje creado MUST aparecer de inmediato en la lista de viajes disponibles para los pasajeros, y el ADMIN MUST recibir una confirmación visible de que se creó correctamente.
- **FR-015**: El sistema MUST permitir que el rol ADMIN edite los datos de un viaje (origen, destino, fecha, hora, camioneta, chofer, precio); el número de asientos no es editable por ser fijo.
- **FR-016**: El sistema MUST permitir que el rol ADMIN elimine un viaje únicamente cuando no tenga reservas pagadas asociadas.
- **FR-017**: El sistema MUST permitir que el rol ADMIN visualice el listado de todas las reservas junto con su estado (pendiente/pagado/expirada) y datos principales (viaje, pasajero(s), monto, fecha).
- **FR-018**: El sistema MUST restringir el acceso a cualquier funcionalidad de creación, edición o eliminación de viajes a usuarios que no tengan rol ADMIN.
- **FR-019**: El sistema MUST restringir la visualización del detalle de una reserva (incluyendo nombre y DNI de los pasajeros) al usuario que la creó y al rol ADMIN; un pasajero MUST NOT poder ver el detalle de reservas hechas por otros usuarios.
- **FR-020**: El sistema MUST permitir que cualquier usuario recupere el acceso a su cuenta indicando su email; si el email existe, MUST generar una contraseña temporal, guardarla de forma encriptada, y mostrarla una única vez en pantalla (no hay envío de correo real en esta versión).
- **FR-021**: Tras iniciar sesión, el sistema MUST dirigir al usuario con rol ADMIN a su panel de administración y al usuario con rol PASAJERO a la lista de viajes, en vez de una única página de bienvenida para todos los roles.
- **FR-022**: El sistema MUST ofrecer, en toda pantalla autenticada, un control visible y funcional para cerrar sesión.
- **FR-023**: El sistema MUST ofrecer al rol ADMIN una pantalla de "Gestionar viajes" que liste todos los viajes (pasados y futuros) con acciones para editar, eliminar, y ver las reservas de cada viaje especifico.
- **FR-024**: El sistema MUST ofrecer al rol ADMIN una vista con la lista de todos los usuarios registrados (DNI, nombre, email, rol).
- **FR-025**: El sistema MUST mostrar mensajes de confirmación visibles al usuario despues de crear/editar/eliminar un viaje, crear una reserva, registrar un pago, y confirmar un pago; y mensajes de error claros cuando una accion no se puede completar.
- **FR-026**: El sistema MUST bloquear una cuenta tras 5 intentos de inicio de sesión fallidos consecutivos, rechazando el acceso aunque luego se ingrese la contraseña correcta, y MUST mostrar un mensaje que indique el bloqueo con un enlace a la recuperación de contraseña. Un login exitoso o una recuperación de contraseña exitosa MUST resetear el contador de intentos fallidos a 0, desbloqueando la cuenta.
- **FR-027**: El sistema MUST permitir a cualquier usuario autenticado ver su perfil (DNI, nombre, email) y cambiar su contraseña, sujeta a la misma regla de complejidad mínima del registro (FR-001).
- **FR-028**: El sistema MUST mostrar, en la pantalla de pago, un código QR (Yape/Plin) junto con el número de destino, para facilitar el pago sin necesidad de teclearlo manualmente.
- **FR-029**: El sistema MUST notificar al ADMIN, en su panel principal, cuando existan reservas pendientes de confirmar pago, con un enlace directo al listado de reservas para revisarlas.
- **FR-030**: El sistema MUST permitir al ADMIN archivar manualmente un viaje únicamente cuando no tenga asientos libres (esté "COMPLETO"). Un viaje archivado MUST dejar de aparecer en la lista de viajes disponibles para pasajeros y en "Gestionar viajes", pero MUST seguir siendo consultable (junto con sus reservas y pasajeros) desde una vista de viajes archivados. El archivado MUST NOT eliminar ni permitir editar los datos de los pasajeros de las reservas asociadas.
- **FR-031**: El sistema MUST ofrecer, en las pantallas de pago y de detalle de boleto/reserva, un control de "volver" que regrese a la pantalla anterior real del navegador, en vez de dirigir siempre a un destino fijo.
- **FR-032**: El sistema MUST permitir al ADMIN rechazar el pago de una reserva pendiente, indicando opcionalmente un motivo en texto libre. Al rechazar, el sistema MUST liberar el asiento asociado (vuelve a "libre") y MUST cambiar el estado de la reserva a "rechazada", permitiendo al pasajero volver a intentar reservar. El pasajero MUST poder ver, al consultar esa reserva, que su pago fue rechazado y el motivo indicado por el ADMIN (si lo hubo).
- **FR-033**: El sistema MUST considerar "histórica" (viaje ya utilizado) a toda reserva en estado "pagado" cuya fecha de viaje ya haya pasado, sin requerir una acción manual de archivado ni borrar ningún dato; esto forma la base de datos histórica de pasajeros que usaron el servicio.
- **FR-034**: El sistema MUST mostrar, en "Mis reservas", un historial del pasajero con las fechas de los viajes ya realizados (reservas históricas, FR-033) y el total de veces que ha viajado.
- **FR-035**: El sistema MUST ofrecer, en la pantalla de comprobante de pago pendiente y en la pantalla de boleto pagado, un botón para volver al menú principal (distinto del control de "volver atrás" ya existente), y MUST ofrecer en "Mis reservas" un botón explícito para ver el boleto de cada reserva pagada.
- **FR-036**: El sistema MUST permitir al ADMIN registrar una venta presencial para un cliente sin cuenta propia (usando la cuenta del ADMIN como comprador) desde la misma pantalla de "confirmar asiento" que usan los pasajeros, ofreciendo un selector de método de pago con tres opciones (Efectivo, Yape, Plin). Cualquiera sea el método elegido, el sistema MUST confirmar el pago y la reserva de inmediato (estado "pagado" desde su creación, sin pasar por "pendiente" ni requerir verificación posterior), ya que el ADMIN recibe/verifica el pago directamente en el momento.
- **FR-039**: El selector de método de pago en la pantalla de "confirmar asiento" MUST mostrarse únicamente cuando quien reserva tiene rol ADMIN; para un usuario con rol PASAJERO, ese selector MUST NOT aparecer, y su reserva MUST seguir el flujo existente (estado "pendiente" hasta que el ADMIN confirme el pago desde el listado de reservas).
- **FR-037**: El sistema MUST validar que el número/código de operación ingresado por el pasajero al pagar (Yape/Plin) tenga exactamente 8 dígitos numéricos, rechazando cualquier otro formato.
- **FR-038**: El sistema MUST impedir que una misma cuenta tenga más de una sesión activa simultánea; al iniciar sesión de nuevo con esa cuenta, la sesión anterior MUST invalidarse automáticamente. Esta restricción es por cuenta, no por dispositivo: cuentas distintas pueden seguir usando el mismo dispositivo/navegador de forma simultánea.

### Key Entities

- **Usuario**: Persona que usa el sistema; atributos: DNI (único, 8 dígitos, solo validado por formato), nombre, email (único), contraseña (almacenada de forma encriptada), rol (PASAJERO o ADMIN), número de intentos de login fallidos consecutivos (se resetea al iniciar sesión con éxito o al recuperar la contraseña; la cuenta se bloquea al llegar a 5).
- **Camioneta (vehículo, también referido como "bus")**: Unidad de transporte asignada a los viajes; atributos: identificador, placa, ruta que cubre habitualmente. "Bus" y "Camioneta" se usan como sinónimos en este documento; la entidad canónica es `Camioneta`.
- **Viaje**: Salida programada entre dos puntos del recorrido Ayacucho-VRAEM; atributos: origen, destino, fecha (no anterior a hoy), hora, camioneta asignada, chofer (nombre, campo de texto simple), precio por asiento, archivado (indica si el ADMIN lo sacó de las listas activas tras llenarse). Siempre tiene exactamente 4 Asientos (fijo, no configurable). Se relaciona con múltiples Asientos y puede tener múltiples Reservas.
- **Asiento**: Unidad reservable dentro de un viaje; atributos: número (1 a 4), posición (el asiento 1 va adelante junto al chofer; los asientos 2-4 van juntos atrás; se deriva del número, no es un campo separado), estado (libre/reservado/pagado); pertenece a un único Viaje.
- **Reserva**: Solicitud de un asiento hecha por un Usuario para un Viaje; atributos: usuario comprador, viaje, asiento elegido (con nombre y DNI del pasajero que viajará), monto total (= precio del viaje), estado (pendiente/pagado/expirada/rechazada), fecha de creación. Se relaciona con un Pago. Cada Reserva cubre exactamente un Asiento; un pasajero que quiere varios asientos crea varias Reservas.
- **Pago**: Registro del intento de cobro de una Reserva; atributos: método (Yape, Plin o Efectivo), estado (pendiente/confirmado/rechazado), referencia/número de operación (exactamente 8 dígitos para Yape/Plin; valor fijo para Efectivo, sin número de operación real), motivo de rechazo (texto libre, opcional, solo cuando el ADMIN rechaza el pago); pertenece a una única Reserva.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un pasajero nuevo puede completar todo el flujo (registro, selección de viaje y asientos, pago y visualización del boleto) en menos de 5 minutos.
- **SC-002**: El 100% de los intentos concurrentes de reservar el mismo asiento resultan en una única reserva exitosa y el resto de intentos recibe aviso de que el asiento ya no está disponible.
- **SC-003**: El 100% de las reservas pendientes cuyo plazo de pago vence son liberadas automáticamente, dejando sus asientos disponibles nuevamente para otros pasajeros.
- **SC-004**: Un administrador puede publicar un nuevo viaje disponible para reserva en menos de 2 minutos.
- **SC-005**: Un administrador puede consultar el estado (pendiente/pagado) de cualquier reserva desde el listado en menos de 30 segundos.
- **SC-006**: El 100% de los boletos emitidos tras un pago confirmado muestran un código de reserva único que no se repite entre reservas.

## Assumptions

- **Confirmación de pago**: dado que Yape y Plin no ofrecen a operadores pequeños una integración automática y pública para validar pagos en tiempo real, el pasajero registra el método y una referencia/número de operación al pagar, y la confirmación final de la reserva (paso de "pendiente" a "pagado") la realiza el administrador de forma manual desde el listado de reservas. No hay integración directa con pasarelas de pago en esta versión.
- **Plazo de liberación de asientos**: una reserva "pendiente" sin confirmación de pago se libera automáticamente a los 30 minutos de creada. La verificación se realiza mediante una revisión periódica simple (no en tiempo real), consistente con el principio de simplicidad del proyecto.
- **Registro obligatorio**: solo pasajeros registrados e identificados pueden reservar asientos; no se contempla compra como invitado.
- **Comprador vs. pasajero**: el usuario que compra (inicia sesión) puede reservar asientos para terceros; el nombre y DNI ingresados por asiento pueden ser distintos de los datos de la cuenta que realiza la compra.
- **Mapa de asientos**: se representa visualmente con una distribución fija de 4 asientos (1 adelante junto al chofer, 3 juntos atrás), la misma para todos los viajes. *(Reemplaza la Assumption original de esta spec, que decía "sin modelar la distribución física real del bus"; ver Clarifications, segunda revisión.)*
- **Sin cancelaciones ni reembolsos**: esta versión no contempla que el pasajero cancele una reserva ya pagada ni procesos de reembolso.
- **Una reserva, un asiento**: cada Reserva cubre exactamente un Asiento. Un pasajero que quiere reservar varios asientos (ej. para su familia) repite el flujo de selección y pago una vez por asiento, generando varias Reservas independientes.
- **Asignación de rol ADMIN**: el rol ADMIN se asigna manualmente (por ejemplo, directamente en la base de datos o por un proceso interno); no existe un flujo de autoregistro para administradores.
- **Rutas cubiertas**: las rutas del sistema conectan Ayacucho con San Francisco, Sivia, Santa Rosa, Kimbiri y Pichari (VRAEM), pudiendo un viaje tener como origen o destino cualquiera de estos puntos.
- **Chofer**: se registra como un campo de texto simple del Viaje (nombre del chofer), sin una entidad `Chofer` separada ni gestión propia (alta/edición/eliminación independiente). No se contempla foto ni datos adicionales del chofer en esta versión.
- **Ruta como entidad**: "ruta" sigue siendo el par origen/destino de cada Viaje; no se crea una entidad `Ruta` con gestión propia. Tampoco se agregan campos de capacidad o foto a `Camioneta`.
- **Recuperación de contraseña**: dado que no hay servidor de correo (SMTP) configurado, la recuperación de contraseña genera una contraseña temporal y la muestra una única vez en pantalla, en vez de enviarla por email. Esto es una medida simplificada para esta versión, no un mecanismo de recuperación seguro para producción con usuarios reales fuera de un entorno controlado. Sigue siendo una limitación temporal: cuando se cuente con un proveedor de correo real, este mecanismo MUST reemplazarse por un envío de correo genuino.
- **Bloqueo de cuenta**: 5 intentos de login fallidos consecutivos bloquean la cuenta, incluso ante la contraseña correcta. Un login exitoso o una recuperación de contraseña resetean el contador. No hay un mecanismo de desbloqueo alternativo (p. ej. por soporte) en esta versión.
- **QR de pago**: el código QR generado en la pantalla de pago codifica un texto simple (número de Yape/Plin y datos de la reserva) mediante una librería local de generación de QR; no existe integración real con las APIs de Yape o Plin para iniciar o verificar el pago automáticamente. La confirmación del pago sigue siendo manual por parte del ADMIN (ver Assumption "Confirmación de pago").
- **Archivado de viajes**: es una acción manual y reversible solo por intervención directa en los datos (no hay un botón de "desarchivar" en esta versión). Un viaje archivado conserva todas sus Reservas, Asientos y datos de pasajeros sin cambios; el ADMIN puede consultarlos pero no editarlos ni eliminarlos, evitando que se alteren registros de pasajeros que ya viajaron.
- **Historial de reservas**: no existe un campo o proceso de archivado separado para la Reserva; una reserva se considera "histórica" simplemente porque está `PAGADO` y la fecha de su Viaje ya pasó (se calcula al consultar, no se persiste un estado adicional). Esto mantiene el esquema mínimo (Principio III) y de igual forma nunca se borra ni se pierde el dato.
- **Venta presencial por el ADMIN**: dado que un cliente presencial no necesariamente tiene una cuenta registrada, la Reserva se registra con el propio ADMIN como "usuario comprador"; el nombre y DNI reales del pasajero se guardan en el Asiento igual que en cualquier otra reserva. No existe una pantalla separada para esto: el ADMIN usa la misma pantalla de "confirmar asiento" que los pasajeros (accesible desde "Ver viajes publicados" en su panel), donde ve un selector de método de pago adicional que un pasajero normal no ve. La referencia/número de operación no se solicita en este caso (a diferencia del formulario de pago del pasajero, FR-037), ya que el ADMIN confirma el pago visualmente en el momento.
- **Sesión única**: la identificación real de "mismo dispositivo/máquina" no es confiable en un entorno web sin técnicas adicionales (fingerprinting) que este proyecto no implementa. En su lugar, se usa el control estándar de sesiones concurrentes de Spring Security, que limita a una sesión activa por cuenta (sin importar el dispositivo); iniciar sesión de nuevo con la misma cuenta invalida la sesión anterior. Cuentas distintas sí pueden compartir un mismo dispositivo/navegador de forma simultánea.
