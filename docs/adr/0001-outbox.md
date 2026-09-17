# ADR 0001 — Transactional outbox (polling), not Debezium

## Status
Accepted

## Context
Shipment booking must not "forget" the Kafka event if the process crashes after the DB commit. Dual-write (DB then Kafka) is the classic consistency bug.

## Decision
Write `outbox_event` in the same Postgres transaction as the aggregate. A 400ms poller publishes unpublished rows. Consumers are idempotent on `eventId`.

## Consequences
- Works on any Kafka + Postgres pair (local Docker, EKS, AKS) without CDC privileges.
- Slightly higher publish latency than log-tailing CDC. Debezium is the next step when the platform team owns the replication slot.
