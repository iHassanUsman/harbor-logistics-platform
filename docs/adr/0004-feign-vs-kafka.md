# ADR 0004 — Feign for queries, Kafka for state changes

## Status
Accepted

## Context
Agent portals need a customer lookup *before* booking (fail fast). Credit reservation is a state change that other services must observe.

## Decision
- **GET** customer: OpenFeign + Resilience4j (timeout/retry on GET).
- **Mutations** that other bounded contexts must see: Kafka via outbox.

## Consequences
- Read path stays request-scoped and easy to debug.
- Write path survives finance or shipment restarts without dual-write loss.
