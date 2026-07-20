# Despliegue en Railway (gratis)

Este proyecto es un monolito Spring Boot + Thymeleaf: un solo servicio (`Dockerfile`)
sirve backend y frontend. Necesita además un servicio de **MySQL**.

Archivos ya preparados en el repo:

- `Dockerfile` — build multi-stage (Maven + JDK 21 → JRE 21).
- `.dockerignore` — evita copiar `target/`, `.git`, credenciales locales, etc.
- `railway.json` — le dice a Railway que use el Dockerfile y el healthcheck.
- `src/main/resources/application-railway.properties` — perfil de producción
  que lee las variables de MySQL de Railway y el puerto dinámico (`PORT`).
- `spring-boot-starter-actuator` (en `pom.xml`) — expone `/actuator/health`
  solamente (sin detalles) para que Railway pueda comprobar que el servicio
  está vivo.

## Pasos

1. **Sube el repo a GitHub** si aún no lo está (Railway despliega desde GitHub).

2. **Crea el proyecto en Railway**
   - New Project → Deploy from GitHub repo → selecciona `pasajes-vraem`.
   - Railway detecta el `Dockerfile` automáticamente.

3. **Agrega la base de datos**
   - Dentro del mismo proyecto: `New` → `Database` → `Add MySQL`.

4. **Configura las variables del servicio web** (pestaña *Variables* del
   servicio del Dockerfile, no del MySQL). Usa la sintaxis de referencia de
   Railway (`${{NombreDelServicioMySQL.VARIABLE}}`) para no copiar valores a
   mano:

   | Variable            | Valor                                  |
   |---------------------|-----------------------------------------|
   | `SPRING_PROFILES_ACTIVE` | `railway`                          |
   | `MYSQLHOST`         | `${{MySQL.MYSQLHOST}}`                  |
   | `MYSQLPORT`         | `${{MySQL.MYSQLPORT}}`                  |
   | `MYSQLDATABASE`     | `${{MySQL.MYSQLDATABASE}}`              |
   | `MYSQLUSER`         | `${{MySQL.MYSQLUSER}}`                  |
   | `MYSQLPASSWORD`     | `${{MySQL.MYSQLPASSWORD}}`              |

   (Ajusta `MySQL` al nombre real que Railway le puso al servicio de base de
   datos si es distinto.)

5. **Genera el dominio público**
   - Settings del servicio web → Networking → `Generate Domain`.

6. **Despliega** (Railway hace build y deploy automáticamente al hacer push,
   o dale a `Deploy` manualmente la primera vez).

## Notas importantes

- **Reservas con expiración automática (FR-011):** `ReservaService` usa
  `@Scheduled` para liberar reservas pendientes tras 30 minutos. Esto solo
  funciona mientras el servicio esté corriendo (no "dormido"). Railway no
  duerme los servicios mientras tengas crédito disponible en el plan
  gratuito/hobby — evita plataformas con "sleep on idle" para este proyecto.
- **`spring.jpa.hibernate.ddl-auto=update`** crea/actualiza las tablas solas
  en el primer arranque contra la base de datos vacía de Railway. No migra
  datos que tengas en tu MySQL local — si necesitas los datos actuales,
  expórtalos con `mysqldump` e impórtalos en la base de Railway.
- El costo se descuenta de un mismo crédito compartido por todos los
  servicios del proyecto (app + MySQL). Revisa el uso en `Usage` del
  dashboard de Railway para no quedarte sin crédito a mitad de mes.
- `application-local.properties` (con tu contraseña de MySQL local) ya está
  en `.gitignore` — no se sube al repo ni a Railway; ese archivo es solo
  para desarrollo en tu máquina.
