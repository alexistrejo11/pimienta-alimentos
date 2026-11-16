# Management port and Grafana Cloud Alloy

Status: **implemented**. Phases 1–3 match this spec. This file is the record of the approved plan.

Implementation spec. Do not expand past the files named in each phase.

Locked decisions:

- Actuator listens on `8081` only when the `dev` or `prod` profile is active.
- `application.yaml` and `application-test.properties` do not set `management.server.port`.
- Do not set `management.server.address`. The process must listen on all interfaces inside the container so siblings on the Docker network can open `api:8081`.
- Do not publish `8081` on the host. `EXPOSE` / Compose `expose` are metadata only.
- Web exposure is `health,info,prometheus` in base, dev, and prod. No `env`, `beans`, `mappings`, or `metrics`.
- Keep the existing `SecurityConfig` Actuator matchers. Do not add a second filter chain.
- Local observability stays `observability/compose.yaml` + `observability/alloy/config.alloy`. Production EC2 is Alloy-only.

Telemetry ingestion stays on the main API port. `POST /api/v1/telemetry/web/events` and `POST /api/v1/pos/telemetry/events` are MVC controllers, not Actuator. This work must not move or re-secure them.

## Why the security rules stay

`SecurityConfig` already defines a `SecurityFilterChain`. Spring Boot 4’s `ManagementWebSecurityAutoConfiguration` backs off when that bean exists, and the chain is installed on the main servlet context only.

| How the process is started | Actuator port | What `SecurityConfig` does |
|---|---|---|
| Profile `test` (`@SpringBootTest` + MockMvc) | Same mock context as the API. No TCP socket. | Matchers apply. This is why they stay. |
| No profile (`application.yaml` only, IDE / `spring-boot:run` without `SPRING_PROFILES_ACTIVE`) | `server.port` (`API_PORT`, default `8080`) | Matchers apply. Exposure is still only `health,info,prometheus`. |
| Profile `dev` or `prod` | `8081` child context | This chain does **not** run there. Port `8081` is unauthenticated. The exposure list is the control. Requests to `/actuator/**` on the API port are matched by this chain and then 404, because Actuator is no longer mounted there. |

Do not bind the management listener to `127.0.0.1`. Do not publish it. Dev must not be the EC2 profile (`backend/Dockerfile` already sets `SPRING_PROFILES_ACTIVE=prod`).

---

## Phase 1 — Backend config and tests

### 1.1 `backend/src/main/resources/application.yaml`

Change only the exposure list. Do not add `management.server.port` or `management.server.address`.

```yaml
      exposure:
        include: health,info,prometheus
```

Leave `management.endpoint.metrics.enabled: true` and `management.prometheus.metrics.export.enabled: true`. Those flags register Micrometer and the Prometheus endpoint. Removing `metrics` from `exposure.include` only hides `GET /actuator/metrics`.

### 1.2 `backend/src/main/resources/application-dev.yaml`

Replace the whole `management:` block with:

```yaml
management:
  server:
    port: ${MANAGEMENT_PORT:8081}
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
```

Delete the dev-only `env`, `beans`, and `mappings` endpoint flags and drop `metrics` from exposure. Health details stay `always` for local diagnosis. That payload is reachable on the Docker network at port `8081`, not on the published API port.

### 1.3 `backend/src/main/resources/application-prod.yaml`

Delete the comment that says Actuator shares `server.port` and that setting `management.server.port` to the same value starts a second connector. On Boot 4.0.5 an equal port stays `ManagementPortType.SAME` (one connector, no isolation).

Put this comment and `server.port` under the existing `management:` key. Keep the rest of the prod management settings (`show-details: never`, `metrics` endpoint disabled, `prometheus` enabled, Java info disabled).

