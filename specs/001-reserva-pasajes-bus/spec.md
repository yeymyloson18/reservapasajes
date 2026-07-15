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

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Comprar un pasaje de principio a fin (Priority: P1)

Un pasajero se registra o inicia sesión, consulta los viajes disponibles hacia su destino en el VRAEM, elige un viaje, selecciona uno o más asientos libres, ingresa el nombre y DNI de cada pasajero que viajará, paga mediante Yape o Plin, y obtiene su boleto con un código de reserva único.

**Why this priority**: Es el flujo de negocio central del sistema; sin esta capacidad completa de extremo a extremo el sistema no genera ningún valor. Corresponde al flujo end-to-end priorizado por el proyecto (login → reservar → pagar → ver boleto).

**Independent Test**: Puede probarse completamente registrando un usuario nuevo, reservando un asiento libre de un viaje existente, pagando con una referencia de Yape/Plin, y verificando que se genera un boleto con código de reserva único y que el asiento pasa de "libre" a "reservado"/"pagado".

**Acceptance Scenarios**:

1. **Given** un pasajero no registrado, **When** se registra con email y contraseña e inicia sesión, **Then** accede a la lista de viajes disponibles.
2. **Given** un pasajero autenticado viendo un viaje con asientos libres, **When** selecciona uno o más asientos libres e ingresa nombre y DNI para cada uno, **Then** el sistema calcula el monto total a pagar (precio del viaje × número de asientos).
3. **Given** una selección de asientos con datos de pasajero completos, **When** el pasajero paga indicando método (Yape o Plin) y referencia de la operación, **Then** el sistema registra la reserva en estado "pendiente" y bloquea los asientos elegidos para que nadie más pueda seleccionarlos.
4. **Given** una reserva cuyo pago fue confirmado, **When** el pasajero consulta su boleto, **Then** ve un código de reserva único junto con ruta, fecha, hora, asientos y datos de los pasajeros.
5. **Given** dos pasajeros que intentan seleccionar el mismo asiento libre casi al mismo tiempo, **When** ambos confirman la selección, **Then** solo uno logra reservarlo y el otro recibe un aviso de que el asiento ya no está disponible.

---

### User Story 2 - Publicar y mantener viajes disponibles (Priority: P2)

Un administrador inicia sesión con rol ADMIN y crea, edita o elimina viajes (ruta, fecha, hora, bus, precio, número de asientos), de modo que existan viajes vigentes para que los pasajeros puedan reservar.

**Why this priority**: Sin viajes publicados por el administrador no hay nada que los pasajeros puedan reservar; es el prerrequisito de contenido para la Historia 1, pero no es tan crítico como el flujo de compra en sí.

**Independent Test**: Puede probarse iniciando sesión como ADMIN, creando un viaje nuevo con sus asientos, editando su precio u horario, y confirmando que los cambios se reflejan en la lista de viajes disponibles para los pasajeros.

**Acceptance Scenarios**:

1. **Given** un usuario con rol ADMIN autenticado, **When** crea un viaje indicando origen, destino, fecha, hora, camioneta (bus) y precio, **Then** el viaje aparece en la lista de viajes disponibles con su mapa de asientos generado según el número de asientos configurado.
2. **Given** un viaje existente sin reservas pagadas, **When** el ADMIN edita su fecha, hora o precio, **Then** los cambios se reflejan inmediatamente para los pasajeros.
3. **Given** un usuario autenticado sin rol ADMIN, **When** intenta crear, editar o eliminar un viaje, **Then** el sistema rechaza la acción.
4. **Given** un viaje con reservas pagadas asociadas, **When** el ADMIN intenta eliminarlo, **Then** el sistema impide la eliminación y explica el motivo.

---

### User Story 3 - Supervisar el estado de las reservas (Priority: P3)

Un administrador consulta el listado de reservas realizadas por los pasajeros y su estado (pendiente/pagado) para hacer seguimiento operativo de la venta de pasajes.

**Why this priority**: Aporta visibilidad de negocio sobre lo ya construido en las Historias 1 y 2, pero el sistema puede operar (venderse pasajes) sin este panel; es una mejora de control, no del flujo transaccional en sí.

**Independent Test**: Puede probarse generando reservas de prueba en distintos estados (pendiente y pagado) e iniciando sesión como ADMIN para verificar que el listado muestra correctamente cada reserva y su estado actual.

**Acceptance Scenarios**:

1. **Given** existen reservas en distintos estados, **When** el ADMIN abre el listado de reservas, **Then** ve cada reserva con su viaje, pasajero(s), monto, estado (pendiente/pagado) y fecha.
2. **Given** una reserva pendiente cuyo plazo de pago venció, **When** el ADMIN consulta el listado, **Then** la reserva aparece con un estado que refleja que fue liberada/expirada y sus asientos vuelven a estar libres para otros pasajeros.

---

### Edge Cases

