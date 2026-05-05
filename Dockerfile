FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
# Asumimos que generas el jar con ./mvnw package
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]