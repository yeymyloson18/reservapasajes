# Implementation Plan: Reserva de Pasajes de Bus VRAEM

**Branch**: `001-reserva-pasajes-bus` | **Date**: 2026-07-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-reserva-pasajes-bus/spec.md`

## Summary

Sistema de venta de pasajes de bus para las rutas Ayacucho–VRAEM (San Francisco, Sivia, Santa
Rosa, Kimbiri, Pichari). Un pasajero se registra/inicia sesión, ve los viajes disponibles,
selecciona uno o más asientos libres de un mapa simple, ingresa nombre y DNI por pasajero,
paga por Yape o Plin (con confirmación manual del ADMIN) y obtiene un boleto con código de
reserva único. Un ADMIN publica y mantiene los viajes, y supervisa el estado de las reservas.
Enfoque técnico: monolito Spring Boot (Java) con vistas Thymeleaf/Bootstrap 5, persistencia en
MySQL, organizado en 5 módulos (`auth`, `viajes`, `reservas`, `pagos`, `admin`) con arquitectura
en capas simple (controller → service → repository), según lo fijado por la constitución del
proyecto.

## Technical Context

**Language/Version**: Java 17 (LTS)

**Primary Dependencies**: Spring Boot 3.x (Spring Web MVC, Spring Data JPA, Spring Security, Thymeleaf), Bootstrap 5 (vía WebJars), MySQL Connector/J

**Storage**: MySQL 8

**Testing**: JUnit 5 + Spring Boot Test; Mockito para tests unitarios de service; MockMvc (`@SpringBootTest`) para tests de integración de endpoints

**Target Platform**: Servidor Linux (JAR ejecutable de Spring Boot), accedido vía navegador web (diseño responsive mobile-first)

**Project Type**: Aplicación web monolítica server-rendered (single Spring Boot project, sin frontend separado)

**Performance Goals**: Sin metas de alto rendimiento (operador regional pequeño); operaciones interactivas (login, listar viajes, reservar, pagar) deben responder en menos de 2s bajo carga normal de decenas de usuarios concurrentes

**Constraints**: Debe respetar los principios constitucionales del proyecto: monolito único (sin microservicios), máximo 5 módulos, capas controller → service → repository sin patrones adicionales, esquema de base de datos mínimo y normalizado, liberación de asientos "simple, no en tiempo real complejo"

**Scale/Scope**: Operador de transporte regional pequeño; decenas a un par de cientos de usuarios registrados, un número reducido de viajes diarios por ruta; picos moderados de concurrencia al abrir la venta de un viaje popular (deben manejarse correctamente vía FR-008, no a gran escala)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Verificación |
|---|---|---|
| I. Monolito Simple (Spring Boot) | ✅ PASS | Un único proyecto Spring Boot desplegable; sin microservicios ni colas propias (ver Technical Context, research.md §1). |
| II. Módulos Acotados (máx. 5) | ✅ PASS | Estructura de código organizada exactamente en 5 módulos: `auth`, `viajes`, `reservas`, `pagos`, `admin` (ver Project Structure). |
| III. Base de Datos Normalizada y Mínima (MySQL) | ✅ PASS | 6 entidades, sin tablas de unión innecesarias — `Asiento` referencia directamente a `Reserva` en vez de crear una tabla intermedia (ver data-model.md). |
| IV. Arquitectura en Capas Simple | ✅ PASS | Cada módulo sigue controller → service → repository; sin CQRS, event sourcing ni abstracciones de un solo consumidor (ver research.md §3–§6 para el único mecanismo adicional: bloqueo pesimista de JPA, que es parte del repository, no una capa nueva). |
| V. UI Simple y Responsive (Thymeleaf + Bootstrap 5) | ✅ PASS | Vistas Thymeleaf + Bootstrap 5 vía WebJars, sin frameworks JS adicionales (research.md §2). |
| VI. Testing Pragmático por Módulo | ✅ PASS | Cada módulo tendrá ≥1 test unitario de service y ≥1 test de integración de endpoint cubriendo el camino feliz (research.md §9; se detallará en tasks.md). |
| VII. Flujo End-to-End Primero | ✅ PASS | Historia de Usuario 1 (P1) es exactamente el flujo login → reservar → pagar → ver boleto y se prioriza sobre las Historias 2 y 3 en tasks.md. |
| VIII. Alcance Estricto | ✅ PASS | No se incluye nada fuera de spec.md (sin notificaciones push, sin multi-idioma, sin roles adicionales, sin reportes avanzados, sin pasarela de pago automática). |

**Resultado**: Sin violaciones. No se requiere `Complexity Tracking`.

## Project Structure

### Documentation (this feature)

```text
specs/001-reserva-pasajes-bus/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md         # Phase 1 output (/speckit-plan command)
├── quickstart.md         # Phase 1 output (/speckit-plan command)
├── contracts/
│   └── routes.md         # Phase 1 output (/speckit-plan command)
└── tasks.md              # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

