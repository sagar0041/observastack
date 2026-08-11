# 3. Use the OpenTelemetry Java agent rather than manual instrumentation

Date: 2026-08-11

## Status

Accepted

## Context

Traces can be produced either by the OpenTelemetry Java agent, attached at
JVM startup and instrumenting known libraries by bytecode manipulation, or
by hand with the OpenTelemetry API in application code.

Manual instrumentation gives precise control and no startup weight. The
agent gives immediate coverage of Spring MVC, JDBC, Kafka, and HTTP
clients without touching application code.

## Decision

Attach the agent, and add manual spans only where a business operation
spans several instrumented calls and the automatic trace does not tell the
story.

## Consequences

The agent adds JVM startup time and a class of failure that is invisible
in application code — when a trace is missing, the cause may be agent
configuration rather than anything in the codebase. Version compatibility
between the agent and instrumented libraries becomes an upgrade concern.

Accepted because the alternative is instrumentation code interleaved
through every layer, which obscures the business logic this project is
trying to keep legible.
