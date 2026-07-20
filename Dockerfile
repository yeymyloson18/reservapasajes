# --- Etapa 1: build ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src ./src
RUN ./mvnw -B clean package -DskipTests

# --- Etapa 2: runtime ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd --no-create-home --shell /usr/sbin/nologin appuser
COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser

ENV SPRING_PROFILES_ACTIVE=railway
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
