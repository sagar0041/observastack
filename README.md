# ObservaStack

A small order-processing system (Order, Inventory, Payment services) built
to demonstrate real observability engineering: distributed tracing,
structured logging correlated to traces, RED metrics, SLO-based alerting,
and a documented incident diagnosed end-to-end using the stack itself.

**Why this exists:** most portfolio projects show CRUD. This one shows
what happens *after* the API works — how you'd actually find and fix a
production problem using traces, metrics, and logs together.

## Stack
Java 21 · Spring Boot 3 · OpenTelemetry · Prometheus · Grafana · Loki · Tempo
· Kafka · PostgreSQL · Docker Compose · (stretch: Kubernetes/Helm)

## Status
See [PLAN.md](./PLAN.md) for current progress and what's next.

## Architecture
See [docs/architecture.md](./docs/architecture.md).

## Running locally
```
docker compose up -d
```
(Details filled in as services are built — see PLAN.md P0.)