```yaml
# Profile: prod — Flyway migrations, schema validate, real AWS S3, tight actuator.
# Actuator listens on management.server.port (default 8081), not API_PORT.
# Do not publish 8081. An equal management port does not isolate Actuator.
management:
  server:
    port: ${MANAGEMENT_PORT:8081}
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: never
    metrics:
      enabled: false
    prometheus:
      enabled: true
  info:
    java:
      enabled: false
```

### 1.4 `backend/src/test/resources/application-test.properties`

No edit. It must not gain `management.server.port`. It already exposes only `health` and disables Redis health.

### 1.5 `backend/src/main/java/io/github/alexistrejo11/pimienta/config/security/SecurityConfig.java`

Keep `ACTUATOR_PUBLIC_PATHS` (`/actuator/health`, `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`) and keep `.requestMatchers("/actuator/**").hasRole("ADMIN")`.

Replace the comment on the prometheus entry. State that these rules cover same-port runs (test profile, and any start that does not use `dev`/`prod`). In `dev`/`prod`, Actuator is on the management port, this chain is not installed there, exposure is `health,info,prometheus`, and `8081` must not be published.

Do not add a management `SecurityFilterChain`, `@Order`, or `securityMatcher`.

### 1.6 Dockerfiles

`EXPOSE` does not publish a port. Local Compose still maps API `8000:8000` because `API_PORT=8000`. `8080` is the `application.yaml` default when `API_PORT` is unset. Do not change the Compose port mapping to `8080`.

`backend/Dockerfile` and `backend/Dockerfile.dev`: replace `EXPOSE 8027` with:

```dockerfile
EXPOSE 8080 8081
```

`backend/Dockerfile.dev` currently runs `apk add --no-cache inotify-tools`. The Compose healthcheck calls `curl`, and that image does not install it. Change the package line to:

```dockerfile
RUN apk add --no-cache inotify-tools curl
```

Do not install curl in the production JRE image. That image has no `HEALTHCHECK`.

### 1.7 `backend/docker-compose.yml`

On service `api` only:

- Keep `ports: ["8000:8000"]`. Do not add a host binding for `8081`.
- Add `expose: ["8081"]` next to `ports`.
- Next to `API_PORT`, set `MANAGEMENT_PORT: "8081"`. The healthcheck below assumes that value. If the env var changes, change the healthcheck in the same edit.
- Replace the healthcheck URL:

```yaml
test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:8081/actuator/health >/dev/null"]
```

Leave interval, timeout, `start_period`, and retries as they are. Do not use an unescaped `${MANAGEMENT_PORT}` inside the test string; Compose would interpolate it on the host before the container sees it.

Postgres, Redis, and LocalStack healthchecks stay as they are.

### 1.8 Phase 1 verification

From `backend/`:

```sh
./mvnw -B test
```

Required: the suite passes. Tests use profile `test` and MockMvc, so nothing should listen on `8081`.

Do not start the full Compose stack as a gate for this phase. Optional, only if Compose is already up after a rebuild of `api`:

- `curl -fsS http://127.0.0.1:8000/actuator/health` fails (connection refused is wrong; HTTP 404 is expected once the app is up).
- `curl -fsS http://127.0.0.1:8000/health` still returns success.
- From another container on `pimienta-net`, `http://api:8081/actuator/prometheus` returns metrics text.
- From the host, port `8081` is not published (`docker compose port api 8081` prints nothing).

---

## Phase 2 — Local Prometheus target and Cloud Alloy files

Do not change `observability/compose.yaml` or `observability/alloy/config.alloy` except where a comment would be required for the new target. The local Alloy file stays log-only (Docker `api` and `web` → `http://loki:3100`). Local metrics stay a Prometheus pull.

### 2.1 `observability/prometheus/prometheus.yml`

Prometheus does not expand environment variables in this file. Change the static target and leave the job name, path, and labels:

```yaml
      - targets: ["api:8081"]
```

`job_name` remains `pimienta-backend`. Labels remain `service: backend` and `environment: local`. `metrics_path` remains `/actuator/prometheus`.