- ¿Qué ocurre si dos pasajeros seleccionan el mismo asiento casi simultáneamente? El sistema debe garantizar que solo una de las reservas tenga éxito y notificar al resto que el asiento ya no está disponible.
- ¿Qué ocurre si un pasajero no completa el pago dentro del plazo definido? La reserva pasa a un estado de expirada/cancelada y los asientos que tenía bloqueados vuelven a estar libres.
- ¿Qué ocurre si el ADMIN intenta reducir el número de asientos de un viaje por debajo de la cantidad ya reservada o pagada? El sistema rechaza el cambio.
- ¿Qué ocurre si el ADMIN intenta eliminar un viaje con reservas pagadas? El sistema impide la eliminación.
- ¿Qué ocurre si un pasajero intenta pagar asientos que ya no están disponibles (por haber expirado su selección)? El sistema rechaza el pago y solicita reiniciar la selección de asientos.
- ¿Qué ocurre si se ingresa un DNI o email con formato inválido durante el registro? El sistema rechaza el registro e indica el campo inválido.
- ¿Qué ocurre si un pasajero intenta registrarse con un email ya usado por otra cuenta? El sistema rechaza el registro e indica que el email ya está en uso.
- ¿Qué ocurre si un pasajero intenta registrarse con un DNI ya usado por otra cuenta? El sistema rechaza el registro e indica que el DNI ya está en uso.
- ¿Qué ocurre si un pasajero intenta ver el detalle de una reserva que no le pertenece? El sistema deniega el acceso.
- ¿Qué ocurre si el nombre ingresado en el registro contiene números o símbolos, o la contraseña no cumple la complejidad mínima? El sistema rechaza el registro e indica el campo inválido.
- ¿Qué ocurre si un viaje ya no tiene asientos libres? El listado de viajes lo muestra como "COMPLETO" y no permite entrar a seleccionar asientos.
- ¿Qué ocurre si un pasajero pide recuperar su contraseña con un email que no existe? El sistema indica que no existe una cuenta con ese email, sin generar ninguna contraseña.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir que un visitante se registre como pasajero indicando DNI, nombre, email y contraseña, y MUST rechazar registros con email o DNI ya existente en otra cuenta, o con DNI/email de formato inválido. El DNI MUST validarse únicamente por formato (8 dígitos numéricos); el sistema no verifica el DNI contra ningún servicio externo (p. ej. RENIEC). El nombre MUST contener solo letras y espacios, con un mínimo de 2 caracteres. La contraseña MUST tener un mínimo de 8 caracteres, con al menos una letra mayúscula y un número.
- **FR-002**: El sistema MUST almacenar la contraseña de cada usuario de forma encriptada (no en texto plano) y MUST permitir iniciar sesión con email y contraseña.
- **FR-003**: El sistema MUST distinguir entre usuarios con rol PASAJERO y rol ADMIN, y MUST restringir las acciones de gestión de viajes exclusivamente al rol ADMIN.
- **FR-004**: El sistema MUST mostrar a los pasajeros autenticados la lista de viajes disponibles con ruta (origen/destino), chofer, fecha, hora y precio. Un viaje sin asientos libres MUST mostrarse como "COMPLETO", sin permitir continuar a la selección de asientos.
- **FR-005**: El sistema MUST mostrar, para un viaje elegido, un mapa simple de asientos indicando cuáles están libres y cuáles ocupados (reservados o pagados).
- **FR-006**: El sistema MUST permitir a un pasajero seleccionar uno o más asientos libres de un viaje e ingresar el nombre y DNI del pasajero que ocupará cada asiento seleccionado; el DNI de cada pasajero MUST validarse solo por formato (8 dígitos numéricos), sin verificación externa. Antes de enviar la reserva, el sistema MUST pedir una confirmación explícita mostrando la cantidad de asientos elegidos y el monto total.
- **FR-007**: El sistema MUST calcular el monto total a pagar como el precio del viaje multiplicado por el número de asientos seleccionados.
- **FR-008**: El sistema MUST impedir que un mismo asiento sea asignado a más de una reserva activa (pendiente o pagada) al mismo tiempo, incluso ante solicitudes simultáneas.
- **FR-009**: El sistema MUST permitir al pasajero pagar el monto total indicando el método (Yape o Plin) y una referencia/número de operación, y MUST registrar el pago asociado a la reserva.
- **FR-010**: El sistema MUST crear la reserva en estado "pendiente" al momento de la selección de asientos y del envío del pago, y MUST bloquear (marcar como reservados) los asientos elegidos mientras la reserva esté pendiente.
- **FR-011**: El sistema MUST liberar automáticamente los asientos de una reserva "pendiente" cuyo pago no fue confirmado dentro del plazo definido (ver Assumptions), devolviéndolos al estado "libre" y marcando la reserva como expirada.
- **FR-012**: El sistema MUST cambiar el estado de la reserva y de sus asientos a "pagado" una vez confirmado el pago correspondiente.
- **FR-013**: El sistema MUST generar un código de reserva único para cada reserva y MUST permitir al pasajero consultar su boleto (ruta, fecha, hora, asientos, pasajeros, código de reserva) una vez pagada.
- **FR-014**: El sistema MUST permitir que el rol ADMIN cree viajes indicando ruta (origen/destino), fecha, hora, camioneta (bus), chofer y precio, y MUST generar automáticamente los asientos del viaje según el número de asientos configurado.
- **FR-015**: El sistema MUST permitir que el rol ADMIN edite los datos de un viaje, salvo reducir el número de asientos por debajo de la cantidad ya reservada o pagada, lo cual MUST rechazar.
- **FR-016**: El sistema MUST permitir que el rol ADMIN elimine un viaje únicamente cuando no tenga reservas pagadas asociadas.
- **FR-017**: El sistema MUST permitir que el rol ADMIN visualice el listado de todas las reservas junto con su estado (pendiente/pagado/expirada) y datos principales (viaje, pasajero(s), monto, fecha).
- **FR-018**: El sistema MUST restringir el acceso a cualquier funcionalidad de creación, edición o eliminación de viajes a usuarios que no tengan rol ADMIN.
- **FR-019**: El sistema MUST restringir la visualización del detalle de una reserva (incluyendo nombre y DNI de los pasajeros) al usuario que la creó y al rol ADMIN; un pasajero MUST NOT poder ver el detalle de reservas hechas por otros usuarios.
- **FR-020**: El sistema MUST permitir que cualquier usuario recupere el acceso a su cuenta indicando su email; si el email existe, MUST generar una contraseña temporal, guardarla de forma encriptada, y mostrarla una única vez en pantalla (no hay envío de correo real en esta versión).
- **FR-021**: Tras iniciar sesión, el sistema MUST dirigir al usuario con rol ADMIN a su panel de administración y al usuario con rol PASAJERO a la lista de viajes, en vez de una única página de bienvenida para todos los roles.
- **FR-022**: El sistema MUST ofrecer, en toda pantalla autenticada, un control visible y funcional para cerrar sesión.

