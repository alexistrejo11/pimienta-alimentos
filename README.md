# Pimienta Alimentos

Operations software for Pimienta Alimentos, a food-services company in Mexico.
This repository contains the public website, authenticated operations workspace,
central REST API, and Android point of sale for school cafeterias.

## Projects

| Project | Purpose | Status |
| --- | --- | --- |
| `web/` | Public website, staff workspace, and POS administration | Production-ready |
| `backend/` | Spring Boot API for company operations and POS synchronization | Production-ready |
| `mobile/pos/` | Local-first Android POS application | Release candidate |

The production domains are acquired and configured for the platform. Production
releases are delivered through GitHub Actions after changes are merged into
`main`. The Android application is delivered as a signed APK through S3 rather
than as a Docker service.

## Architecture

```text
Visitors and staff -> Angular web application -> Spring Boot API
Cafeteria operators -> Android POS -> local storage -> API synchronization
Spring Boot API -> PostgreSQL, Redis, and S3
```

## Technology Stack

| Area | Technologies |
| --- | --- |
| Web | Angular 21, TypeScript, SSR, Express, Tailwind CSS |
| API | Java 26, Spring Boot 4, PostgreSQL, Flyway, Redis, JWT |
| Mobile | Kotlin, Jetpack Compose, Room, WorkManager, Retrofit |
| Delivery | GitHub Actions, GHCR, Docker Compose, Cloudflare Access, S3 |

## Local Development

### Web

```bash
cd web
npm ci
npm start
```

### Backend

```bash
cd backend
docker compose up --build
```

Production-shaped API on this host (`backend/.env` + local `Dockerfile`):

```bash
cd backend
docker compose -f docker-compose.prod.yml up --build
```

### Android POS

```bash
cd mobile/pos
./gradlew :app:assembleDebug
./gradlew :app:test
```

## Delivery

- Changes under `web/` publish and deploy the web container from `main`.
- Changes under `backend/` publish and deploy the API container from `main`.
- Changes under `mobile/pos/` run Android tests, build a signed release APK,
  and publish a versioned S3 object plus `latest.apk` alias and `current.json`
  from `main` (bump `versionCode` in Gradle before merge).
- Required production credentials are stored in GitHub Actions and deployment
  environments, never in this repository.

## Documentation

Project navigation and detailed product, architecture, integration, workflow,
and implementation notes are organized under `docs/`. The documentation
structure is intentionally maintained separately from this project overview.

## License

Apache License 2.0.
