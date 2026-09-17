# Harbor Logistics Platform

Java **21** / Spring Boot **3.4** / Spring Cloud **2024.0** event-driven **logistics control plane**.

Original design for agent/customer booking, credit, and tracking. **Not** employer source and not a NetSuite/WSO2 clone.

| | |
| --- | --- |
| Stack | Gateway, OpenFeign, Kafka, Postgres outbox, Redis, Liquibase, OTel, Kubernetes |
| Hard problem | Booking + credit + tracking have **different consistency and scale** needs |
| Approach | Sync **reads** (Feign); async **writes** (outbox + choreography saga); cache tracking |

---

## System context

Agents never call finance or shipment databases. The gateway is the only public surface. Bounded contexts keep their own Postgres.

```mermaid
flowchart TB
  subgraph portals [Portals]
    Agent[Agent_portal]
    Customer[Customer_portal]
  end

  subgraph edge [Edge]
    Gw[Spring_Cloud_Gateway]
  end

  subgraph domains [Bounded_contexts]
    CustSvc[customer_service]
    ShipSvc[shipment_service]
    FinSvc[finance_service]
  end

  subgraph data [Data_plane]
    PgC[(Postgres_customer)]
    PgS[(Postgres_shipment)]
    PgF[(Postgres_finance)]
    Redis[(Redis_tracking)]
    Kafka[Kafka]
  end

  Agent -->|Bearer HTTPS| Gw
  Customer -->|Bearer HTTPS| Gw
  Gw --> CustSvc
  Gw --> ShipSvc
  Gw --> FinSvc
  CustSvc --> PgC
  ShipSvc --> PgS
  ShipSvc --> Redis
  FinSvc --> PgF
  ShipSvc -->|outbox| Kafka
  FinSvc -->|outbox| Kafka
  Kafka --> FinSvc
  Kafka --> ShipSvc
```

**Why not Eureka?** On EC2, client-side discovery was correct. On Kubernetes, DNS *is* discovery. See [ADR 0003](docs/adr/0003-no-eureka.md).

---

## Security — edge, blast radius, data

Harbor assumes the internet is hostile and **east-west traffic is still authenticated**. The repo ships a Bearer stub; production swaps it for JWKS.

```mermaid
flowchart TB
  subgraph public [Public]
    User[Browser_or_partner]
  end

  subgraph dmz [Ingress]
    TLS[TLS]
    Bearer[Bearer_required]
    Route[Path_based_routing]
  end

  subgraph private [Cluster_network_policy]
    Gw2[gateway]
    C[customer_no_public_DB]
    S[shipment]
    F[finance]
  end

  User --> TLS
  TLS --> Bearer
  Bearer -->|401 if missing| Deny[Deny]
  Bearer --> Route
  Route --> Gw2
  Gw2 --> C
  Gw2 --> S
  Gw2 --> F
  S -->|Feign GET only| C
  S -.->|no direct SQL| F
```

```mermaid
flowchart LR
  subgraph token [Token_in_production]
    IdP[IdP_JWKS]
    Claims[sub_roles_tenant]
    IdP --> Claims
  end

  subgraph enforce [Enforcement]
    Gw3[Gateway_validates_signature]
    Svc[Service_trusts_gateway_headers]
    Db[Separate_DB_per_context]
  end

  Claims --> Gw3
  Gw3 --> Svc
  Svc --> Db
```

| Control | In this repo | Production hardening |
| --- | --- | --- |
| Authentication | `BearerStubFilter` — missing Bearer → **401** | RS256 JWT vs IdP JWKS, short TTL, refresh rotation |
| Authorization | Demo token is a stand-in | Role claims: `AGENT` vs `CUSTOMER`; object-level checks on `customerId` |
| Service exposure | Only gateway is meant for Ingress | NetworkPolicy: pods cannot be reached from the internet |
| Dual-write / spoofed events | Outbox in the **same DB transaction** as the aggregate | Kafka ACL: only finance may produce `harbor.finance` |
| Replay / duplicates | `processed_event` in finance; status guard on shipment | Idempotency key on `POST /api/shipments` |
| Secrets | Env for JDBC / Kafka / Redis | External Secrets, RDS IAM, no connection strings in images |
| Data isolation | One database per service | Separate schemas + least-privilege DB users |
| Cache | Tracking TTL 10 minutes | Encrypt Redis at rest; do not cache PII without TTL + tenant key prefix |

Actuator is reachable for health in Compose; lock `/actuator/**` to the admin network in production.

---

## Scalability — split sync and async

Reads that must fail fast stay on HTTP. State that other contexts must observe goes through Kafka so finance and shipment scale independently.