### 2.2 `observability/alloy/config.cloud.alloy`

New file. Alloy has no shell `${VAR:-default}` syntax. Use `sys.env` / `coalesce`. Do not add a `traceId` label. Copy the local discovery, relabel, and `loki.process` stages; only the write destination, the default environment, and the scrape block differ.

Required env (fail startup if missing): `GCLOUD_LOKI_URL`, `GCLOUD_LOKI_USERNAME`, `GCLOUD_PROM_URL`, `GCLOUD_PROM_USERNAME`, `GCLOUD_RW_API_KEY`.

Optional env: `DEPLOY_ENV` (default `prod`), `BACKEND_MGMT_TARGET` (default `api:8081`), `ALLOY_SERVICE_KEEP_REGEX` (default `^(api|web)$`).

`GCLOUD_LOKI_URL` is the full push URL, for example `https://logs-prod-000.grafana.net/loki/api/v1/push`. `GCLOUD_PROM_URL` is the remote-write URL, for example `https://prometheus-prod-00.grafana.net/api/prom/push`. Usernames are the Grafana Cloud instance ids. One token (`GCLOUD_RW_API_KEY`) is the basic-auth password for both writes; the token needs logs write and metrics write on that stack.

```alloy
// EC2: stdout from Compose services api and web → Grafana Cloud Logs.
// Metrics: scrape Actuator on the internal management port → Grafana Cloud Metrics.
// Do not label traceId. It stays in the JSON body.

discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}

discovery.relabel "pimienta" {
  targets = discovery.docker.containers.targets

  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    regex         = coalesce(sys.env("ALLOY_SERVICE_KEEP_REGEX"), "^(api|web)$")
    action        = "keep"
  }

  rule {
    source_labels = ["__meta_docker_container_name"]
    regex         = "/(.*)"
    target_label  = "container"
  }

  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    regex         = "^api$"
    replacement   = "backend"
    target_label  = "service"
  }

  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    regex         = "^web$"
    replacement   = "web-ssr"
    target_label  = "service"
  }
}

loki.source.docker "pimienta" {
  host       = "unix:///var/run/docker.sock"
  targets    = discovery.relabel.pimienta.output
  labels     = { job = "pimienta", environment = coalesce(sys.env("DEPLOY_ENV"), "prod") }
  forward_to = [loki.process.application.receiver]
}

// Labels service and environment come from Docker/Compose, not from JSON service
// (audit events reuse that field for the Spring application name).
loki.process "application" {
  stage.json {
    expressions = {
      level   = "level",
      source  = "source",
      channel = "channel",
    }
  }

  stage.labels {
    values = {
      level   = "level",
      source  = "source",
      channel = "channel",
    }
  }

  forward_to = [loki.write.grafana_cloud.receiver]
}

loki.write "grafana_cloud" {
  endpoint {
    url = sys.env("GCLOUD_LOKI_URL")

    basic_auth {
      username = sys.env("GCLOUD_LOKI_USERNAME")
      password = sys.env("GCLOUD_RW_API_KEY")
    }
  }
}

prometheus.remote_write "grafana_cloud" {
  endpoint {
    url = sys.env("GCLOUD_PROM_URL")

    basic_auth {
      username = sys.env("GCLOUD_PROM_USERNAME")
      password = sys.env("GCLOUD_RW_API_KEY")
    }
  }
}

prometheus.scrape "pimienta_backend" {
  targets = [
    {
      __address__ = coalesce(sys.env("BACKEND_MGMT_TARGET"), "api:8081"),
      service     = "backend",
      environment = coalesce(sys.env("DEPLOY_ENV"), "prod"),
    },
  ]

  job_name        = "pimienta-backend"
  metrics_path    = "/actuator/prometheus"
  scheme          = "http"
  scrape_interval = "15s"
  scrape_timeout  = "10s"
  forward_to      = [prometheus.remote_write.grafana_cloud.receiver]
}
```

