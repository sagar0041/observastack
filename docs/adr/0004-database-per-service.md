# 4. One PostgreSQL database per service

Date: 2026-08-21

## Status

Accepted

## Context

M2 put `order-service`'s tables in the single `observastack` database M1
provisions, with a note that nothing had yet forced a decision on
schema-per-service versus database-per-service. M3 adds
`inventory-service`, the first service that needs tables of its own, so
that decision can no longer be deferred.

Both options run on the same M1 Postgres instance either way — this is
about database boundaries, not infrastructure. A shared database with two
schemas (or even two unprefixed table sets) would work today and is less
moving parts.

## Decision

Each service gets its own database on the shared Postgres instance:
`observastack` for `order-service`, `inventory` for `inventory-service`.
`infra/postgres/init-inventory-db.sh` creates the second database via
Postgres's standard `/docker-entrypoint-initdb.d/` mechanism.

Each service's Liquibase changelog only ever runs against its own
database, and nothing in either service's code can write to the other's
tables — the only path between them is `inventory-service`'s HTTP API.

## Consequences

A shared Postgres instance is still a single point of failure and a
capacity bottleneck across services, which per-database isolation does
nothing to fix — that's a hosting/topology decision, not a schema one,
and stays out of scope here.

What per-database isolation does buy, now rather than later: no service
can ever join across another service's tables or run a migration that
touches them, so the HTTP boundary between `order-service` and
`inventory-service` stays the only real integration point — the one this
system exists to demonstrate tracing across (M4).

The cost is operational: `docker-entrypoint-initdb.d/` scripts only run
against an empty data directory, so an already-initialized `postgres-data`
volume from before this change needs recreating (`docker compose down -v`)
to pick up the new database. Documented on the init script itself and in
`ROADMAP.md`'s M3 entry; a schema-per-service split inside one database
wouldn't have had this problem, which is the real trade this decision
makes.
