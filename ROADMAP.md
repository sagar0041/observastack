# Roadmap

Milestones are ordered. Each ships as one pull request.

- [x] **M0** Repository setup — conventions, ADRs, build skeleton, CI
- [x] **M1** Local environment — Docker Compose with Prometheus, Grafana,
      Loki, Tempo, PostgreSQL; verified starting cleanly
- [x] **M2** Order Service — domain model, placement use case, REST API,
      PostgreSQL persistence via Liquibase
- [x] **M3** Inventory Service — stock reservation with concurrency
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

### 2026-08-12 — M2

Added the reactor parent `pom.xml` (dependency management only, per
`AGENTS.md`'s module layout) and the first module, `services/order-service`,
on Spring Boot 3.3.13. Neither existed before this milestone despite M0
being marked done — M0's "build skeleton" turned out to be aspirational;
this is the first real Maven build in the repo.

**Domain.** `Order` is the aggregate: a private constructor enforces
invariants (non-null customer, at least one line item — see
`EmptyOrderException`), and there is no setter, only `place()` and
`cancel()`, each of which validates the current `OrderStatus` before
transitioning. Lifecycle is `CREATED -> PLACED -> CANCELLED`:
`CREATED` is the construction-time state — invariants hold, but nothing's
committed yet; `PLACED` is the real business event, the one M3's
inventory reservation and M8's `OrderPlaced` Kafka event react to;
`CANCELLED` is a terminal state reachable from either, for when
placement can't be honoured or a customer backs out. There's no route
back to `CREATED` from `PLACED` — placement is one-way. `Order.create`
and `Order.place`/`cancel` take a `java.time.Clock` rather than calling
`Instant.now()` directly, so timestamp assertions in tests are exact
rather than range checks. Line items, SKU, money, and both ids are
value objects (Java records) with their own validating constructors —
`Money` normalises scale to 2 decimal places so `"5"` and `"5.00"`
compare equal, and rejects rather than rounds anything with more
precision than that.

**Persistence.** `OrderEntity` is a separate JPA entity with no
compile-time dependency on the domain package (ADR 0002); line items
are `@ElementCollection @Embeddable`, not their own `@Entity`, because
they have no identity independent of the order, matching the domain
model. `OrderMapper` is a MapStruct `@Mapper` — the line-item and
value-object conversions are generated, but the top-level
`Order`/`OrderEntity` conversion is hand-written inside the same class,
because `Order`'s constructor is private and MapStruct's bean-mapping
strategy doesn't apply to an aggregate with no mutation surface; this
is documented on the class. Schema is two Liquibase-managed tables,
`orders` and `order_line_items`, both currently in the `observastack`
database M1 already provisions — no schema-per-service split yet, since
nothing has forced that decision.

**API.** `POST /orders` and `GET /orders/{id}`, with `PlaceOrderRequest`
/ `OrderResponse` DTOs that stay in the `api` package; the application
layer's `PlaceOrderCommand` carries primitives, not domain types, so the
use case — not the controller — is what turns them into `Sku`/`Money`/
`OrderLineItem`.

**Tests, 44 total, all passing (as of the second review round below;
28 originally):** `OrderTest`/`OrderLineItemTest`/
`MoneyTest` cover domain invariants with no Spring context;
`OrderRepositoryImplTest` is `@DataJpaTest` against a real Testcontainers
Postgres running the actual Liquibase changelog; `PlaceOrderEndToEndTest`
is `@SpringBootTest` with a random port and `TestRestTemplate`, hitting
the real HTTP endpoints against another Testcontainers Postgres —
nothing mocked, so it's the placement use case exercised through every
layer at once, not just the service method.

Beyond `mvn verify`, also ran the built jar against the actual M1
`docker-compose` Postgres and drove it with real `curl` requests: placed
an order, fetched it back by id, confirmed a 404 on an unknown id and a
400 on an empty line-item list, then checked the rows directly with
`psql` to confirm what the API returned matches what's actually in the
`orders` and `order_line_items` tables.

Two build-environment problems surfaced and are recorded as such, not
silently worked around: Spring Boot 3.3.13 manages Testcontainers
1.19.8, whose bundled `docker-java` client can't negotiate with a Docker
Engine that dropped API versions below 1.40 — fixed by bumping
`testcontainers.version` to 1.20.4 and pinning `api.version=1.44` in a
`docker-java.properties` test resource, both specific to this sandbox's
Docker Engine version rather than a real project requirement. Separately,
declaring `spring-boot-maven-plugin` without an explicit `repackage`
execution produces a plain jar with no main manifest attribute — easy to
miss since `mvn verify` doesn't run the jar, only builds it; caught by
actually launching it.

**Review round:** asked for a proper review after opening the PR. Two
findings, both fixed:
1. `PlaceOrderRequest` had no upper bound on SKU length or price
   magnitude, so a SKU over 64 characters or a price with more than 10
   integer digits passed Bean Validation and the domain layer, then
   failed at the database (`sku varchar(64)`, `unit_price
   numeric(12,2)`) as an unhandled `DataIntegrityViolationException` —
   a 500, not the 400 a validation failure should be. Fixed with
   `@Size(max = 64)` and `@Digits(integer = 10, fraction = 2)` on the
   request DTO, plus a `DataIntegrityViolationException` handler as a
   backstop for anything that still gets through. Covered by two new
   `PlaceOrderEndToEndTest` cases.
2. `OrderEntity` had no `@GeneratedValue` and didn't implement
   `Persistable` — its id is assigned by the domain layer
   (`OrderId.newId()`), so Spring Data JPA's default `isNew()` check
   (id == null) always saw a non-null id and called `merge()` instead
   of `persist()`, issuing a needless existence-checking `SELECT`
   before every single insert. Fixed by implementing
   `Persistable<UUID>` with a `@Transient isNew` flag reset by
   `@PostLoad`/`@PostPersist`. Verified by hand — ran the built jar
   against the real M1 Postgres with `spring.jpa.show-sql=true` and
   confirmed placing an order now issues a bare `insert into orders`
   with no preceding `select`; not covered by an automated test, since
   asserting on statement counts wasn't already in this suite's toolkit
   and adding one felt like more machinery than the fix warranted.

**Second review round:** asked for stronger domain modeling and
consideration of edge cases and operational aspects, including "handling
alternative journey options and managing long-running requests across
distributed integrations." That last part doesn't apply yet —
order-service doesn't call anything external in M2 (Inventory is M3,
Kafka is M8) — so distributed-integration resilience stays deferred to
those milestones rather than being speculatively designed against
nothing, per M9's own scope (circuit breaker, with metrics). What's
actually addressable within M2 today:

