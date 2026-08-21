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
| PostgreSQL | localhost:5432, databases `observastack` + `inventory` | observastack / observastack |
| Kafka      | localhost:29092 (`--profile kafka`) | — |

Grafana comes up with Prometheus, Loki, and Tempo already wired in as
datasources — nothing to configure by hand.

`order-service` and `inventory-service` each get their own database on
the same Postgres instance (ADR 0004); `infra/postgres/init-inventory-db.sh`
creates `inventory` on first startup. If you ran `docker compose up`
before M3, that init script never ran against your existing volume —
`docker compose down -v` and bring Postgres back up to pick up the second
database (this drops all local Postgres data).

Once Postgres is up, run both services (`payment-service` lands in M8):

```bash
mvn -pl services/inventory-service -am spring-boot:run   # http://localhost:8082
mvn -pl services/order-service -am spring-boot:run       # http://localhost:8081
```

Each migrates its own schema via Liquibase on startup. `order-service`
calls `inventory-service` synchronously over HTTP to reserve stock when
placing an order, so start `inventory-service` first.

```bash
# seed some stock
curl -X POST http://localhost:8082/stock-items \
  -H "Content-Type: application/json" \
  -d '{"sku":"WIDGET-1","quantity":10}'

# place an order — reserves stock in inventory-service synchronously
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"customerId":"11111111-1111-1111-1111-111111111111","currency":"USD","lineItems":[{"sku":"WIDGET-1","quantity":2,"unitPrice":9.99}]}'

curl http://localhost:8081/orders/{id}
curl -X POST http://localhost:8081/orders/{id}/cancel   # releases the reservation, if any

curl http://localhost:8082/stock-items/WIDGET-1
```

`Idempotency-Key` is required on `POST /orders` — a client retry that
reuses the same key gets back the order already placed under it instead
of creating a second one. If inventory can't cover a line item, the order
still gets created and is retrievable — just as `CANCELLED` rather than
`PLACED`, with no reservation held (see `ROADMAP.md`'s M3 entry).

## Documentation

- [AGENTS.md](./AGENTS.md) — stack, package structure, Javadoc standard
- [CONTRIBUTING.md](./CONTRIBUTING.md) — branch, commit, and PR conventions
- [ROADMAP.md](./ROADMAP.md) — milestones and progress
- [docs/adr/](./docs/adr/) — architecture decision records
