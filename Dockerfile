# ---- Stage 1: build ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy only pom.xml first so dependency layer is cached across code changes
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

# Now copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# ---- Stage 2: runtime ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root user — don't run as root in a container
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=build /app/target/order-monolith-*.jar app.jar

USER appuser
EXPOSE 8080

# exec form — ensures SIGTERM reaches the JVM directly, not a shell wrapper
ENTRYPOINT ["java", "-jar", "app.jar"]