### Key Entities

- **Usuario**: Persona que usa el sistema; atributos: DNI (único, 8 dígitos, solo validado por formato), nombre, email (único), contraseña (almacenada de forma encriptada), rol (PASAJERO o ADMIN).
- **Camioneta (vehículo, también referido como "bus")**: Unidad de transporte asignada a los viajes; atributos: identificador, placa, ruta que cubre habitualmente. "Bus" y "Camioneta" se usan como sinónimos en este documento; la entidad canónica es `Camioneta`.
- **Viaje**: Salida programada entre dos puntos del recorrido Ayacucho-VRAEM; atributos: origen, destino, fecha, hora, camioneta asignada, chofer (nombre, campo de texto simple), precio por asiento, número total de asientos. Se relaciona con múltiples Asientos y puede tener múltiples Reservas.
- **Asiento**: Unidad reservable dentro de un viaje; atributos: número, estado (libre/reservado/pagado); pertenece a un único Viaje.
- **Reserva**: Solicitud de uno o más asientos hecha por un Usuario para un Viaje; atributos: usuario comprador, viaje, asientos elegidos (cada uno con nombre y DNI del pasajero que viajará), monto total, estado (pendiente/pagado/expirada), fecha de creación. Se relaciona con un Pago.
- **Pago**: Registro del intento de cobro de una Reserva; atributos: método (Yape o Plin), estado, referencia/número de operación; pertenece a una única Reserva.

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
- **Mapa de asientos**: se representa como una lista o cuadrícula simple numerada según la cantidad de asientos del viaje, sin modelar la distribución física real del bus.
- **Sin cancelaciones ni reembolsos**: esta versión no contempla que el pasajero cancele una reserva ya pagada ni procesos de reembolso.
- **Límite de asientos por reserva**: no hay un tope adicional al de asientos libres disponibles en el viaje; un pasajero puede reservar tantos asientos libres como existan.
- **Asignación de rol ADMIN**: el rol ADMIN se asigna manualmente (por ejemplo, directamente en la base de datos o por un proceso interno); no existe un flujo de autoregistro para administradores.
- **Rutas cubiertas**: las rutas del sistema conectan Ayacucho con San Francisco, Sivia, Santa Rosa, Kimbiri y Pichari (VRAEM), pudiendo un viaje tener como origen o destino cualquiera de estos puntos.
- **Chofer**: se registra como un campo de texto simple del Viaje (nombre del chofer), sin una entidad `Chofer` separada ni gestión propia (alta/edición/eliminación independiente). No se contempla foto ni datos adicionales del chofer en esta versión.
- **Ruta como entidad**: "ruta" sigue siendo el par origen/destino de cada Viaje; no se crea una entidad `Ruta` con gestión propia. Tampoco se agregan campos de capacidad o foto a `Camioneta`.
- **Recuperación de contraseña**: dado que no hay servidor de correo (SMTP) configurado, la recuperación de contraseña genera una contraseña temporal y la muestra una única vez en pantalla, en vez de enviarla por email. Esto es una medida simplificada para esta versión, no un mecanismo de recuperación seguro para producción con usuarios reales fuera de un entorno controlado.
