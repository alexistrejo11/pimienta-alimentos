# Multi-stage: Maven build → JRE 25. Build context: backend/
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring -g 1001 \
  && adduser -S spring -u 1001 -G spring

COPY --from=build /workspace/target/*.jar /app/app.jar

USER spring:spring
EXPOSE 8027

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -Duser.timezone=UTC -jar /app/app.jar"]