Empaquetado por módulo de negocio (no por capa global), tal como exige el Principio II de la
constitución: cada módulo agrupa su propio `controller`, `service`, `repository` y sus
entidades JPA.

```text
src/main/java/pe/vraem/pasajes/
├── PasajesVraemApplication.java
├── auth/
│   ├── controller/        # RegistroController, LoginController
│   ├── service/            # UsuarioService (registro, autenticación)
│   ├── repository/         # UsuarioRepository
│   └── model/               # Usuario, Rol (enum)
├── viajes/
│   ├── controller/          # ViajeController (listado, detalle), AdminViajeController (CRUD)
│   ├── service/               # ViajeService, CamionetaService, AsientoService
│   ├── repository/            # ViajeRepository, CamionetaRepository, AsientoRepository
│   └── model/                  # Viaje, Camioneta, Asiento, EstadoAsiento (enum)
├── reservas/
│   ├── controller/           # ReservaController, AdminReservaController
│   ├── service/                # ReservaService (selección de asientos, bloqueo, expiración)
│   ├── repository/             # ReservaRepository
│   └── model/                   # Reserva, EstadoReserva (enum)
├── pagos/
│   ├── controller/            # PagoController, AdminPagoController (confirmación)
│   ├── service/                 # PagoService
│   ├── repository/              # PagoRepository
│   └── model/                    # Pago, MetodoPago (enum), EstadoPago (enum)
├── admin/
│   └── controller/            # AdminHomeController (panel de administración)
└── security/                  # Configuración de Spring Security (roles, filtros) — infraestructura compartida, no lógica de negocio de un módulo

src/main/resources/
├── application.properties
├── templates/
│   ├── auth/                  # registro.html, login.html
│   ├── viajes/                # lista.html, detalle.html, admin-form.html
│   ├── reservas/              # seleccion-asientos.html, detalle.html, boleto.html
│   ├── pagos/                  # formulario.html
│   └── admin/                  # panel.html, listado-reservas.html
└── static/                     # ajustes CSS mínimos sobre Bootstrap 5, si se requieren

src/test/java/pe/vraem/pasajes/
├── auth/          # UsuarioServiceTest (unit), AuthControllerIT (integration)
├── viajes/        # ViajeServiceTest (unit), ViajeControllerIT (integration)
├── reservas/      # ReservaServiceTest (unit), ReservaControllerIT (integration)
├── pagos/         # PagoServiceTest (unit), PagoControllerIT (integration)
└── admin/         # AdminControllerIT (integration)
```

**Structure Decision**: Proyecto único Maven/Spring Boot (Opción 1: single project), pero
empaquetado por módulo de negocio en vez de por capa global, para cumplir el Principio II
(máximo 5 módulos con fronteras claras: `auth`, `viajes`, `reservas`, `pagos`, `admin`). El
paquete `security` no es un módulo de negocio adicional sino configuración transversal de
Spring Security (autenticación/autorización), compartida por los 5 módulos.

## Complexity Tracking

*Sin violaciones a la Constitution Check — esta sección se deja vacía intencionalmente.*
