# --- Stage 1: Build Java Application ---
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy maven wrapper and pom file
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Pre-fetch dependencies (cache layer)
RUN ./mvnw dependency:go-offline

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Minimal Runtime Image ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/trade-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=prod
ENV DB_HOST=db
ENV DB_PORT=5432
ENV DB_NAME=tradedb
ENV DB_USER=postgres
ENV DB_PASSWORD=postgres

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
