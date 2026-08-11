# 1. Record architecture decisions

Date: 2026-08-11

## Status

Accepted

## Context

Design choices made early in a project are difficult to reconstruct later.
Without a record, the reasoning behind a decision is lost and the decision
gets revisited repeatedly, or worse, reversed by someone who never saw the
constraint that motivated it.

## Decision

Architecture decisions are recorded as short markdown files in
`docs/adr/`, numbered sequentially and never edited once accepted. A
decision that is later reversed gets a new ADR marking the old one as
superseded.

Each ADR states the context, the decision, and the consequences —
including the consequences we are unhappy about.

## Consequences

Adding a dependency or changing a structural pattern now requires writing
a paragraph justifying it. This is deliberate friction.
