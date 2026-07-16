# Data Model: Reserva de Pasajes de Bus VRAEM

Esquema mínimo y normalizado (Principio III de la constitución). No se introducen tablas de
unión adicionales a las 6 entidades descritas en la especificación: cada `Asiento` referencia
directamente a la `Reserva` que lo ocupa (cuando aplica), evitando una tabla intermedia
innecesaria.

## Usuario

Cuenta que permite iniciar sesión, ya sea como pasajero comprador o como administrador.

| Campo | Tipo | Reglas |
|---|---|---|
| id | bigint, PK | autogenerado |
| dni | varchar(8) | único, exactamente 8 dígitos numéricos, solo validado por formato (sin verificación externa) |
| nombre | varchar(150) | obligatorio |
| email | varchar(150) | único, formato de email válido |
| passwordHash | varchar(255) | obligatorio, hash BCrypt (nunca texto plano) |
| rol | enum(`PASAJERO`, `ADMIN`) | obligatorio |
| intentosFallidos | int | obligatorio, por defecto 0; se incrementa en cada login fallido, se resetea a 0 en un login exitoso o al recuperar la contraseña; la cuenta se considera bloqueada cuando llega a 5 (`Usuario.INTENTOS_MAXIMOS`) |

## Camioneta

Vehículo asignado a uno o más viajes.

| Campo | Tipo | Reglas |
|---|---|---|
| id | bigint, PK | autogenerado |
| placa | varchar(10) | único |
| ruta | varchar(150) | descripción de la ruta habitual (p. ej. "Ayacucho - Kimbiri") |

## Viaje

Salida programada entre dos puntos del recorrido Ayacucho-VRAEM.

| Campo | Tipo | Reglas |
|---|---|---|
| id | bigint, PK | autogenerado |
| origen | varchar(100) | obligatorio |
| destino | varchar(100) | obligatorio |
| fecha | date | obligatorio |
| hora | time | obligatorio |
| camioneta_id | bigint, FK → Camioneta | obligatorio |
| precio | decimal(10,2) | obligatorio, > 0 |
| chofer | varchar(150) | obligatorio; nombre del chofer. Campo de texto simple, sin entidad `Chofer` separada (ver Assumptions de spec.md). |
| archivado | boolean | obligatorio, por defecto `false`; el ADMIN lo marca manualmente cuando el viaje ya no tiene asientos libres (FR-030). Un viaje archivado se excluye de la lista de viajes disponibles y de "Gestionar viajes", pero sigue siendo consultable junto con sus Reservas y Asientos. |

No tiene columna `numeroAsientos`: la capacidad es fija en `ViajeService.CAPACIDAD_ASIENTOS = 4`
(ver Assumptions de spec.md, segunda revisión). Al crear un Viaje se generan automáticamente
sus 4 `Asiento` (números 1 a 4); no es editable.

**Relaciones**: 1 Viaje → N Asiento (siempre N=4). 1 Viaje → N Reserva.

## Asiento

Unidad reservable dentro de un viaje. Guarda también los datos del pasajero mientras está
ocupado por una reserva activa (evita una tabla de unión adicional). Cada Viaje tiene
exactamente 4 asientos.

| Campo | Tipo | Reglas |
|---|---|---|
| id | bigint, PK | autogenerado |
| viaje_id | bigint, FK → Viaje | obligatorio |
| numero | int | obligatorio; único junto con `viaje_id`; valores 1 a 4. El numero 1 va "adelante" (junto al chofer); los numeros 2-4 van juntos "atras". La posicion se deriva del numero (metodo `Asiento.esAdelante()`), no es una columna separada. |
| estado | enum(`LIBRE`, `RESERVADO`, `PAGADO`) | obligatorio, por defecto `LIBRE` |
| reserva_id | bigint, FK → Reserva | nulo si `estado = LIBRE`; obligatorio si `RESERVADO`/`PAGADO` |
| nombrePasajero | varchar(150) | nulo si `estado = LIBRE` |
| dniPasajero | varchar(8) | nulo si `estado = LIBRE`; exactamente 8 dígitos numéricos, solo formato |

