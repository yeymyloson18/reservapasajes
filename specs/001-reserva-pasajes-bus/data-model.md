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
| numeroAsientos | int | obligatorio, > 0; genera automáticamente esa cantidad de `Asiento` al crear el viaje. No editable por debajo de la cantidad de asientos ya en estado `RESERVADO`/`PAGADO` (FR-015). |

**Relaciones**: 1 Viaje → N Asiento. 1 Viaje → N Reserva.

## Asiento

Unidad reservable dentro de un viaje. Guarda también los datos del pasajero mientras está
ocupado por una reserva activa (evita una tabla de unión adicional).

| Campo | Tipo | Reglas |
|---|---|---|
| id | bigint, PK | autogenerado |
| viaje_id | bigint, FK → Viaje | obligatorio |
| numero | int | obligatorio; único junto con `viaje_id` |
| estado | enum(`LIBRE`, `RESERVADO`, `PAGADO`) | obligatorio, por defecto `LIBRE` |
| reserva_id | bigint, FK → Reserva | nulo si `estado = LIBRE`; obligatorio si `RESERVADO`/`PAGADO` |
| nombrePasajero | varchar(150) | nulo si `estado = LIBRE` |
| dniPasajero | varchar(8) | nulo si `estado = LIBRE`; exactamente 8 dígitos numéricos, solo formato |

**Transiciones de estado**:
- `LIBRE → RESERVADO`: al confirmarse la selección de asientos de una nueva Reserva (FR-010), bajo bloqueo pesimista para evitar doble asignación (FR-008).
- `RESERVADO → PAGADO`: cuando el ADMIN confirma el pago de la Reserva (FR-012).
- `RESERVADO → LIBRE`: cuando la Reserva vence sin pago confirmado dentro del plazo (30 min) y se libera automáticamente (FR-011); se limpian `reserva_id`, `nombrePasajero`, `dniPasajero`.

## Reserva

Solicitud de uno o más asientos hecha por un Usuario para un Viaje.

| Campo | Tipo | Reglas |
|---|---|---|
| id | bigint, PK | autogenerado |
| usuario_id | bigint, FK → Usuario | obligatorio (comprador; puede reservar para terceros) |
| viaje_id | bigint, FK → Viaje | obligatorio |
| montoTotal | decimal(10,2) | obligatorio; = precio del viaje × número de asientos elegidos |
| estado | enum(`PENDIENTE`, `PAGADO`, `EXPIRADA`) | obligatorio, por defecto `PENDIENTE` |
| fechaCreacion | datetime | obligatorio, autogenerado |
| codigoReserva | varchar(8) | único, generado al crear la reserva |

**Relaciones**: 1 Reserva → N Asiento (vía `Asiento.reserva_id`). 1 Reserva → 1 Pago.

**Transiciones de estado**:
- `PENDIENTE → PAGADO`: al confirmar el ADMIN el pago asociado (FR-012).
- `PENDIENTE → EXPIRADA`: al vencer el plazo de 30 minutos sin pago confirmado (FR-011).

**Regla de acceso**: solo el `Usuario` propietario (`usuario_id`) o un `ADMIN` pueden ver el
detalle de una Reserva, incluyendo nombre y DNI de los pasajeros (FR-019).

## Pago

Registro del intento de cobro de una Reserva.

| Campo | Tipo | Reglas |
|---|---|---|
| id | bigint, PK | autogenerado |
| reserva_id | bigint, FK → Reserva | obligatorio, único (una Reserva tiene un único Pago) |
| metodo | enum(`YAPE`, `PLIN`) | obligatorio |
| estado | enum(`PENDIENTE`, `CONFIRMADO`) | obligatorio, por defecto `PENDIENTE` |
| referencia | varchar(50) | obligatorio; número/código de operación ingresado por el pasajero |

**Transiciones de estado**:
- `PENDIENTE → CONFIRMADO`: acción manual del ADMIN, que dispara la transición de `Reserva` y sus `Asiento`s a `PAGADO` (FR-012).

## Diagrama de relaciones (resumen)

```text
Usuario (1) ──< Reserva (N)
Camioneta (1) ──< Viaje (N)
Viaje (1) ──< Asiento (N)
Viaje (1) ──< Reserva (N)
Reserva (1) ──< Asiento (N)     [vía Asiento.reserva_id]
Reserva (1) ── Pago (1)
```
