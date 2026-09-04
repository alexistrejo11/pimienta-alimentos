# Multi-stage: Maven build → JRE 26. Build context: backend/
FROM maven:3.9-eclipse-temurin-26-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:26-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring -g 1001 \
  && adduser -S spring -u 1001 -G spring

COPY --from=build /workspace/target/*.jar /app/app.jar

RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]