1. **Idempotent placement.** `POST /orders` now requires an
   `Idempotency-Key` header. `Order` gained an `IdempotencyKey` value
   object and a unique DB constraint on it; `PlaceOrderService` checks
   `OrderRepository.findByIdempotencyKey` before building a new order,
   so a client retry after a dropped response returns the order already
   placed rather than creating a second one. The rarer case — two
   requests with the same key reaching the database at nearly the same
   instant — is handled by the unique constraint itself:
   `OrderRepositoryImpl.save` translates the resulting
   `DataIntegrityViolationException` into a domain
   `DuplicateIdempotencyKeyException`, surfaced as 409, and the caller
   retrying the read (or the request) will find the winner's order.
   Building this test-drove a real Hibernate gotcha: `save()`'s
   try/catch didn't actually work at first, because plain
   `JpaRepository.save()` only *queues* the insert — Hibernate doesn't
   send it, and so doesn't hit the constraint, until some later flush.
   Fixed with `saveAndFlush`, which keeps the constraint check inside
   the block that's actually handling it. Covered by
   `OrderRepositoryImplTest` (sequential saves with a shared key,
   deterministic, no thread orchestration needed) and
   `PlaceOrderEndToEndTest` (a real replayed HTTP request).

2. **Cancellation exposed via the API.** `Order.cancel()` already
   existed (that's the whole reason `CANCELLED` was in the lifecycle),
   but nothing called it. Added `POST /orders/{id}/cancel` backed by a
   new `CancelOrderService`. This is the first genuine *update* path in
   the service — everything before this only ever inserted — and it
   exposed the flip side of the M2 review's `Persistable` fix: the
   mapper always builds a brand-new `OrderEntity`, so passing that
   through `OrderRepository.save()`'s existing insert-optimized path
   would have tried to `persist()` a row that already exists. Rather
   than make `save()` guess which case it's in, `OrderRepository` now
   has a separate `update(Order)`: load the already-managed entity,
   apply just the fields that can actually change
   (`status`/`placedAt`/`cancelledAt` — line items, customer, and
   identity are fixed for the life of an order), and let Hibernate's
   dirty checking do the rest. That last part wasn't obvious either:
   the first version called `jpaRepository.save()` on the already-managed
   entity anyway, which routes through `merge()` since `Persistable`
   correctly reports it as not-new — and merging an entity with itself
   broke on the `lineItems` collection (`UnsupportedOperationException`
   inside Hibernate's collection-replace code). Fixed by dropping the
   redundant `save()` call entirely in favor of `jpaRepository.flush()`.
   Both bugs were caught by the tests written for this feature, not
   found by inspection — `OrderRepositoryImplTest` now covers `update`
   directly, and `PlaceOrderEndToEndTest` drives cancellation through
   real HTTP.

3. **Currency on `Money`.** `Money` was an amount with no currency,
   which is fine for a single-currency demo but not something a real
   model should assume silently. `Money` now carries a `java.util.Currency`
   and refuses to `add()` across currencies; `Order`'s constructor
   checks all line items share one currency and throws
   `MixedCurrencyException` if not, rather than letting a mismatch
   surface later as a confusing `totalPrice()` failure. `currency()` on
   `Order` is derived from the line items, not stored redundantly.

Also touched: `PlaceOrderRequest`'s Bean Validation now includes an
ISO-4217-shape check on the currency field, `OrderMapper` gained a
second genuinely-generated MapStruct mapping (line items — `sku`/
`quantity` map directly, `unitPrice`/`currency` via nested-property
`source` paths, with one `expression` where two flat columns have to
recombine into one `Money`), and the Liquibase changelog picked up a
`0002` changeset rather than editing `0001` — schema changes get new
changesets from here on, not edits to ones already shipped, even though
nothing has actually deployed this yet. That changeset adds its new
columns as `NOT NULL` with no default, which only works against an
empty table; there's no real data to migrate around yet, but a
production version of this same change would need a default value and
a backfill step first.

Verified the same way as before: `mvn clean verify` (44 tests, all
passing, including the two failures above that the tests themselves
caught before the manual verification pass), plus the built jar run
against a **freshly reset** M1 Postgres volume (the pre-existing rows
from earlier ad-hoc `curl` testing didn't have the new `NOT NULL`
columns and correctly blocked the migration until the volume was
dropped) — placed an order, replayed it with the same idempotency key
and confirmed one row in `psql`, cancelled it, confirmed the cancel
rejected a second attempt, and confirmed a missing `Idempotency-Key`
header returns 400.

Per the review discussion, a permanent AGENTS.md rule about
idempotency/retry-safety on mutating endpoints was drafted for review
rather than added directly — see the PR thread; it isn't committed
here.

Not verified: true concurrent placement under real thread contention
(the duplicate-key path is tested sequentially, which exercises the
same code but not actual simultaneous requests); behavior against a
differently-versioned Docker Engine or Postgres than this session's;
anything past M2's scope — no inventory call, no Kafka, no
tracing/metrics/logs correlation (M3-M8), no resilience patterns ahead
of M9.

### 2026-08-21 — M3

Added `services/inventory-service`, a second Spring Boot module with the
same `domain`/`application`/`infrastructure`/`api` layout as
`order-service`. It owns its own database, `inventory`, separate from
`order-service`'s `observastack` (ADR 0004) — `infra/postgres/init-inventory-db.sh`
creates it via Postgres's standard init-script mechanism.

**Domain.** Two aggregates. `StockItem` (keyed by `Sku`) holds an
`availableQuantity` and exposes `reserve(quantity)` /
`release(quantity)`, both throwing on a non-positive quantity,
`reserve` additionally throwing `InsufficientStockException` when
quantity exceeds what's available. `Reservation` (keyed by a generated
`ReservationId`, referencing an `OrderId`) holds the SKU/quantity lines
it was created with and a `releasedAt` set at most once — releasing an
already-released reservation throws `IllegalReservationStateException`.
Stock only ever enters the system through `POST /stock-items`
(`CreateStockItemService`) — no restock/adjustment API; deliberately out
of scope for this milestone.

**Concurrency.** This is the milestone's actual point, so it's worth
stating the mechanism precisely. `StockItemEntity` carries a JPA
`@Version` column; `StockItemRepositoryImpl.update()` loads the managed
entity, applies the new quantity, and calls `flush()` inside a
try/catch for `ObjectOptimisticLockingFailureException`, translating it
to a domain `ConcurrentStockUpdateException`. `ReserveStockService` /
`ReleaseStockService` catch that and retry the whole attempt (default
10 attempts — deliberately generous, since retries are cheap and a low
ceiling would wrongly reject a request that lost one race but still had
stock available on the next attempt) through a *separate* collaborator
bean, `StockLedgerWriter`. That split matters: `@Transactional` on a
method only starts a new transaction when called from *outside* the
bean, so a retry loop calling `this.reserveOnce()` on itself would
silently reuse one open transaction across every attempt instead of
getting a fresh one per try — a bug M2 hit once already and this
milestone was designed from the start to avoid repeating. Two lines for
the same SKU in one reservation request are aggregated into a single
decrement before touching the database, rather than processed as two
independent read-modify-write cycles that would lose one of them.

Proven under real contention, not just reasoned about: `ReserveStockConcurrencyTest`
seeds one SKU with 10 units, fires 25 threads at `POST /reservations`
simultaneously (two `CountDownLatch`es — one for "every thread is ready
and waiting", released together by a second — rather than just starting
25 threads and hoping the OS schedules them close together), and asserts
exactly 10 succeed, 15 are rejected as insufficient stock, and the final
available quantity is exactly 0. Run three times to check for flakiness;
stable all three times.

**Order → Inventory integration.** `order-service` gained
`InventoryPort` (application layer) — `reserve(OrderId, lines)` /
`release(OrderId)` — with `InventoryClient` (`infrastructure/client`) as
its only implementation, a `RestClient` with a 2s connect / 5s read
timeout so a stalled inventory-service can't hang order placement
indefinitely. `PlaceOrderService` now builds the order, attempts a
reservation, and reacts to the two distinct failure shapes differently:
`StockUnavailableException` (inventory looked and said no — a 409 or
404 from a reservation attempt) is caught and the order is cancelled
instead of placed, still returning 201 with a durable, retrievable
`CANCELLED` order rather than an error response; anything else
(`RestClientException` — inventory down, timed out, or erroring) is left
to propagate, surfaced by `order-service`'s API as 503, because
conflating "inventory said no" with "we couldn't even ask" would make an
infrastructure outage look like an out-of-stock item to the caller.
`CancelOrderService` releases the reservation, but only when the order
was actually `PLACED` — a `CREATED`-then-cancelled order never had one.

Idempotency on `POST /reservations` follows the same shape M2 already
established for `POST /orders`, now written down as a rule in
`AGENTS.md` rather than living only in two places by convention: the
order's id is the natural key (one order has at most one reservation),
backed by a unique constraint (`uq_reservations_order_id`) that
`ReservationRepositoryImpl.save` translates into a domain
`DuplicateReservationException` on the rare concurrent-race path, the
same pattern `order-service`'s idempotency-key handling already used.

**Tests.** 37 in `inventory-service` (domain invariants with no Spring
context; `@DataJpaTest` repository tests against Testcontainers Postgres
— including a deterministic optimistic-lock-conflict test that mutates a
row via `TestEntityManager` native SQL to force a stale read without
needing real threads; the full `@SpringBootTest` end-to-end suite; the
concurrency test above). `order-service` grew from 44 to 46 plus a new
7-test `InventoryClientTest` (53 total): `PlaceOrderEndToEndTest` now
covers the stock-unavailable-cancels-instead-of-fails path and
cancel-releases-the-reservation, using a hand-written `FakeInventoryPort`
(`@TestConfiguration` + `@Primary`) rather than standing up a second real
Spring Boot app with its own Testcontainers Postgres just to test how
`order-service` reacts to a reservation outcome — that reaction is this
test's job, inventory-service's own reservation logic is
`inventory-service`'s test suite's job, and HTTP-response-to-exception
translation is `InventoryClientTest`'s, using `MockRestServiceServer`
against the real `InventoryClient`. 90 tests total across the reactor
(53 in `order-service`, 37 in `inventory-service`), `mvn clean verify`
green.

Beyond the automated suite: ran both built jars against the real M1
`docker-compose` Postgres (freshly recreated volume, to pick up the new
`inventory` database) and drove the whole flow with `curl` — created a
`WIDGET-1` stock item with 10 units, placed an order for 3 (confirmed
`PLACED`, stock now 7), cancelled it (confirmed stock back to 10), then
placed an order for 12 against 10 available and confirmed it came back
`CANCELLED` with `placedAt: null` and stock untouched — the core
concurrency-handling scenario the automated test proves under load,
reproduced once here against real HTTP and real Postgres end to end.
Confirmed with `psql \dt` against each database directly that
`order-service`'s tables live only in `observastack` and
`inventory-service`'s only in `inventory`.

A latent bug was found and fixed along the way, in already-merged M2
code: `OrderEntity.lineItems` was `@ElementCollection` with the JPA
default (lazy) fetch, which worked in M2 only because every call site
happened to run inside a transaction. Building `ReservationEntity`'s
equivalent collection surfaced the general problem — a repository port
should return fully-usable domain objects regardless of the caller's
transactional context, and `ReserveStockService`'s idempotency check
calls `findByOrderId` outside a transaction on purpose, to keep the
retry loop's transaction boundaries in `StockLedgerWriter` — so both
collections are now `FetchType.EAGER`, with the reasoning recorded on
both entities.

Not verified: behavior on a machine other than this session's container;
`inventory-service` under a real network partition rather than a
same-host localhost call (that's what M9's circuit breaker is scoped to
address); any restock/inventory-adjustment path (doesn't exist yet, and
wasn't asked for); tracing/metrics/logs correlation across the
Order → Inventory HTTP call (M4-M6).
