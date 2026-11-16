## Runtime Components

The production API runs in a Docker container with the production Spring profile. PostgreSQL is the primary data store; Redis supports refresh tokens and rate limiting; S3 stores assets. Flyway applies schema migrations and Hibernate validates the production schema.

Actuator exposes health, information, and Prometheus metrics in the production configuration. Secrets and service connection values are supplied as environment variables, not committed to the repository.

## Delivery Flow

```mermaid
flowchart LR
  Commit[Push to main] --> Test[Maven tests]
  Test --> Image[Build Docker image]
  Image --> Ghcr[Publish to GHCR]
  Ghcr --> Ssh[SSH to EC2]
  Ssh --> Server[Compose service api]
  Server --> Api[https://api.pimienta-alimentos.com]
```

Pull requests and changes to `main` under `backend/` run Maven tests. After a successful push to `main`, GitHub Actions publishes both `latest` and commit-tagged images to GHCR. The deploy job SSHs to the EC2 host, pulls the image, recreates only the `api` service, and removes unused images for that repository. Host layout, Caddy, and Compose are in [deploy.md](../pimienta-alimentos/deploy.md).

## Local Environment

Local Docker Compose provides PostgreSQL 16, Redis 7, and LocalStack for S3-compatible storage alongside the API. The development profile is intended for this environment; production uses external service credentials.

```sh
cd backend
docker network create pimienta-net
docker compose up --build
```

Production-shaped local run uses `docker-compose.prod.yml` and `backend/.env`.
