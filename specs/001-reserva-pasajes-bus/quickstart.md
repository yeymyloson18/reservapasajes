# Quickstart: Validación de Reserva de Pasajes de Bus VRAEM

Guía para levantar el sistema localmente y validar de extremo a extremo las historias de
usuario del spec. No incluye código de implementación; ver `data-model.md` y
`contracts/routes.md` para el detalle de entidades y rutas.

## Prerrequisitos

- JDK 17 instalado.
- Maven (o el wrapper `mvnw` del proyecto).
- MySQL 8 corriendo localmente, con una base de datos creada (p. ej. `pasajes_vraem`) y
  credenciales configuradas en `src/main/resources/application.properties`.

## Puesta en marcha

```bash
# Compilar y ejecutar la aplicación
mvn spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`.

Antes de la primera prueba, crear manualmente un Usuario con rol `ADMIN` directamente en la
base de datos (según Assumption "Asignación de rol ADMIN" del spec), con una contraseña ya
hasheada con BCrypt.

## Escenario 1: Compra de un pasaje de extremo a extremo (Historia 1, P1)

1. Como ADMIN, crear un viaje (ver Escenario 2) para tener contenido disponible.
2. Abrir `/registro` y crear una cuenta de pasajero con DNI (8 dígitos), nombre, email y
   contraseña.
3. Iniciar sesión en `/login`.
4. Ir a `/viajes` y verificar que el viaje creado aparece con ruta, fecha, hora y precio
   (FR-004).
5. Abrir `/viajes/{id}` y verificar el mapa de asientos libres/ocupados (FR-005).
6. Seleccionar uno o más asientos libres, ingresar nombre y DNI de cada pasajero, y confirmar
   (FR-006, FR-007). Verificar que el monto total mostrado = precio × número de asientos.
7. Verificar que los asientos elegidos pasan a `RESERVADO` y ya no pueden ser seleccionados
   por otro usuario (FR-008, FR-010).
8. En `/reservas/{id}/pago`, registrar método (Yape o Plin) y una referencia de operación
   (FR-009).
9. Como ADMIN, ir a `/admin/reservas`, ubicar la reserva en estado `PENDIENTE` y confirmar el
   pago (`/admin/reservas/{id}/confirmar-pago`) (FR-012).
10. Como el pasajero, abrir `/reservas/{id}` y verificar que se muestra el boleto con código
    de reserva único, ruta, fecha, hora, asientos y datos de los pasajeros (FR-013).

**Resultado esperado**: el flujo completo (registro → reserva → pago → boleto) se completa
sin errores y en menos de 5 minutos (SC-001).

## Escenario 2: Publicar y mantener viajes (Historia 2, P2)

1. Iniciar sesión como ADMIN.
2. En `/admin/viajes/nuevo`, crear un viaje indicando origen, destino, fecha, hora, camioneta
   y precio, con un número de asientos (p. ej. 15) (FR-014). Verificar que se generan
   automáticamente esa cantidad de asientos en estado `LIBRE`.
3. Editar el viaje (`/admin/viajes/{id}/editar`) cambiando el precio o la hora, y verificar
   que el cambio se refleja en `/viajes/{id}` (FR-015).
4. Intentar reducir el número de asientos por debajo de la cantidad ya reservada/pagada y
   verificar que el sistema lo rechaza (FR-015, edge case).
5. Intentar eliminar un viaje con reservas pagadas y verificar que el sistema lo impide
   (FR-016, edge case).
6. Iniciar sesión como un usuario `PASAJERO` e intentar acceder a `/admin/viajes/nuevo`:
   verificar que el acceso es rechazado (FR-003, FR-018).

## Escenario 3: Supervisar el estado de las reservas (Historia 3, P3)

1. Generar al menos dos reservas de prueba: una pagada (siguiendo el Escenario 1) y una
   pendiente (sin confirmar el pago).
2. Como ADMIN, abrir `/admin/reservas` y verificar que ambas reservas aparecen con su viaje,
   pasajero(s), monto y estado correcto (FR-017).
3. Esperar (o simular) el vencimiento del plazo de 30 minutos de la reserva pendiente sin
   pago, y verificar que pasa a `EXPIRADA` y que sus asientos vuelven a estado `LIBRE`
   (FR-011, SC-003).

## Escenario 4: Concurrencia sobre un mismo asiento (SC-002)

1. Con un viaje que tenga al menos un asiento libre, disparar dos solicitudes de reserva
   casi simultáneas sobre el mismo asiento (dos pestañas/sesiones distintas, o un script de
   prueba con dos hilos).
2. Verificar que solo una de las solicitudes logra reservar el asiento y la otra recibe un
   aviso de que el asiento ya no está disponible (FR-008, SC-002).
