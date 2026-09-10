# Local Observability

This stack is for local development only. Production uses a separate observability deployment with private access, persistent storage, retention, TLS, and deployment-managed secrets.

## Requirements

- Docker Desktop or Docker Engine with Compose v2
- The backend local Compose service running as `api`
- The shared Docker network `pimienta-net`

Create the network once if it does not exist:

```sh
docker network create pimienta-net
```

Start the backend first, then start observability:

```sh
cd backend
docker compose up --build -d

cd ../observability
cp .env.example .env
docker compose up -d
```

Local interfaces:

- Grafana: http://localhost:3000 (`admin` / `admin` by default)
- Prometheus: http://localhost:9090
- Loki: http://localhost:3100
- Alloy: http://localhost:12345

Prometheus scrapes `api:8080/actuator/prometheus`. Alloy discovers Docker stdout logs through the read-only Docker socket and forwards them to Loki. Only Grafana, Prometheus, Loki, and Alloy are exposed on localhost; none is reachable from another machine by default.

Stop the stack without removing data:

```sh
docker compose down
```

Reset local observability data:

```sh
docker compose down -v
```
