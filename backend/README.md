# Pimienta Alimentos API

Spring Boot REST API for Pimienta Alimentos operations. The service centralizes
identity, employees, attendance, contracts, CRM, tasks, inventory, payroll,
files, notifications, headquarters, and Android POS synchronization.

## Status

Version `2.1.0`. The production API domain is acquired and configured. The
production container is published and deployed through GitHub Actions after
changes are merged into `main`.

## API

The versioned API prefix is:

```text
/api/v1
```

The prefix is defined centrally in `shared/web/ApiPaths.java` and reused by
API controllers and security configuration. Health and Actuator endpoints
remain outside the versioned API prefix.

## Features

- JWT authentication and administrator-approved registration
- Role-based access for administrators and managers
- Employee profiles, attendance, schedules, photos, and imports
- CRM opportunities and projects
- Contracts, tasks, payroll, and notifications
- Headquarters, inventory, stock, and operational transactions
- S3-backed file storage
- POS device enrollment, bootstrap, synchronization, telemetry, and releases
- OpenAPI and Swagger UI

## Technology Stack

- Java 26
- Spring Boot 4
- Spring Security and JWT
- PostgreSQL 16 with Flyway
- Redis
- AWS S3
- Maven and Docker

## Requirements

- Java 26
- Docker and Docker Compose for local dependencies
- Maven, or the included Maven wrapper

## Local Development

Dev stack (Postgres, Redis, LocalStack, `Dockerfile.dev`). Env values are
hardcoded in `docker-compose.yml` so a production `backend/.env` is not used:

```bash
docker compose up --build
```

Production-shaped run on this machine (JAR via `Dockerfile`, reads `.env`,
joins `infra_central_network` / `shared_app_network`):

```bash
cp .env.example .env   # once; fill real secrets
docker compose -f docker-compose.prod.yml up --build
```

For host-based development against published local ports:

```bash
docker compose up -d postgres redis localstack
./mvnw spring-boot:run
```

Do not run Maven against a production `.env`: `DotenvEnvironmentPostProcessor`
loads `.env` from the working directory.

## Test

```bash
./mvnw test
```

Integration tests use the test profile and an in-memory database where
appropriate. Never commit `.env`, credentials, JWT secrets, or private keys.

## Production Delivery

The backend workflow runs tests, publishes the container image to GHCR, and
deploys it to the production host through Cloudflare Access after a successful
push to `main`. Production configuration is supplied by the deployment
environment.

## Documentation

Backend navigation, API notes, POS integration material, and implementation
details are organized under `docs/`.

## License

Apache License 2.0.
