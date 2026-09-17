# ADR 0002 — Choreography saga for credit reservation

## Status
Accepted

## Context
Booking a shipment must reserve customer credit. A synchronous two-phase call from shipment to finance couples availability and timeout budgets. An orchestrator service would be a new single point of failure.

## Decision
Choreography:

1. Shipment persists `PENDING` + `ShipmentBooked` outbox.
2. Finance consumes, reserves or rejects, emits `CreditReserved` / `CreditRejected`.
3. Shipment confirms or compensates (`REJECTED`) and refreshes the Redis tracking cache.

## Consequences
- Happy path is eventually consistent for a few hundred milliseconds.
- Tracking reads after POST may still show `PENDING` until finance replies — the API documents this.
- Duplicate Kafka deliveries are absorbed by `processed_event` (finance) and status guards (shipment).
