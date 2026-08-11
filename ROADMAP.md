# Roadmap

Milestones are ordered. Each ships as one pull request.

- [x] **M0** Repository setup — conventions, ADRs, build skeleton, CI
- [ ] **M1** Local environment — Docker Compose with Prometheus, Grafana,
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
