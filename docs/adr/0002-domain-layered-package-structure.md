# 2. Organise packages by domain layer

Date: 2026-08-11

## Status

Accepted

## Context

The conventional Spring Boot layout groups classes by technical type —
`controller/`, `service/`, `repository/`, `entity/`. This makes the
framework obvious and the business rules invisible: understanding what an
order actually *is* means opening four packages, and the domain model ends
up expressed as JPA entities with public setters, which is not a model at
all.

## Decision

Each service is organised by layer: `domain`, `application`,
`infrastructure`, `api`. The `domain` package holds entities, value
objects, domain events, and repository interfaces, and carries no
framework dependencies — no Spring annotations, no JPA, no Jackson.

Persistence uses separate JPA entities in `infrastructure/persistence`,
mapped to and from domain objects with MapStruct.

Dependencies point inward: `api` → `application` → `domain`.
`infrastructure` implements interfaces owned by the inner layers.

## Consequences

There is real mapping cost — two representations of an order, and a mapper
between them. For a system this size, that is more code than a single
annotated entity would be.

We accept it because it keeps invariants enforceable in constructors
rather than hoped for, makes the domain unit-testable without a Spring
context, and prevents persistence concerns from shaping the model. The
cost scales linearly; the benefit compounds.
