# Multi-stage: Maven build → JRE 25. Build context: backend/
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -q -B -DskipTests package

# glibc (Ubuntu Jammy). webp-imageio ships glibc libwebp and segfaults on Alpine musl.
FROM eclipse-temurin:25-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system --gid 1001 spring \
  && useradd --system --uid 1001 --gid spring --no-create-home spring

COPY --from=build /workspace/target/*.jar /app/app.jar

USER spring:spring
EXPOSE 8080 8081

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} --enable-native-access=ALL-UNNAMED -Duser.timezone=UTC -jar /app/app.jar"]
