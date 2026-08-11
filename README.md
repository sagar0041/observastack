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

Grafana is at http://localhost:3000.

## Documentation

- [AGENTS.md](./AGENTS.md) — stack, package structure, Javadoc standard
- [CONTRIBUTING.md](./CONTRIBUTING.md) — branch, commit, and PR conventions
- [ROADMAP.md](./ROADMAP.md) — milestones and progress
- [docs/adr/](./docs/adr/) — architecture decision records