`job_name` is what becomes the Prometheus `job` label. Do not also set `job` inside `targets`.

EC2 Compose service names must stay `api` and `web`, or `ALLOY_SERVICE_KEEP_REGEX` must be set. A different name drops every log line. The POS container is out of scope until its Compose service name is added later.

### 2.3 `observability/compose.cloud.yaml`

Alloy only. Same image tag as local (`grafana/alloy:v1.10.2`). No Prometheus, Loki, or Grafana services. No `ports:` key at all.

Bind the Alloy HTTP UI to loopback inside the container so it is not reachable from `pimienta-net` either.

Compose interpolates `${...}` from the shell or from `--env-file`, not from the service `env_file`. The run command in Phase 3 passes `--env-file .env.cloud` for that reason. `env_file` still injects the same file into the container so `sys.env` can read it.

```yaml
name: pimienta-observability-cloud

services:
  alloy:
    image: grafana/alloy:v1.10.2
    command:
      - run
      - /etc/alloy/config.alloy
      - --server.http.listen-addr=127.0.0.1:12345
    env_file:
      - .env.cloud
    volumes:
      - ./alloy/config.cloud.alloy:/etc/alloy/config.alloy:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    networks:
      - pimienta-net
    restart: unless-stopped

networks:
  pimienta-net:
    name: ${DOCKER_NETWORK:-pimienta-net}
    external: true
```

`/var/run/docker.sock` is root-equivalent on the host. It is required for `loki.source.docker`. Mount it read-only. Do not publish it.

The Alloy service must join the network where the API’s Compose DNS name is `api`. Locally that network is `pimienta-net`. On EC2 the homelab Compose (not in this repo) may use another name; set `DOCKER_NETWORK` in `.env.cloud`.

### 2.4 `observability/.env.cloud.example`

Committed template. Empty secrets. No real tokens.

```bash
# Copy to observability/.env.cloud. Never commit that file.
# Grafana Cloud token needs both logs write and metrics write.
# Usernames are the stack's Loki and Prometheus instance ids, not the token.

DEPLOY_ENV=prod
DOCKER_NETWORK=pimienta-net
BACKEND_MGMT_TARGET=api:8081
ALLOY_SERVICE_KEEP_REGEX=^(api|web)$

GCLOUD_LOKI_URL=
GCLOUD_LOKI_USERNAME=
GCLOUD_PROM_URL=
GCLOUD_PROM_USERNAME=
GCLOUD_RW_API_KEY=
```

### 2.5 `.gitignore`

Root `.gitignore` ignores `.env` and `*.env`. That does **not** match `.env.cloud`. Add a line:

```
.env.cloud
```

So `observability/.env.cloud` cannot be committed. `.env.cloud.example` must stay tracked.

### 2.6 Phase 2 verification

Static checks only (Cloud credentials are not required):

- `prometheus.yml` target is `api:8081`, job name unchanged.
- `config.cloud.alloy` has no `traceId` label key.
- `compose.cloud.yaml` has one service, `alloy`, and no `ports` key.
- `git check-ignore -v observability/.env.cloud` reports the new ignore rule after the file exists. Until the file exists, `git check-ignore -v --no-index observability/.env.cloud` still works with Git 2.24+; if `--no-index` is unavailable, confirm the pattern is present in `.gitignore`.

Do not point local `config.alloy` at Grafana Cloud. Do not add `prometheus.scrape` to the local Alloy config.

---

## Phase 3 — Docs and final checks

### 3.1 `observability/README.md`

Update the local description: Prometheus scrapes `api:8081/actuator/prometheus`. Alloy still only tails `api` and `web` stdout into local Loki. Grafana, Prometheus, Loki, and Alloy stay bound to `127.0.0.1`.

Add a short “EC2 / Grafana Cloud” section:

