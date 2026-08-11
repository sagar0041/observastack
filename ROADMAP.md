# Roadmap

Order-processing microservices (Order, Inventory, Payment) instrumented
end-to-end for observability: distributed tracing, structured logging,
metrics, SLO-based alerting, and a documented incident-response exercise.

## Milestones

- [ ] **M0** Repo scaffold & local dev environment — docker-compose with
      Prometheus, Grafana, Loki, Tempo running cleanly
- [ ] **M1** Order Service — core API, PostgreSQL persistence, tests
- [ ] **M2** Inventory Service — stock check/reserve API, first
      cross-service call
- [ ] **M3** OpenTelemetry tracing across both services
- [ ] **M4** Structured logs correlated to traces (Loki)
- [ ] **M5** Metrics — RED + JVM/connection-pool metrics via Prometheus
- [ ] **M6** Grafana dashboards as code
- [ ] **M7** Kafka async flow + Payment Service, trace context across
      the async boundary
- [ ] **M8** SLO definition + Grafana alerting on burn rate
- [ ] **M9** Fault-injection exercise + incident writeup
      (`docs/incident-runbook.md`)
- [ ] **M10** *(stretch)* Kubernetes/Helm deployment
- [ ] **M11** Docs & polish — architecture diagram, dashboard screenshots

## Engineering Notes

Dated entries as milestones land — what was built, why, and any trade-offs.