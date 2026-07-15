# Contrato de Rutas Web (MVC server-rendered)

La aplicación es un monolito Spring Boot con vistas Thymeleaf (no expone una API REST
pública separada). El "contrato" son las rutas HTTP que cada módulo expone, con su método,
rol requerido y propósito. Todas las rutas devuelven HTML renderizado por el servidor, salvo
que se indique lo contrario.

## Módulo `auth`

| Método | Ruta | Rol requerido | Descripción |
|---|---|---|---|
| GET | `/registro` | público | Formulario de registro de pasajero |
| POST | `/registro` | público | Crea un Usuario (rol `PASAJERO`); valida email/DNI únicos y formato de DNI (FR-001) |
| GET | `/login` | público | Formulario de inicio de sesión |
| POST | `/login` | público | Autentica (Spring Security form login) (FR-002) |
| POST | `/logout` | autenticado | Cierra la sesión |

## Módulo `viajes`

| Método | Ruta | Rol requerido | Descripción |
|---|---|---|---|
| GET | `/viajes` | autenticado | Lista de viajes disponibles (ruta, fecha, hora, precio) (FR-004) |
| GET | `/viajes/{id}` | autenticado | Detalle del viaje y mapa de asientos libres/ocupados (FR-005) |
| GET | `/admin/viajes/nuevo` | `ADMIN` | Formulario de creación de viaje |
| POST | `/admin/viajes` | `ADMIN` | Crea un viaje y genera sus asientos (FR-014) |
| GET | `/admin/viajes/{id}/editar` | `ADMIN` | Formulario de edición de viaje |
| POST | `/admin/viajes/{id}` | `ADMIN` | Actualiza el viaje; rechaza reducir asientos por debajo de los ya reservados/pagados (FR-015) |
| POST | `/admin/viajes/{id}/eliminar` | `ADMIN` | Elimina el viaje si no tiene reservas pagadas (FR-016) |

## Módulo `reservas`

| Método | Ruta | Rol requerido | Descripción |
|---|---|---|---|
| POST | `/viajes/{id}/reservas` | `PASAJERO` | Crea una Reserva `PENDIENTE` con los asientos elegidos y datos de cada pasajero (nombre, DNI); aplica bloqueo pesimista para evitar doble asignación (FR-006, FR-008, FR-010) |
| GET | `/reservas/{id}` | propietario o `ADMIN` | Detalle de una reserva, incluye boleto con código único si está pagada (FR-013, FR-019) |
| GET | `/admin/reservas` | `ADMIN` | Listado de todas las reservas con su estado (FR-017) |

## Módulo `pagos`

| Método | Ruta | Rol requerido | Descripción |
|---|---|---|---|
| GET | `/reservas/{id}/pago` | propietario | Formulario para registrar método (Yape/Plin) y referencia de pago (FR-009) |
| POST | `/reservas/{id}/pago` | propietario | Registra el Pago en estado `PENDIENTE` asociado a la Reserva |
| POST | `/admin/reservas/{id}/confirmar-pago` | `ADMIN` | Confirma el pago; transiciona Pago/Reserva/Asientos a `PAGADO` (FR-012) |

## Módulo `admin`

| Método | Ruta | Rol requerido | Descripción |
|---|---|---|---|
| GET | `/admin` | `ADMIN` | Panel de administración: accesos a gestión de viajes y reservas |

## Notas de seguridad transversales

- Toda ruta bajo `/admin/**` MUST requerir rol `ADMIN` (FR-003, FR-018), reforzado por
  configuración de Spring Security, no solo por lógica de controller.
- `GET /reservas/{id}` MUST verificar que el usuario autenticado sea el propietario de la
  reserva o tenga rol `ADMIN` (FR-019).