1. On the server, the API container is profile `prod`, Actuator is on `8081`, and that port is not in Compose `ports`.
2. The API service name on the Docker network must be `api` (or set `ALLOY_SERVICE_KEEP_REGEX`).
3. Copy the example env file, fill Cloud URLs and credentials, choose `DOCKER_NETWORK` if it is not `pimienta-net`.
4. Start Alloy only:

```sh
cd observability
cp .env.cloud.example .env.cloud
docker compose --env-file .env.cloud -f compose.cloud.yaml up -d
```

5. Do not start `observability/compose.yaml` on EC2. That file runs Prometheus, Loki, and Grafana.
6. Confirm in Grafana Cloud: a log stream `{service="backend"}` (or `web-ssr`) and a metric `up{job="pimienta-backend"}`. `alerts.yml` is local Prometheus only; Cloud alerts are configured in Grafana, not by this repo.

### 3.2 `docs/project/pimienta-alimentos/observability.md`

- The metrics sentence must say Prometheus (local) or Alloy (EC2) scrapes `/actuator/prometheus` on the API’s management port `8081` inside the Docker network.
- Remove the claim that `/actuator/prometheus` is reachable through the trusted reverse proxy. It is not on the public API port in `dev` or `prod`.
- Local topology paragraph: full stack in `observability/compose.yaml`, scrape target `api:8081`.
- Production paragraph: EC2 runs only Alloy (`observability/compose.cloud.yaml`). Alloy tails container stdout to Grafana Cloud Logs and remote-writes the `8081` scrape to Grafana Cloud Metrics. Prometheus, Loki, and Grafana containers are not run on EC2. Secrets stay in `.env.cloud`, not in git.
- Leave the telemetry path section as it is: browsers and POS push to the API on the main port; they are not scrape targets.
- Log label rules stay the same: `service`, `environment`, `level`, `source`, `channel`. `traceId` is not a label.

### 3.3 `backend/README.md`

Add a short note under Local Development (the Compose section is enough; do not rewrite the file):

- Profile `dev` (Compose) and profile `prod` (image) serve Actuator on `8081`.
- Local Compose publishes only `8000:8000`. Port `8081` is `expose` only.
- `GET /health` on the API port is the public health route. `/actuator/health` is on `8081`.

Do not document Grafana Cloud credentials in the backend README.

### 3.4 Final verification

1. `cd backend && ./mvnw -B test` still passes.
2. Diff does not contain `management.server.port` in `application.yaml` or `application-test.properties`.
3. Diff does not contain `management.server.address`.
4. Diff does not publish `8081` (`ports:` containing `8081` is a failure). `expose` and `EXPOSE` are expected.
5. `application-dev.yaml` exposure is exactly `health,info,prometheus` and has no `env`, `beans`, or `mappings` keys.
6. Local `config.alloy` still writes to `http://loki:3100/loki/api/v1/push` and has no `prometheus.scrape` or `remote_write`.
7. Cloud compose file does not define `prometheus`, `loki`, or `grafana` services.

Runtime, when Docker is available:

1. Rebuild and start `backend` Compose, then `observability` Compose.
2. Prometheus target `pimienta-backend` is UP at `api:8081`.
3. Host `localhost:8000/actuator/prometheus` is not a metrics payload.
4. `docker compose -f observability/compose.cloud.yaml config` shows no published ports. Skip a real Cloud push unless credentials are present.

### Out of scope

- The homelab `docker-compose.prod.yml` and Caddyfile (they are not in this repo). On the server, do not add a `ports` entry for `8081`, and do not add a Caddy route to it. If a proxy healthcheck calls `/actuator/health` on the public API port, point it at `GET /health` or probe `api:8081` from inside the Docker network.
- A management-port `SecurityFilterChain`.
- Shipping POS container logs.
- Recreating `observability/prometheus/alerts.yml` as Grafana Cloud alert rules.
- Editing `docs/project/pimienta-alimentos/observability-plan.md` or `docs/project/pimienta-backend/infrastructure.md`.
