# Roadmap

Milestones are ordered. Each ships as one pull request.

- [x] **M0** Repository setup — conventions, ADRs, build skeleton, CI
- [x] **M1** Local environment — Docker Compose with Prometheus, Grafana,
      Loki, Tempo, PostgreSQL; verified starting cleanly
- [ ] **M2** Order Service — domain model, placement use case, REST API,
      PostgreSQL persistence via Liquibase
- [ ] **M3** Inventory Service — stock reservation with concurrency
      handling; Order calls it synchronously
- [ ] **M4** Distributed tracing — OpenTelemetry agent on both services,
      Order → Inventory trace visible in Grafana
- [ ] **M5** Log correlation — structured JSON logs carrying trace_id,
      shipped to Loki, navigable from a trace
- [ ] **M6** Metrics — RED per endpoint, JVM and connection pool metrics,
      one business metric
- [ ] **M7** Dashboards as code — golden signals per service plus a system
      overview, provisioned from `infra/`
- [ ] **M8** Asynchronous flow — Payment Service consuming `OrderPlaced`;
      trace context propagated across the Kafka boundary
- [ ] **M9** Resilience — circuit breaker on the Order → Inventory call,
      with metrics exposed
- [ ] **M10** SLOs and alerting — latency and availability objectives,
      Grafana alert rules on error budget burn
- [ ] **M11** Incident exercise — inject a fault, diagnose it using only
      the dashboards, write up `docs/incident-runbook.md`
- [ ] **M12** Kubernetes deployment (stretch) — Helm chart, ServiceMonitor
- [ ] **M13** Documentation — architecture diagrams, dashboard captures

## Notes

Dated entries as milestones land: what was built, what was decided, and
what was traded away.

### 2026-08-11 — M1

Added `docker-compose.yml` at the repo root with Prometheus, Grafana,
Loki, Tempo, and PostgreSQL, all with pinned image tags and healthchecks.
Config files live under `infra/` (`infra/prometheus/`, `infra/loki/`,
`infra/tempo/`, `infra/grafana/provisioning/`) and are mounted read-only
into the containers. Grafana is provisioned with all three datasources
on startup — no manual UI configuration.

Kafka sits behind `docker compose --profile kafka up -d` since
order/inventory/payment services don't need it until M8, and a plain
`docker compose up -d` should not pay for a broker nobody's using yet.

Verified locally: `docker compose up -d` brings up postgres, prometheus,
loki, tempo, and grafana, and all five reach Docker's `healthy` state.
Beyond the healthcheck, also checked each one is actually doing its job:
Prometheus is scraping itself (`up`), Loki's `/ready` and Tempo's
`/ready` both respond, and Grafana's provisioned Prometheus/Loki
datasources pass their `/health` check while Tempo's search API answers
through the Grafana proxy (Tempo's datasource plugin doesn't implement
the generic `/health` route, so that one shows "not implemented" in the
Grafana UI — connectivity itself is confirmed via the proxied search
call, and that's expected quirk of the Tempo plugin, not a stack
problem). Postgres accepts connections and reports version 16.4.

Separately started `docker compose --profile kafka up -d kafka` and
confirmed it reaches `healthy` and that a topic create/produce/consume
round-trip works. Confirmed a plain `docker compose up -d` (no profile)
does not start Kafka.

Not verified: behavior on a machine other than this session's container,
Grafana anonymous/multi-user auth, and anything past what M1 asks for —
no dashboards, no scrape targets beyond Prometheus itself (those come
with the services in M2+ and dashboards-as-code in M7).
