<!--
Sync Impact Report
Version change: [TEMPLATE] → 1.0.0 (initial ratification)
Modified principles: N/A (first concrete version, replacing template placeholders)
Added sections:
  - Core Principles I–VIII (Monolito Simple, Módulos Acotados, Base de Datos Normalizada
    y Mínima, Arquitectura en Capas Simple, UI Simple y Responsive, Testing Pragmático
    por Módulo, Flujo End-to-End Primero, Alcance Estricto)
  - Restricciones de Alcance (Fuera de Alcance)
  - Flujo de Desarrollo y Calidad
  - Governance
Removed sections: none (template placeholders only)
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ compatible (generic placeholders, no changes needed)
  - .specify/templates/spec-template.md ✅ compatible (generic placeholders, no changes needed)
  - .specify/templates/tasks-template.md ✅ compatible (tests are opt-in and this constitution
    explicitly requests them, so /speckit-tasks will include test tasks per module)
  - .specify/templates/checklist-template.md ✅ compatible (no constitution-specific content)
Follow-up TODOs: none
-->

# Pasajes VRAEM Constitution

## Core Principles

### I. Monolito Simple (Spring Boot)
El sistema se construye como un monolito único en Java con Spring Boot. Está PROHIBIDO
dividir el sistema en microservicios, colas de mensajería entre servicios propios, o
despliegues independientes por módulo. Todo el backend corre en un solo proceso/artefacto
desplegable.
**Rationale**: El equipo es pequeño y el alcance (VRAEM) no requiere escalado independiente
por dominio; un monolito reduce la complejidad operativa y acelera la entrega.

### II. Módulos Acotados (Máximo 5)
El sistema se organiza en, como máximo, 5 módulos: `auth`, `viajes`, `reservas`, `pagos`,
`admin`. No se crean módulos adicionales sin una enmienda explícita a esta constitución.
Cada módulo agrupa su propio controller, service y repository; no se comparten
responsabilidades de negocio entre módulos fuera de sus fronteras declaradas.
**Rationale**: Limitar el número de módulos evita la fragmentación prematura y mantiene el
dominio (reserva de pasajes) fácil de razonar de punta a punta.

### III. Base de Datos Normalizada y Mínima (MySQL)
La persistencia usa MySQL con un esquema normalizado. Está PROHIBIDO crear tablas,
columnas o relaciones que no sean requeridas por un flujo funcional vigente
("no tablas innecesarias"). Toda tabla nueva debe justificarse por un caso de uso
existente en la especificación de la funcionalidad correspondiente.
**Rationale**: Un esquema mínimo y normalizado reduce el riesgo de inconsistencias y
facilita las migraciones mientras el proyecto crece.

### IV. Arquitectura en Capas Simple (Controller → Service → Repository)
Cada módulo sigue estrictamente el flujo `controller -> service -> repository`. Está
PROHIBIDO introducir patrones o capas adicionales (CQRS, event sourcing, capas de
abstracción genéricas, interfaces sin más de una implementación, etc.) que no tengan un
uso real e inmediato. Toda abstracción debe tener al menos dos consumidores reales o una
necesidad de test explícita para justificarse.
**Rationale**: La arquitectura en capas básica es suficiente para el alcance del proyecto;
patrones complejos añaden costo de mantenimiento sin beneficio demostrado aquí.

### V. UI Simple y Responsive (Thymeleaf + Bootstrap 5)
La interfaz se construye con Thymeleaf y Bootstrap 5. El diseño MUST ser elegante, limpio
y responsive con enfoque mobile-first. Está PROHIBIDO el exceso de animaciones,
componentes decorativos sin función, o dependencias de frontend adicionales
(frameworks JS pesados, librerías de UI alternativas) que dupliquen lo que Bootstrap 5
ya provee.
**Rationale**: Thymeleaf + Bootstrap 5 mantiene el frontend acoplado al monolito Spring
Boot, sin la complejidad de un stack frontend separado, y cubre las necesidades de un
sistema de reservas de uso simple.

