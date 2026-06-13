# syntax=docker/dockerfile:1

###############################################################################
# Stage 1 — build the Spring Boot fat jar with JDK 17.
# The Maven image already ships JDK 17, so this sidesteps the host JDK-21 +
# Lombok annotation-processing trap entirely (see CLAUDE.md "Commands").
###############################################################################
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Resolve dependencies first so this layer stays cached until pom.xml changes.
# Best-effort: the package step below fetches anything go-offline misses
# (Spring Boot plugins, etc.).
COPY pom.xml .
RUN mvn -B dependency:go-offline || true

# Now the sources. CI builds with -DskipTests; mirror that here.
COPY src ./src
RUN mvn -B clean package -DskipTests

###############################################################################
# Stage 2 — slim runtime: just the JRE + the built jar.
###############################################################################
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
# App also forces this TZ at the JVM level; set it here too for clean log stamps.
ENV TZ=Africa/Nairobi

# Single fat jar; the glob keeps this independent of the 0.0.1-SNAPSHOT version.
COPY --from=build /build/target/*.jar /app/app.jar

EXPOSE 8443
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
