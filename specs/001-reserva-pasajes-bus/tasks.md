---

description: "Task list for Reserva de Pasajes de Bus VRAEM"
---

# Tasks: Reserva de Pasajes de Bus VRAEM

**Input**: Design documents from `specs/001-reserva-pasajes-bus/` (plan.md, spec.md, data-model.md, contracts/routes.md, research.md, quickstart.md)

**Tests**: Incluidos. La constitución del proyecto (Principio VI) exige, como mínimo, un test unitario de service y un test de integración de endpoint por módulo (`auth`, `viajes`, `reservas`, `pagos`, `admin`), cubriendo el camino feliz.

**Organization**: Tareas agrupadas por historia de usuario (US1, US2, US3) según prioridad de `spec.md`, precedidas por Setup y Foundational.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: Historia de usuario a la que pertenece la tarea (US1, US2, US3)
- Cada tarea incluye la ruta de archivo exacta (paquete base `pe.vraem.pasajes`)

## Path Conventions

- Código de producción: `src/main/java/pe/vraem/pasajes/<modulo>/...`
- Vistas: `src/main/resources/templates/<modulo>/...`
- Tests: `src/test/java/pe/vraem/pasajes/<modulo>/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialización del proyecto Spring Boot

- [X] T001 Crear proyecto Maven Spring Boot en la raíz del repositorio (`pom.xml`) con dependencias: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-thymeleaf, spring-boot-starter-test, mysql-connector-j, org.webjars:bootstrap:5.x
- [X] T002 Configurar `src/main/resources/application.properties` (conexión MySQL, dialecto Hibernate, puerto, `spring.jpa.hibernate.ddl-auto`)
- [X] T003 [P] Crear la estructura de paquetes vacía por módulo: `auth`, `viajes`, `reservas`, `pagos`, `admin`, `security` (cada uno con subpaquetes `controller`, `service`, `repository`, `model` donde aplique) bajo `src/main/java/pe/vraem/pasajes/`
- [X] T004 [P] Crear layout base Thymeleaf responsive mobile-first con Bootstrap 5 (navbar, contenedor, footer) en `src/main/resources/templates/layout.html`

**Checkpoint**: Proyecto compila y arranca (`mvn spring-boot:run`) sirviendo una página en blanco.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelo de datos y seguridad base que TODAS las historias de usuario necesitan

**⚠️ CRITICAL**: Ninguna historia de usuario puede implementarse hasta completar esta fase

- [X] T005 [P] Crear enum `Rol` (`PASAJERO`, `ADMIN`) en `src/main/java/pe/vraem/pasajes/auth/model/Rol.java`
- [X] T006 [P] Crear entidad `Usuario` (dni único 8 dígitos, nombre, email único, passwordHash, rol) en `src/main/java/pe/vraem/pasajes/auth/model/Usuario.java`
- [X] T007 [P] Crear `UsuarioRepository` (con `findByEmail`, `existsByDni`) en `src/main/java/pe/vraem/pasajes/auth/repository/UsuarioRepository.java`
- [X] T008 [P] Crear entidad `Camioneta` (placa única, ruta) en `src/main/java/pe/vraem/pasajes/viajes/model/Camioneta.java`
- [X] T009 [P] Crear enum `EstadoAsiento` (`LIBRE`, `RESERVADO`, `PAGADO`) en `src/main/java/pe/vraem/pasajes/viajes/model/EstadoAsiento.java`
- [X] T010 [P] Crear entidad `Viaje` (origen, destino, fecha, hora, camioneta, precio, numeroAsientos) en `src/main/java/pe/vraem/pasajes/viajes/model/Viaje.java`
- [X] T011 [P] Crear entidad `Asiento` (viaje, numero, estado, reserva_id nullable, nombrePasajero nullable, dniPasajero nullable) en `src/main/java/pe/vraem/pasajes/viajes/model/Asiento.java`
- [X] T012 [P] Crear `CamionetaRepository`, `ViajeRepository` y `AsientoRepository` (con método de bloqueo pesimista `@Lock(PESSIMISTIC_WRITE)` para releer asientos por id) en `src/main/java/pe/vraem/pasajes/viajes/repository/`
- [X] T013 [P] Crear enum `EstadoReserva` (`PENDIENTE`, `PAGADO`, `EXPIRADA`) en `src/main/java/pe/vraem/pasajes/reservas/model/EstadoReserva.java`
- [X] T014 [P] Crear entidad `Reserva` (usuario, viaje, montoTotal, estado, fechaCreacion, codigoReserva único) en `src/main/java/pe/vraem/pasajes/reservas/model/Reserva.java`
- [X] T015 [P] Crear `ReservaRepository` (con `findByEstadoAndFechaCreacionBefore` para expiración) en `src/main/java/pe/vraem/pasajes/reservas/repository/ReservaRepository.java`
- [X] T016 [P] Crear enums `MetodoPago` (`YAPE`, `PLIN`) y `EstadoPago` (`PENDIENTE`, `CONFIRMADO`) en `src/main/java/pe/vraem/pasajes/pagos/model/`
- [X] T017 [P] Crear entidad `Pago` (reserva único, metodo, estado, referencia) en `src/main/java/pe/vraem/pasajes/pagos/model/Pago.java`
- [X] T018 [P] Crear `PagoRepository` en `src/main/java/pe/vraem/pasajes/pagos/repository/PagoRepository.java`
- [X] T019 Configurar Spring Security (`UserDetailsService` sobre `UsuarioRepository`, bean `BCryptPasswordEncoder`, reglas de acceso: `/admin/**` solo `ADMIN`, form login/logout) en `src/main/java/pe/vraem/pasajes/security/SecurityConfig.java`
- [X] T020 Habilitar tareas programadas (`@EnableScheduling`) en `src/main/java/pe/vraem/pasajes/PasajesVraemApplication.java`

**Checkpoint**: Esquema de base de datos generado, seguridad base configurada. Las historias de usuario pueden comenzar.

---

## Phase 3: User Story 1 - Comprar un pasaje de principio a fin (Priority: P1) 🎯 MVP

**Goal**: Un pasajero se registra/inicia sesión, ve viajes disponibles, selecciona asientos e ingresa datos de pasajero, paga (Yape/Plin), un ADMIN confirma el pago, y el pasajero ve su boleto con código único.

**Independent Test**: Registrar un usuario nuevo, reservar un asiento libre de un viaje existente (creado directamente vía repository/seed en el test), pagar con una referencia, confirmar el pago como ADMIN, y verificar que se genera el boleto con código de reserva único y que el asiento queda `PAGADO`.

### Tests for User Story 1

- [X] T021 [P] [US1] Unit test `UsuarioService` (registro valida DNI/email únicos y formato, hash BCrypt de contraseña, login) en `src/test/java/pe/vraem/pasajes/auth/UsuarioServiceTest.java`
- [X] T022 [P] [US1] Integration test flujo registro + login (`AuthController`) en `src/test/java/pe/vraem/pasajes/auth/AuthControllerIT.java`
- [X] T023 [P] [US1] Unit test `ReservaService` (cálculo de monto total, bloqueo pesimista de asientos, rechazo si algún asiento ya no está `LIBRE`, generación de código único, y `liberarReservasExpiradas()`: una reserva `PENDIENTE` con `fechaCreacion` de más de 30 min pasa a `EXPIRADA` y sus asientos vuelven a `LIBRE`) en `src/test/java/pe/vraem/pasajes/reservas/ReservaServiceTest.java`
- [X] T024 [P] [US1] Integration test `POST /viajes/{id}/reservas` y `GET /reservas/{id}` (creación de reserva happy path, caso de dos solicitudes concurrentes sobre el mismo asiento, y verificación de que un usuario autenticado distinto del propietario y sin rol ADMIN recibe acceso denegado al `GET /reservas/{id}`, FR-019) en `src/test/java/pe/vraem/pasajes/reservas/ReservaControllerIT.java`
- [X] T025 [P] [US1] Unit test `PagoService` (registrar pago pendiente; confirmar pago transiciona Reserva y Asientos a `PAGADO`) en `src/test/java/pe/vraem/pasajes/pagos/PagoServiceTest.java`
- [X] T026 [P] [US1] Integration test flujo de pago (`POST /reservas/{id}/pago` y `POST /admin/reservas/{id}/confirmar-pago`), verificando además que tras la confirmación `GET /reservas/{id}` muestra el boleto completo (código de reserva único, ruta, fecha, hora, asientos y datos de los pasajeros, FR-013) en `src/test/java/pe/vraem/pasajes/pagos/PagoControllerIT.java`

### Implementation for User Story 1

- [X] T027 [US1] Implementar `UsuarioService` (registro con validación de unicidad DNI/email, hash BCrypt, consulta para login) en `src/main/java/pe/vraem/pasajes/auth/service/UsuarioService.java`
- [X] T028 [US1] Implementar `RegistroController` (`GET`/`POST /registro`) en `src/main/java/pe/vraem/pasajes/auth/controller/RegistroController.java` + vista `src/main/resources/templates/auth/registro.html`
- [X] T029 [US1] Implementar `LoginController` (`GET /login`) en `src/main/java/pe/vraem/pasajes/auth/controller/LoginController.java` + vista `src/main/resources/templates/auth/login.html` (POST /login y /logout gestionados por Spring Security)
- [X] T030 [P] [US1] Implementar `ViajeService.listarDisponibles()` y `obtenerDetalle(id)` en `src/main/java/pe/vraem/pasajes/viajes/service/ViajeService.java`
- [X] T031 [US1] Implementar `ViajeController` (`GET /viajes`, `GET /viajes/{id}`) en `src/main/java/pe/vraem/pasajes/viajes/controller/ViajeController.java` + vistas `src/main/resources/templates/viajes/lista.html` y `src/main/resources/templates/viajes/detalle.html` (mapa de asientos libre/ocupado; el formulario de selección de asientos vive en esta misma vista en lugar de una plantilla separada, ver Notes)
- [X] T032 [US1] Implementar `ReservaService.crearReserva(...)` (bloqueo pesimista de asientos, validación de datos de pasajero por asiento, cálculo de monto, generación de código de reserva único) en `src/main/java/pe/vraem/pasajes/reservas/service/ReservaService.java`
- [X] T033 [US1] Implementar `ReservaService.liberarReservasExpiradas()` con `@Scheduled(fixedRate = ...)` (cada 1 minuto, libera reservas `PENDIENTE` con más de 30 min) en `src/main/java/pe/vraem/pasajes/reservas/service/ReservaService.java`
- [X] T034 [US1] Implementar `ReservaController` (`POST /viajes/{id}/reservas`, `GET /reservas/{id}` restringido a propietario/ADMIN) en `src/main/java/pe/vraem/pasajes/reservas/controller/ReservaController.java` + vista `src/main/resources/templates/reservas/detalle.html` (sirve también como boleto cuando la reserva está `PAGADO`)
- [X] T035 [US1] Implementar `PagoService.registrarPago(...)` y `confirmarPago(...)` en `src/main/java/pe/vraem/pasajes/pagos/service/PagoService.java`
- [X] T036 [US1] Implementar `PagoController` (`GET`/`POST /reservas/{id}/pago`, `POST /admin/reservas/{id}/confirmar-pago` restringido a ADMIN) en `src/main/java/pe/vraem/pasajes/pagos/controller/PagoController.java` + vista `src/main/resources/templates/pagos/formulario.html`

**Checkpoint**: El flujo end-to-end login → reservar → pagar → ver boleto funciona de forma independiente y verificable (SC-001, SC-002, SC-003, SC-006).

---

## Phase 4: User Story 2 - Publicar y mantener viajes disponibles (Priority: P2)

**Goal**: Un ADMIN crea, edita y elimina viajes, con generación automática de asientos y validaciones de negocio.

**Independent Test**: Iniciar sesión como ADMIN, crear un viaje con N asientos, editar su precio/hora, intentar reducir asientos por debajo de los ya reservados (debe rechazarse), e intentar eliminar un viaje con reservas pagadas (debe rechazarse).

### Tests for User Story 2

- [X] T037 [P] [US2] Unit test `ViajeService` (crear viaje genera N asientos `LIBRE`; editar rechaza reducir asientos por debajo de los `RESERVADO`/`PAGADO`; eliminar rechaza si existen reservas `PAGADO`) en `src/test/java/pe/vraem/pasajes/viajes/ViajeServiceTest.java`
- [X] T038 [P] [US2] Integration test `AdminViajeController` (crear, editar y eliminar viaje happy path, y acceso denegado a no-ADMIN) en `src/test/java/pe/vraem/pasajes/viajes/AdminViajeControllerIT.java`

### Implementation for User Story 2

- [X] T039 [US2] Implementar `ViajeService.crearViaje(...)` (genera automáticamente los `Asiento` según `numeroAsientos`), `editarViaje(...)` (rechaza reducir asientos por debajo de los ocupados, FR-015) y `eliminarViaje(...)` (rechaza si hay reservas pagadas, FR-016) en `src/main/java/pe/vraem/pasajes/viajes/service/ViajeService.java`
- [X] T040 [US2] Implementar selección/alta de `Camioneta` dentro del formulario de viaje (dropdown de camionetas existentes + alta rápida por placa/ruta) en `ViajeService.obtenerOCrearCamioneta(...)` en `src/main/java/pe/vraem/pasajes/viajes/service/ViajeService.java`
- [X] T041 [US2] Implementar `AdminViajeController` (`GET /admin/viajes/nuevo`, `POST /admin/viajes`, `GET /admin/viajes/{id}/editar`, `POST /admin/viajes/{id}`, `POST /admin/viajes/{id}/eliminar`, restringido a ADMIN) en `src/main/java/pe/vraem/pasajes/viajes/controller/AdminViajeController.java` + vista `src/main/resources/templates/viajes/admin-form.html`

**Checkpoint**: El ADMIN puede publicar y mantener viajes de forma independiente; US1 sigue funcionando (SC-004).

---

## Phase 5: User Story 3 - Supervisar el estado de las reservas (Priority: P3)

**Goal**: Un ADMIN consulta el listado de todas las reservas y su estado, y accede a un panel de administración central.

**Independent Test**: Generar una reserva pagada y una pendiente (dejando que esta última expire), iniciar sesión como ADMIN, y verificar que el listado muestra ambas con su viaje, pasajero(s), monto y estado correctos (incluida la expirada).

### Tests for User Story 3

- [X] T042 [P] [US3] Integration test `GET /admin/reservas` (lista reservas en estados pendiente/pagado/expirada; acceso denegado a no-ADMIN) en `src/test/java/pe/vraem/pasajes/reservas/AdminReservaControllerIT.java`
- [X] T043 [P] [US3] Unit test `AdminService.obtenerResumen()` (conteo de viajes activos y reservas pendientes) en `src/test/java/pe/vraem/pasajes/admin/AdminServiceTest.java`
- [X] T044 [P] [US3] Integration test acceso a `/admin` (ADMIN permitido, PASAJERO denegado) en `src/test/java/pe/vraem/pasajes/admin/AdminControllerIT.java`

### Implementation for User Story 3

- [X] T045 [US3] Implementar `AdminReservaController` (`GET /admin/reservas`, restringido a ADMIN) en `src/main/java/pe/vraem/pasajes/reservas/controller/AdminReservaController.java` + vista `src/main/resources/templates/admin/listado-reservas.html`
- [X] T046 [US3] Implementar `AdminService.obtenerResumen()` (conteo de viajes activos y reservas pendientes) en `src/main/java/pe/vraem/pasajes/admin/service/AdminService.java`
- [X] T047 [US3] Implementar `AdminHomeController` (`GET /admin`, restringido a ADMIN) en `src/main/java/pe/vraem/pasajes/admin/controller/AdminHomeController.java` + vista `src/main/resources/templates/admin/panel.html`

**Checkpoint**: Todas las historias de usuario (US1, US2, US3) funcionan de forma independiente (SC-005).

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Mejoras que afectan a múltiples historias

- [X] T048 [P] Ajustar navegación compartida (enlaces a `/admin` visibles solo para ADMIN, enlace a "Mis reservas" para PASAJERO) en `src/main/resources/templates/layout.html`. Requirió además una nueva ruta `GET /reservas` ("mis reservas") en `ReservaController`/`ReservaService.listarPorUsuario(...)` + vista `reservas/mis-reservas.html` para que el enlace tenga destino.
- [X] T049 [P] Páginas de error simples (403/404/500) responsive con Bootstrap 5 en `src/main/resources/templates/error/`
- [X] T050 Ejecutar manualmente los 4 escenarios de `specs/001-reserva-pasajes-bus/quickstart.md` contra MySQL real y corregir los hallazgos. Bugs encontrados y corregidos: (1) fragmento Thymeleaf `head(pageTitle)` mal declarado sobre `<th:block>` en vez de `<head>`, rompía el parseo de toda vista que lo usara; (2) `ViajeNoEncontradoException`/`ReservaNoEncontradaException` no mapeaban a 404 (devolvían 500), corregido con `@ResponseStatus(HttpStatus.NOT_FOUND)`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede iniciar de inmediato
- **Foundational (Phase 2)**: Depende de Setup — BLOQUEA todas las historias de usuario
- **User Story 1 (Phase 3)**: Depende de Foundational. Es el MVP; sin dependencias de otras historias
- **User Story 2 (Phase 4)**: Depende de Foundational. Reutiliza `ViajeService`/`ViajeController` creados parcialmente en US1 (lectura); añade escritura. Puede desarrollarse en paralelo a US1 por un desarrollador distinto si se coordina el archivo `ViajeService.java`
- **User Story 3 (Phase 5)**: Depende de Foundational y de que existan Reservas (US1) para tener datos que listar; en términos de código no depende de US2
- **Polish (Phase 6)**: Depende de que las historias deseadas estén completas

### Dentro de cada historia

- Tests (si se incluyen) antes de la implementación correspondiente
- Modelos/entidades (Foundational) antes que services
- Services antes que controllers
- `auth`, `viajes` (lectura), `reservas`, `pagos` se completan en US1 antes del checkpoint de esa historia

### Parallel Opportunities

- Todas las tareas [P] de Setup pueden correr en paralelo
- Todas las tareas [P] de Foundational (T005–T018) pueden correr en paralelo (archivos distintos); T019 y T020 son secuenciales tras ellas
- Dentro de US1, los tests T021–T026 pueden correr en paralelo entre sí; luego T027–T036 tienen dependencias internas (service antes que controller del mismo módulo) pero los módulos `auth`, `viajes`, `reservas`, `pagos` pueden avanzar en paralelo entre equipos distintos
- US2 y US3 pueden desarrollarse en paralelo entre sí una vez completado US1 (ambas dependen de Foundational, no una de la otra)

---

## Parallel Example: User Story 1

```bash
# Lanzar en paralelo los tests de User Story 1:
Task: "Unit test UsuarioService en src/test/java/pe/vraem/pasajes/auth/UsuarioServiceTest.java"
Task: "Integration test AuthController en src/test/java/pe/vraem/pasajes/auth/AuthControllerIT.java"
Task: "Unit test ReservaService en src/test/java/pe/vraem/pasajes/reservas/ReservaServiceTest.java"
Task: "Integration test ReservaController en src/test/java/pe/vraem/pasajes/reservas/ReservaControllerIT.java"
Task: "Unit test PagoService en src/test/java/pe/vraem/pasajes/pagos/PagoServiceTest.java"
Task: "Integration test PagoController en src/test/java/pe/vraem/pasajes/pagos/PagoControllerIT.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 solamente)

1. Completar Phase 1: Setup
2. Completar Phase 2: Foundational (CRÍTICO — bloquea todas las historias)
3. Completar Phase 3: User Story 1
4. **DETENER y VALIDAR**: probar User Story 1 de forma independiente (Escenario 1 y 4 de `quickstart.md`)
5. Desplegar/demostrar si está listo — ya cumple el Principio VII de la constitución (flujo end-to-end primero)

### Incremental Delivery

1. Setup + Foundational → base lista
2. Añadir US1 → probar de forma independiente → MVP funcional
3. Añadir US2 → probar de forma independiente → ADMIN puede publicar viajes reales
4. Añadir US3 → probar de forma independiente → ADMIN tiene visibilidad operativa completa
5. Polish → navegación, errores, validación final con quickstart.md

---

## Notes

- [P] = archivos distintos, sin dependencias entre sí
- [Story] mapea cada tarea a su historia de usuario para trazabilidad
- Cada historia de usuario debe quedar completable y probable de forma independiente
- Verificar que los tests fallen antes de implementar (T021–T026, T037–T038, T042–T044)
- Hacer commit tras cada tarea o grupo lógico de tareas
- Detenerse en cada checkpoint para validar la historia de forma independiente
- Respetar el límite de 5 módulos (auth, viajes, reservas, pagos, admin) y la arquitectura controller → service → repository (Principios II y IV de la constitución); `security` es configuración transversal, no un módulo de negocio adicional
