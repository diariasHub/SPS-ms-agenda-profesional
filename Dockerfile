# =========================================================
# ETAPA 1 - BUILD
# =========================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# =========================================================
# ETAPA 2 - RUNTIME
# =========================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/agenda-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]