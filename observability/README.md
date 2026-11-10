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

Prometheus scrapes `api:8081/actuator/prometheus`. That is the API container's management port. It is not published on the host, and it is not on the public API port. Alloy tails stdout only from the Compose `api` and `web` services, stamps `service=backend` or `service=web-ssr`, and forwards JSON logs to Loki. Grafana, Prometheus, Loki, and Alloy are bound to `127.0.0.1` only.

Grafana is provisioned with Backend, Browser, POS Fleet, and Telemetry Ingestion dashboards. After a staff login, a browser error should appear under `{service="backend", channel="web-client"}`. POS events appear under `{service="backend", channel="pos-client"}` after a device sync (or a retrying sync).

Stop the stack without removing data:

```sh
docker compose down
```

Reset local observability data:

```sh
docker compose down -v
```

## EC2 / Grafana Cloud

Production EC2 runs Alloy only. Do not start `compose.yaml` there. That file runs Prometheus, Loki, and Grafana.

1. The API container uses the `prod` profile. Actuator listens on `8081`, and that port is not in Compose `ports`.
2. The API service name on the Docker network must be `api`, and the web service `web`. If either name differs, set `ALLOY_SERVICE_KEEP_REGEX`.
3. Copy the example env file and fill the Grafana Cloud URLs and credentials. Set `DOCKER_NETWORK` when the API network is not `pimienta-net`.
4. Start Alloy only:

```sh
cd observability
cp .env.cloud.example .env.cloud
docker compose --env-file .env.cloud -f compose.cloud.yaml up -d
```

5. In Grafana Cloud, confirm a log stream `{service="backend"}` or `{service="web-ssr"}`, and a metric `up{job="pimienta-backend"}`. `alerts.yml` applies to local Prometheus only. Cloud alerts are configured in Grafana, not by this repo.