```mermaid
flowchart TB
  subgraph syncPath [Synchronous_path]
    Book[POST_book]
    Feign[Feign_GET_customer]
    Write[(Write_PENDING_plus_outbox)]
    Book --> Feign --> Write
  end

  subgraph asyncPath [Asynchronous_path]
    Poll[Outbox_poller_400ms]
    K[Kafka]
    Fin[Finance_reserve_credit]
    Comp[Shipment_confirm_or_reject]
    Poll --> K --> Fin --> K --> Comp
  end

  subgraph scaleOut [Scale_independently]
    GwHpa[Gateway_HPA_CPU]
    ShipHpa[Shipment_HPA]
    FinHpa[Finance_HPA]
    Partitions[Kafka_partitions]
  end

  Write --> Poll
  Comp --> Cache[Redis_tracking]
```

| Bottleneck | Scale | Do not |
| --- | --- | --- |
| Edge QPS | Gateway replicas + CPU HPA | Put booking logic in the gateway |
| Tracking reads | Redis cache-aside, TTL | Hit Postgres on every poll from the portal |
| Credit reservation | Finance consumers × partitions | Two-phase HTTP from shipment to finance |
| Booking writes | Shipment replicas; outbox is per-row | Dual-write DB then Kafka |

Kubernetes HPA for shipment is in [`k8s/deploy.yaml`](k8s/deploy.yaml) (EC2 → Kubernetes story).

---

## Consistency — choreography saga

Booking is **not** a distributed lock. It is a documented eventually-consistent handshake.

```mermaid
sequenceDiagram
  participant A as Agent
  participant G as Gateway
  participant S as Shipment
  participant O as Outbox
  participant K as Kafka
  participant F as Finance

  A->>G: Bearer POST /api/shipments
  G->>S: route
  S->>S: Feign GET customer
  S->>S: persist PENDING
  S->>O: ShipmentBooked same TX
  S-->>A: 200 id status PENDING
  O->>K: publish
  K->>F: ShipmentBooked
  alt credit available
    F->>O: CreditReserved
    O->>K: publish
    K->>S: CreditReserved
    S->>S: CONFIRMED plus Redis
  else insufficient credit
    F->>O: CreditRejected
    O->>K: publish
    K->>S: CreditRejected
    S->>S: REJECTED compensate plus Redis
  end
  A->>G: GET /api/shipments/id
  G->>S: cache then DB
  S-->>A: CONFIRMED or REJECTED
```

Compensation is explicit: `REJECTED` is a first-class status, not a silent delete. Details: [ADR 0002](docs/adr/0002-choreography-saga.md), [ADR 0001](docs/adr/0001-outbox.md).

---

## Resilience

```mermaid
flowchart LR
  subgraph http [HTTP]
    Retry[Gateway_retry_GET_5xx]
    FeignCb[Feign_plus_Resilience4j]
  end

  subgraph events [Events]
    Outbox2[Outbox_survives_crash]
    Idem[Idempotent_consumers]
  end

  subgraph cache [Cache]
    Redis2[Redis_down_falls_back_to_DB]
  end
```

- Gateway retries **GET** on 5xx only (no retry on booking POST).
- If Redis is absent, `InMemoryTrackingCache` / DB read still serves tracking.
- Duplicate Kafka deliveries: finance ignores known `eventId`; shipment ignores non-`PENDING`.

---

## Observability

```mermaid
flowchart LR
  App[Services]
  App -->|OTLP| Collector[OTel_Collector]
  App -->|JSON_stdout| Elk[ELK_or_Filebeat]
  App -->|Actuator| Prom[Prometheus]
  Collector --> Backend[Trace_backend]
```

Correlation: `X-Correlation-Id` at the gateway; event `correlationId` on Kafka payloads.

---

## Bounded contexts

| Service | Responsibility | Sync | Async |
| --- | --- | --- | --- |
| `gateway-service` | Auth stub, routing, GET retries | HTTP | — |
| `customer-service` | Agent/customer master data | REST | — |
| `shipment-service` | Book + track | Feign to customer | Saga participant |
| `finance-service` | Credit reservation | REST snapshot | Saga participant |

## Quick start

```bash
mvn -q test
mvn -q -DskipTests package
docker compose up --build
```

Seeded customers: `C-ACME` (limit 10000), `C-POOR` (limit 100).

```bash
curl -s -X POST http://localhost:8080/api/shipments \
  -H 'Authorization: Bearer demo-token' \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"C-ACME","origin":"DXB","destination":"LHR","amount":500}'
```

`GET /api/shipments/{id}` with the same Bearer header: `PENDING` → `CONFIRMED` or `REJECTED`.

## Design decisions

[docs/adr](docs/adr) — outbox vs Debezium, choreography vs orchestrator, DNS vs Eureka, Feign vs Kafka.

## License

Apache-2.0