### VI. Testing Pragmático por Módulo
Cada uno de los 5 módulos MUST tener, como mínimo: un test unitario a nivel de service y
un test de integración a nivel de endpoint que cubra el flujo principal del módulo. No es
obligatorio cubrir todos los casos borde; el objetivo es garantizar que el camino feliz
("happy path") de cada módulo esté verificado automáticamente.
**Rationale**: Un mínimo de cobertura por módulo da confianza para refactorizar sin
imponer el costo de una suite exhaustiva que el alcance del proyecto no justifica.

### VII. Flujo End-to-End Primero
Antes de agregar funcionalidades adicionales, el sistema MUST soportar el flujo completo
end-to-end: login → reservar → pagar → ver boleto. Cualquier trabajo que no contribuya
directamente a completar o estabilizar este flujo se pospone hasta que el flujo principal
funcione de extremo a extremo.
**Rationale**: Validar el flujo crítico de negocio primero reduce el riesgo de construir
funcionalidades secundarias sobre una base incompleta.

### VIII. Alcance Estricto (Sin Features Fuera de Especificación)
Está PROHIBIDO agregar funcionalidades que no estén explícitamente definidas en la
especificación de la funcionalidad correspondiente. Ver la sección "Restricciones de
Alcance" para la lista de exclusiones conocidas.
**Rationale**: Mantener el alcance ceñido a lo especificado evita desviar tiempo y
complejidad hacia funcionalidades que no son parte del MVP del sistema de reservas.

## Restricciones de Alcance (Fuera de Alcance)

Explícitamente fuera de alcance para este proyecto, salvo enmienda futura de esta
constitución:

- Notificaciones push.
- Soporte multi-idioma (internacionalización).
- Roles de usuario adicionales a los ya contemplados por el módulo `auth`/`admin`.
- Reportes avanzados o analítica de negocio más allá de lo estrictamente necesario para
  operar el flujo de reservas.

## Flujo de Desarrollo y Calidad

- Toda propuesta de implementación (plan/tasks) MUST priorizar completar el flujo
  end-to-end (Principio VII) antes de extender funcionalidades del mismo módulo o de
  otros módulos.
- Toda revisión de código o plan MUST verificar: (a) que no se excedan los 5 módulos
  definidos, (b) que se respete el flujo controller → service → repository sin capas
  extra, (c) que el esquema de base de datos siga siendo mínimo y normalizado, y (d) que
  cada módulo tocado mantenga al menos un test unitario y un test de integración del
  flujo principal.
- Cualquier funcionalidad propuesta que no esté en la especificación vigente (incluyendo
  las listadas en "Restricciones de Alcance") MUST rechazarse o diferirse a una futura
  enmienda constitucional, no implementarse de forma oportunista.

## Governance

Esta constitución prevalece sobre cualquier otra guía de desarrollo, plantilla o
convención informal del proyecto. En caso de conflicto entre esta constitución y un
plan, spec o tarea generada, la constitución tiene precedencia y el artefacto en
conflicto MUST corregirse.

**Procedimiento de enmienda**: Cualquier cambio a los principios, restricciones de
alcance o flujo de calidad MUST documentarse en este archivo, incluir el razonamiento
("Rationale") del cambio, y actualizar el Sync Impact Report al inicio del archivo.

**Política de versionado** (SemVer aplicado a esta constitución):
- MAJOR: eliminación o redefinición incompatible de un principio existente (p. ej.,
  abandonar el monolito, superar los 5 módulos, o retirar el requisito de testing).
- MINOR: adición de un nuevo principio o sección, o expansión material de una guía
  existente.
- PATCH: aclaraciones de redacción, correcciones tipográficas o refinamientos no
  semánticos.

**Revisión de cumplimiento**: Todo plan (`/speckit-plan`) MUST incluir una verificación
explícita contra los ocho principios en su sección "Constitution Check" antes de la
Fase 0, y cualquier violación MUST justificarse en "Complexity Tracking" o eliminarse
del plan.

**Version**: 1.0.0 | **Ratified**: 2026-07-15 | **Last Amended**: 2026-07-15
