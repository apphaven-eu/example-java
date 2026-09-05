FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Separate layer so dependencies are re-downloaded only when pom.xml changes.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
# curl is not in the base image, the container healthcheck needs it.
RUN apk add --no-cache curl \
    && adduser -S -u 10001 app

WORKDIR /app
COPY --from=build /build/target/app.jar /app/app.jar
USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
