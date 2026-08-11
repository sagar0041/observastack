# ObservaStack

An order-processing system built to demonstrate production observability
practice: distributed tracing across synchronous and asynchronous
boundaries, logs correlated to traces, RED metrics, SLO-based alerting,
and a documented incident diagnosed using the stack itself.

The business domain is deliberately small. The observability layer is not.

## Services

| Service           | Responsibility                                    |
|-------------------|---------------------------------------------------|
| order-service     | Order placement and lifecycle                     |
| inventory-service | Stock availability and reservation                |
| payment-service   | Payment authorisation, driven by Kafka events     |

`order-service` calls `inventory-service` synchronously over REST, and
emits an `OrderPlaced` event consumed by `payment-service`. The two
different call styles exist so trace context propagation can be shown
across both.

## Observability

| Signal  | Pipeline                                            |
|---------|-----------------------------------------------------|
| Traces  | OpenTelemetry Java agent → OTLP → Tempo             |
| Metrics | Micrometer → Prometheus                             |
| Logs    | Logback JSON → Loki, correlated by trace_id         |

Grafana dashboards and alert rules are provisioned from `infra/` and
version controlled. Nothing is configured by clicking in the UI.

## Running locally

Requires Docker and JDK 21.

```bash
docker compose up -d          # observability stack + Postgres
mvn verify                    # build and test all modules
```

Kafka isn't needed until M8, so it's excluded from the default stack.
Bring it up explicitly when a service needs it:

```bash
docker compose --profile kafka up -d
```

| Service    | URL / port              | Credentials     |
|------------|--------------------------|------------------|
| Grafana    | http://localhost:3000    | admin / admin    |
| Prometheus | http://localhost:9090    | —                |
| Loki       | http://localhost:3100    | —                |
| Tempo      | http://localhost:3200, OTLP on 4317 (gRPC) / 4318 (HTTP) | — |
| PostgreSQL | localhost:5432            | observastack / observastack |
| Kafka      | localhost:29092 (`--profile kafka`) | — |

Grafana comes up with Prometheus, Loki, and Tempo already wired in as
datasources — nothing to configure by hand.

## Documentation

- [AGENTS.md](./AGENTS.md) — stack, package structure, Javadoc standard
- [CONTRIBUTING.md](./CONTRIBUTING.md) — branch, commit, and PR conventions
- [ROADMAP.md](./ROADMAP.md) — milestones and progress
- [docs/adr/](./docs/adr/) — architecture decision records
