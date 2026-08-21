# AGENTS.md

Conventions for this repository. Applies to every contributor.

## Stack

| Concern          | Choice                                      |
|------------------|---------------------------------------------|
| Language         | Java 21 (LTS)                               |
| Framework        | Spring Boot 3.3.x                           |
| Build            | Maven, multi-module                         |
| Persistence      | PostgreSQL 16, Spring Data JPA, Liquibase   |
| Messaging        | Apache Kafka (Spring for Apache Kafka)      |
| Resilience       | Resilience4j                                |
| Mapping          | MapStruct (domain ↔ persistence)            |
| Tracing          | OpenTelemetry Java agent → Tempo            |
| Metrics          | Micrometer → Prometheus                     |
| Logs             | Logback JSON encoder → Loki                 |
| Dashboards       | Grafana, provisioned as code                |
| Testing          | JUnit 5, AssertJ, Testcontainers            |
| CI               | GitHub Actions                              |
| Local env        | Docker Compose                              |

Do not introduce a new library or framework without adding an ADR under
`docs/adr/` explaining the choice.

## Module layout

```
observastack/
├── pom.xml                  # parent POM, dependency management only
├── services/
│   ├── order-service/
│   ├── inventory-service/
│   └── payment-service/
├── infra/                   # Prometheus, Grafana, Loki, Tempo config
├── docs/adr/                # architecture decision records
└── .github/workflows/
```

## Package structure (per service)

Organised by domain layer, not by technical type. Base package
`com.observastack.<service>`.

```
domain/            Entities, value objects, domain events, repository
                   interfaces, domain exceptions. No Spring, no JPA, no
                   Jackson annotations. This layer must compile with zero
                   framework dependencies on the classpath.
application/       Use-case services that orchestrate domain objects.
                   Transaction boundaries live here. Depends on domain
                   only, via interfaces.
infrastructure/
  persistence/     JPA entities, Spring Data repositories, MapStruct
                   mappers, implementations of domain repository interfaces.
  messaging/       Kafka producers and consumers, event serialisation.
  client/          Outbound HTTP clients implementing application-layer ports.
  config/          Spring configuration classes.
api/               REST controllers, request/response DTOs, exception
                   handlers. DTOs never leak into application or domain.
```

Dependency rule: `api` → `application` → `domain`. `infrastructure`
implements interfaces owned by inner layers. Nothing in `domain` imports
from any other package in the service.

## Javadoc standard

Required on every public type and every public method that isn't a trivial
accessor. Document intent and contract, not restatement of the signature.

Format:

```java
/**
 * Reserves stock for an order line, decrementing available quantity.
 *
 * <p>Reservation is optimistic: the caller may receive
 * {@link InsufficientStockException} even after a successful availability
 * check, because concurrent reservations are resolved at commit time.
 *
 * @param sku         the stock keeping unit to reserve; must not be null
 * @param quantity    units to reserve; must be positive
 * @param orderId     order the reservation belongs to, used for release
 * @return the created reservation, never null
 * @throws InsufficientStockException if available stock is below quantity
 * @throws IllegalArgumentException   if quantity is not positive
 */
Reservation reserve(Sku sku, int quantity, OrderId orderId);
```

Rules:
- One-sentence summary first, ending with a period. Imperative for methods
  ("Reserves stock"), noun phrase for types.
- Use `<p>` for additional paragraphs. Never a wall of prose.
- Always document `@param`, `@return`, `@throws` where they apply.
- Document nullability and validity constraints explicitly.
- On domain classes, state the invariant the type enforces.
- Use `{@link}` for cross-references rather than bare names.
- No `@author` tags. No commented-out code. No TODO without a linked issue.

## Testing

- Unit tests for domain logic — no Spring context, no mocks of value objects.
- `@DataJpaTest` with Testcontainers for repository implementations.
- `@SpringBootTest` with Testcontainers for use-case level integration tests.
- Given/when/then structure. Test names describe behaviour:
  `reserve_throwsInsufficientStock_whenQuantityExceedsAvailable`.
- Every bug fix starts with a failing test.

## Idempotency

Any endpoint that creates a resource as a side effect of a mutating call
must be safe to retry, because a client that never sees the response
(timeout, dropped connection) cannot tell "it failed" from "it succeeded
and the response was lost" and will retry. Two acceptable shapes:

- A client-supplied idempotency key on the request, checked against a
  unique constraint before creating anything new (`order-service`'s
  `POST /orders` — `Idempotency-Key` header, `IdempotencyKey` value
  object, `uq_orders_idempotency_key`).
- A natural key the caller already owns that the resource can only ever
  have one of (`inventory-service`'s `POST /reservations` — `orderId` is
  the natural key, since one order has at most one reservation;
  `uq_reservations_order_id` backs it the same way).

Either way, the unique constraint is the real guarantee, not the
pre-check — a pre-check-then-insert has a race between two concurrent
requests with the same key, so the repository implementation must also
translate the resulting constraint violation into a domain exception
rather than let it surface as an unhandled 500. See
`OrderRepositoryImpl.save` and `ReservationRepositoryImpl.save` for the
pattern.

## Definition of done

A milestone is not done until:
1. Tests pass locally (`mvn verify`).
2. Javadoc is present per the standard above.
3. `ROADMAP.md` is updated.
4. For milestones touching Docker: `docker compose up` has actually been
   run and verified. A green build is not evidence the stack starts.
