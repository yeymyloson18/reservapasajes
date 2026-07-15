# Research: Reserva de Pasajes de Bus VRAEM

Este documento resuelve las decisiones técnicas necesarias para el plan de implementación.
El stack base ya está fijado por la constitución del proyecto (`.specify/memory/constitution.md`),
por lo que la investigación aquí se centra en decisiones de diseño concretas dentro de ese stack,
no en elegir entre alternativas de framework.

## 1. Lenguaje, framework y persistencia

- **Decision**: Java 17 (LTS) + Spring Boot 3.x, con Spring Web (MVC), Spring Data JPA sobre
  Hibernate, y MySQL 8 como motor de base de datos.
- **Rationale**: Fijado por el Principio I (Monolito Simple con Spring Boot) y el Principio III
  (MySQL) de la constitución. Java 17 es la LTS estable soportada por Spring Boot 3.
- **Alternatives considered**: Ninguna evaluada — el stack es un requisito no negociable de la
  constitución.

## 2. Vista / UI

- **Decision**: Server-side rendering con Thymeleaf, estilos con Bootstrap 5 vía WebJars (sin
  build de frontend separado), mobile-first.
- **Rationale**: Principio V de la constitución. Evita la complejidad de un frontend
  desacoplado (SPA) para un sistema de alcance pequeño.
- **Alternatives considered**: Ninguna — descartado explícitamente por la constitución
  ("sin dependencias de frontend adicionales").

## 3. Autenticación y autorización

- **Decision**: Spring Security con autenticación basada en sesión (form login tradicional),
  contraseñas hasheadas con `BCryptPasswordEncoder`. Roles `PASAJERO` y `ADMIN` mapeados a
  `GrantedAuthority`. Reglas de acceso: rutas de gestión de viajes (`/admin/viajes/**`)
  restringidas a `ADMIN` (FR-003, FR-018); vista de detalle de una reserva restringida al
  usuario propietario o a `ADMIN` (FR-019).
- **Rationale**: Es el mecanismo estándar e integrado de Spring Boot; no añade infraestructura
  adicional (coherente con Principio I).
- **Alternatives considered**: JWT/OAuth2 — rechazado por ser innecesario para una aplicación
  server-rendered de sesión única; añade complejidad no justificada por el alcance.

## 4. Prevención de doble reserva de asiento (FR-008, SC-002)

- **Decision**: Al confirmar la selección de asientos, el service de `reservas` ejecuta, dentro
  de una única transacción, una relectura de los asientos elegidos con bloqueo pesimista
  (`@Lock(LockModeType.PESSIMISTIC_WRITE)` de Spring Data JPA, traducido a `SELECT ... FOR
  UPDATE` en MySQL) y verifica que sigan en estado `LIBRE` antes de marcarlos `RESERVADO`. Si
  alguno ya no está libre, la transacción se aborta y se informa al usuario.
- **Rationale**: Garantiza consistencia ante solicitudes concurrentes sin necesidad de colas,
  bloqueo distribuido ni infraestructura adicional — coherente con el monolito simple y con
  "no requiere ser en tiempo real complejo" (regla de negocio original).
- **Alternatives considered**: Bloqueo optimista con reintentos (más complejo de exponer al
  usuario en UI síncrona); locking distribuido con Redis (rechazado: introduce una dependencia
  externa no justificada por el alcance del proyecto).

## 5. Liberación automática de reservas expiradas (FR-011, SC-003)

- **Decision**: Tarea programada dentro del mismo proceso Spring Boot (`@Scheduled(fixedRate =
  ...)`, ejecutándose por ejemplo cada 1 minuto) que busca reservas en estado `PENDIENTE` con
  `fechaCreacion` de más de 30 minutos, las marca como `EXPIRADA` y libera (`LIBRE`) los
  asientos asociados en la misma transacción.
- **Rationale**: Cumple la regla de negocio ("simple, no requiere tiempo real complejo") sin
  añadir un proceso o cola externa.
- **Alternatives considered**: Verificación perezosa (lazy) al momento de leer el asiento —
  rechazada porque no garantiza liberar asientos que nadie vuelve a consultar, dejando el
  estado inconsistente para reportes del ADMIN (Historia 3).

## 6. Confirmación de pago (Yape / Plin)

- **Decision**: El pasajero registra el método (`YAPE`/`PLIN`) y una referencia/número de
  operación al pagar; el `Pago` y la `Reserva` quedan en estado `PENDIENTE`. El ADMIN confirma
  manualmente el pago desde el listado de reservas, lo que transiciona `Pago` a `CONFIRMADO` y
  `Reserva`/`Asiento`s asociados a `PAGADO`.
- **Rationale**: Documentado como supuesto en `spec.md` (sección Assumptions) — Yape y Plin no
  ofrecen a operadores pequeños una integración pública de verificación en tiempo real.
- **Alternatives considered**: Integración con pasarela de pago automática — rechazada por
  requerir infraestructura/convenios no disponibles para este proyecto y por violar el
  principio de alcance estricto (Principio VIII).

## 7. Código de reserva único (FR-013, SC-006)

- **Decision**: Al confirmar la reserva se genera un código corto alfanumérico (8 caracteres,
  derivado de un UUID en mayúsculas) y se valida su unicidad contra la base de datos
  (columna `codigo_reserva` con restricción `UNIQUE`) antes de persistir.
- **Rationale**: Simple, no requiere un servicio externo de generación de códigos; la
  probabilidad de colisión es despreciable y la unicidad queda garantizada además por la
  restricción de base de datos.
- **Alternatives considered**: Contador secuencial con prefijo — descartado por exponer el
  volumen de ventas del negocio en el propio código.

## 8. Validación de DNI

- **Decision**: Validación únicamente de formato (exactamente 8 dígitos numéricos) mediante
  Bean Validation (`@Pattern(regexp = "\\d{8}")`) tanto para `Usuario.dni` como para el DNI de
  cada pasajero ingresado en `Asiento`. Sin llamadas a servicios externos.
- **Rationale**: Resuelto explícitamente en `spec.md` (sección Clarifications, sesión
  2026-07-15).
- **Alternatives considered**: Verificación contra RENIEC — descartada por el usuario en la
  fase de clarificación.

## 9. Testing

- **Decision**: JUnit 5 + Spring Boot Test. Tests unitarios de `service` con Mockito
  (mockeando repositorios). Tests de integración de `controller`/endpoint con
  `@SpringBootTest` + `MockMvc`, usando una base de datos de pruebas (p. ej. H2 en modo
  compatibilidad MySQL, o un esquema MySQL de test) para cada módulo.
- **Rationale**: Cumple el Principio VI (mínimo un test unitario de service y un test de
  integración de endpoint por módulo, cubriendo el camino feliz).
- **Alternatives considered**: Cobertura exhaustiva de casos borde — explícitamente fuera de
  alcance según el Principio VI.