**Transiciones de estado**:
- `LIBRE → RESERVADO`: al confirmarse la selección de asientos de una nueva Reserva (FR-010), bajo bloqueo pesimista para evitar doble asignación (FR-008).
- `RESERVADO → PAGADO`: cuando el ADMIN confirma el pago de la Reserva (FR-012).
- `RESERVADO → LIBRE`: cuando la Reserva vence sin pago confirmado dentro del plazo (30 min) y se libera automáticamente (FR-011); se limpian `reserva_id`, `nombrePasajero`, `dniPasajero`.

## Reserva

Solicitud de un asiento hecha por un Usuario para un Viaje. Cada Reserva cubre exactamente
un Asiento; un pasajero que quiere varios asientos crea varias Reservas (una por asiento).

| Campo | Tipo | Reglas |
|---|---|---|
| id | bigint, PK | autogenerado |
| usuario_id | bigint, FK → Usuario | obligatorio (comprador; puede reservar para terceros) |
| viaje_id | bigint, FK → Viaje | obligatorio |
| montoTotal | decimal(10,2) | obligatorio; = precio del viaje (una reserva = un asiento) |
| estado | enum(`PENDIENTE`, `PAGADO`, `EXPIRADA`, `RECHAZADA`) | obligatorio, por defecto `PENDIENTE` |
| fechaCreacion | datetime | obligatorio, autogenerado |
| codigoReserva | varchar(8) | único, generado al crear la reserva |

**Relaciones**: 1 Reserva → 1 Asiento (vía `Asiento.reserva_id`). 1 Reserva → 1 Pago.

**Transiciones de estado**:
- `PENDIENTE → PAGADO`: al confirmar el ADMIN el pago asociado (FR-012), o de inmediato en una venta en efectivo (FR-036).
- `PENDIENTE → EXPIRADA`: al vencer el plazo de 30 minutos sin pago confirmado (FR-011).
- `PENDIENTE → RECHAZADA`: al rechazar el ADMIN el pago asociado; libera el Asiento (vuelve a `LIBRE`) (FR-032).

**Historial (derivado, no persistido)**: una Reserva se considera "histórica" (FR-033/FR-034) cuando `estado = PAGADO` y `viaje.fecha` es anterior a la fecha actual. No hay columna `archivada` para Reserva: se calcula en el momento de la consulta sobre las columnas ya existentes (`estado`, y `fecha` del Viaje relacionado).

**Regla de acceso**: solo el `Usuario` propietario (`usuario_id`) o un `ADMIN` pueden ver el
detalle de una Reserva, incluyendo nombre y DNI de los pasajeros (FR-019).

## Pago

Registro del intento de cobro de una Reserva.

| Campo | Tipo | Reglas |
|---|---|---|
| id | bigint, PK | autogenerado |
| reserva_id | bigint, FK → Reserva | obligatorio, único (una Reserva tiene un único Pago) |
| metodo | enum(`YAPE`, `PLIN`, `EFECTIVO`) | obligatorio |
| estado | enum(`PENDIENTE`, `CONFIRMADO`, `RECHAZADO`) | obligatorio, por defecto `PENDIENTE` |
| referencia | varchar(50) | obligatorio; para `YAPE`/`PLIN` debe tener exactamente 8 dígitos numéricos (validado en `PagoForm`, no en la entidad); para `EFECTIVO` se guarda un valor fijo ("EFECTIVO"), ya que no existe número de operación real |
| motivoRechazo | varchar(255) | nulo salvo que `estado = RECHAZADO`; texto libre opcional ingresado por el ADMIN |

**Transiciones de estado**:
- `PENDIENTE → CONFIRMADO`: acción manual del ADMIN, que dispara la transición de `Reserva` y sus `Asiento`s a `PAGADO` (FR-012).
- `PENDIENTE → RECHAZADO`: acción manual del ADMIN, que dispara la transición de `Reserva` a `RECHAZADA` y del `Asiento` asociado a `LIBRE` (FR-032).

## Diagrama de relaciones (resumen)

```text
Usuario (1) ──< Reserva (N)
Camioneta (1) ──< Viaje (N)
Viaje (1) ──< Asiento (4, fijo)
Viaje (1) ──< Reserva (N)
Reserva (1) ── Asiento (1)      [vía Asiento.reserva_id]
Reserva (1) ── Pago (1)
```
