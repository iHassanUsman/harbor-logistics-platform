# ADR 0003 — No Eureka: discovery is the platform

## Status
Accepted

## Context
On EC2 fleets, client-side Eureka (or Consul) was the right discovery model: instances came and went without a load balancer per service. Kubernetes (and Docker Compose) already provide stable DNS names and endpoints.

## Decision
- Local: `http://customer:8081` via Compose DNS.
- Cluster: Kubernetes Services. Feign URLs come from env (`CUSTOMER_URI`).
- Edge: Spring Cloud Gateway, not a second discovery client.

## Consequences
- Fewer moving parts than Netflix OSS on K8s.
- Documented so a resume line "implemented discovery" maps to the *reason* we later removed it.
