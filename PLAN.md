# ObservaStack — Project Plan

Order-processing microservices (Order, Inventory, Payment) instrumented
end-to-end with OpenTelemetry, feeding Prometheus (metrics), Loki (logs),
and Tempo (traces) via Grafana. Includes real SLO-based alerting and a
documented, deliberately-injected incident diagnosed using the stack itself.

**Stack:** Java 21, Spring Boot 3, OpenTelemetry, Micrometer, Prometheus,
Grafana, Loki, Tempo, Kafka, PostgreSQL, Docker Compose, GitHub Actions.
Kubernetes/Helm is a stretch goal (P10).

## Working agreement (read this before every session)

- Work through tasks **in order**. Skip only if genuinely blocked, and say why.
- A task isn't done until it's tested and passing. Don't commit red.
- Commit messages are real: imperative mood, what changed + why, reference
  the task ID below. No invented timestamps, no pretending to be a persona.
- Grafana dashboards and alert rules are provisioned as code (YAML/JSON in
  `infra/`), not clicked together in the UI — so they're reviewable in git.
- After finishing a task, check it off and add 2-3 lines to the Progress
  Log — plain language, honest about what was hard or cut corners.
- End every session with a short summary for Sagar: what changed, why,
  what to review, and any open question.

## Tasks

- [ ] **P0** Repo scaffold — done (this folder). Wire docker-compose fully,
      confirm `docker compose up` brings up Prometheus/Grafana/Loki/Tempo
      cleanly, get CI building an empty multi-module skeleton.
- [ ] **P1** Order Service — Spring Boot module, `POST /orders`,
      `GET /orders/{id}`, PostgreSQL persistence, unit + slice tests.
- [ ] **P2** Inventory Service — stock check/reserve API, PostgreSQL.
      Order Service calls it synchronously over REST — first cross-service
      call to trace.
- [ ] **P3** OpenTelemetry instrumentation — attach OTel Java agent to both
      services, auto-instrument HTTP calls, export traces to Tempo. Verify
      the Order → Inventory trace waterfall renders in Grafana.
- [ ] **P4** Structured logging + trace correlation — JSON logs with
      trace_id/span_id via MDC, shipped to Loki. Confirm you can click a
      trace in Grafana and jump straight to its correlated logs.
- [ ] **P5** Metrics — Micrometer → Prometheus. RED metrics (rate, errors,
      duration) per endpoint, JVM + HikariCP pool metrics, one custom
      business metric (orders/min).
- [ ] **P6** Grafana dashboards as code — golden-signals dashboard per
      service + one system overview, provisioned via JSON in `infra/`.
- [ ] **P7** Kafka async flow — add Payment Service, triggered by an
      `order.created` Kafka event from Order Service. Confirm trace context
      propagates across the async boundary (the part most demo projects skip).
- [ ] **P8** Alerting + SLOs — define one real SLO (e.g. 99% of orders
      processed under 500ms), Grafana Unified Alerting rule on burn rate.
- [ ] **P9** Chaos/incident exercise — deliberately inject a fault (e.g.
      connection pool exhaustion). Diagnose it using only the dashboards,
      then write `docs/incident-runbook.md` as an honest postmortem:
      what broke, which signal caught it first, time-to-diagnosis, the fix.
- [ ] **P10** *(stretch)* Kubernetes deployment — Helm chart, deploy to
      local kind/minikube, Prometheus Operator ServiceMonitor.
- [ ] **P11** Docs & polish — architecture diagram (update it as things
      actually changed, not just at the end), README with dashboard
      screenshots/GIFs, CI badge.

## Progress Log

<!-- Each entry: date, task ID, 2-3 honest lines on what was built and any
     decisions or trade-offs made. -->
