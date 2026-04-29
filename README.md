# Payment System

A distributed payment processing infrastructure built with Spring Boot microservices. Modeled after the engine that powers platforms like Razorpay or PayPal. Handles peer-to-peer money movement with fraud detection, event-driven architecture, and production-grade reliability patterns.

---

## Architecture

```
                                                       Client
                                                         │
                                                  ┌──────▼──────┐
                                                  │ API Gateway │  JWT Auth · Rate Limiting
                                                  │  Port 8080  │  Role-based Routing
                                                  └─────┬───────┘
                                                        │
                                        ┌───────────────┼───────────────┐
                                        │               │               │
                                  ┌─────▼──────┐  ┌─────▼──────┐  ┌─────▼──────┐
                                  │    Auth    │  │  Payment   │  │   Fraud    │
                                  │  Service   │  │  Service   │  │  Service   │
                                  │  Port 8081 │  │  Port 8082 │  │  Port 8083 │
                                  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
                                        │               │               │
                                     Auth DB       Payment DB       Fraud DB
                                    (Postgres)     (Postgres)      (Postgres)
                                                        │
                                                   Redis Cache
                                           (Idempotency + Rate Limiting)
                              
                              
                                      ──────────── Kafka Event Bus ────────────
                              
                                Auth Service     → user-registered-topic   → Payment Service
                                Payment Service  → payment-topic           → Fraud Service
                                Payment Service  → payment-topic           → Notification Service
                                Fraud Service    → fraud-alert-topic       → Payment Service
                                Fraud Service    → fraud-review-topic      → Payment Service
                              
                                                  ┌──────────────┐
                                                  │ Notification │
                                                  │   Service    │
                                                  │  Port 8084   │
                                                  └──────────────┘
                              
                                            ┌─────────────────────────┐
                                            │   Grafana LGTM Stack    │
                                            │  Traces · Logs · Metrics│
                                            └─────────────────────────┘
                              
                                       ╔═════════════════════════════════════╗
                                       ║      Docker Internal Network        ║
                                       ║  Only port 8080 exposed externally  ║
                                       ╚═════════════════════════════════════╝
```

---

## Services

### API Gateway (8080)
- JWT validation on every request. no token, no entry
- Role-based access control. `/fraud/**` restricted to ADMIN
- Rate limiting per IP via Redis. 5 req/min on login, 10 req/min on payments
- Strips untrusted `X-User-Id` headers. Injects verified identity downstream
- Request routing to all services

### Auth Service (8081)
- User registration and login
- JWT issuance with userId (UUID) as subject and role as claim
- BCrypt password hashing
- Publishes `user-registered-event` event on registration

### Payment Service (8082)
- Double-entry ledger - Every transaction creates DEBIT and CREDIT entries
- Idempotency via Redis - Duplicate requests return same response without reprocessing
- Pessimistic DB locking - Prevents double spending on concurrent requests
- Outbox Pattern - Guaranteed Kafka delivery even on service crash
- Account ownership validation - Users can only move money from their own accounts
- Account number abstraction - UUIDs stay internal, users interact via account numbers
- Consumes `user-registered-event` → auto-creates account on registration
- Consumes `fraud-alert-event` → freezes account upon High alert (Saga)
- Consumes `fraud-review-event` → unfreezes account if false positive else reverse transaction on admin approval

### Notification Service (8084)
- Consumes payment events from Kafka
- Async payment confirmations - never blocks payment processing
- Extensible to email/SMS providers

### Fraud Detection Service (8083)
- Rule-based fraud engine evaluates every transaction
- Rules: high value (≥ ₹10,000), round numbers, self-transfer, rapid successive payments
- Saves alerts to dedicated fraud DB with risk levels (LOW, MEDIUM, HIGH)
- Admin review queue approve (unfreeze) or reject (keep frozen) via outbox pattern
- Publishes fraud alerts to trigger Saga compensation in Payment Service

---

## Key Design Decisions

### Outbox Pattern
Direct Kafka publishing risks message loss if service crashes mid-transaction. Instead, events are written to an outbox table in the same DB transaction. A scheduler reads and publishes, guaranteeing delivery.

### Idempotency with Redis
Every payment request carries a client-generated `Idempotency-Key` header. Redis stores the key with TTL. Duplicate requests return the original response without reprocessing. Prevents double charges on network retries.

### Pessimistic Locking
Concurrent payments to/from the same account can cause double spending. `@Lock(PESSIMISTIC_WRITE)` on account fetches ensures one transaction processes at a time per account.

### Saga Pattern for Fraud Reversal
Fraud detection is Async. payment processes first, fraud analysis follows. When fraud is confirmed, Fraud Service publishes a compensation event. Payment Service reverses the transaction and freezes the account. Full audit trail preserved, no records deleted.

### Kafka over RabbitMQ
Multiple services consume every payment event independently (Notification + Fraud). Kafka's consumer group model makes this trivial. Message retention means no events are lost if a service goes down temporarily. Consumer catches up on restart.

### Database per Service
Each service owns its data. No cross-service JPA relationships. Services communicate via events. Payment DB, Fraud DB, and Auth DB are completely independent.

### Account Number Abstraction
Users share and interact with account numbers (e.g. `ABC123DEF456`). Internal UUIDs never surface in the API. Account number → UUID resolution happens server-side. Realistic and secure.

### JWT at Gateway Only
Auth Service issues JWT. Gateway validates it using the shared secret. No Auth Service call needed on every request. Downstream services trust the `X-User-Id` header injected by Gateway after validation.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.x |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Messaging | Apache Kafka (KRaft mode no Zookeeper) |
| Cache / Idempotency / Rate Limiting | Redis |
| Database | PostgreSQL (3 separate instances) |
| Authentication | Spring Security + JWT (jjwt 0.13) |
| Containerization | Docker + Docker Compose |
| Observability | Grafana LGTM (Traces, Logs, Metrics via OTLP) |
| Mapping | MapStruct |
| Boilerplate | Lombok |

---

## Microservices Patterns

| Pattern | Where |
|---|---|
| API Gateway | Spring Cloud Gateway - single entry point |
| Database per Service | Auth DB, Payment DB, Fraud DB |
| Event-Driven Architecture | Kafka backbone across all services |
| Outbox Pattern | Payment Service + Fraud Service |
| Idempotent Consumer | Redis idempotency keys |
| Saga Pattern | Fraud reversal compensation flow |
| Pessimistic Locking | Account fetches during payment processing |

---

## Security

- JWT validation at Gateway - no token reaches any service unauthenticated
- Header stripping - client-supplied `X-User-Id` removed before Gateway injects verified identity
- Account ownership validation - users can only deposit/transfer from their own accounts
- Role-based access - ADMIN role required for fraud endpoints
- Rate limiting per IP - brute force and spam protection on auth and payment routes
- BCrypt password hashing
- Internal network isolation - only port 8080 exposed externally via Docker
- No UUIDs in API surface - account numbers only

---

## Prerequisites

- Docker and Docker Compose
- Java 21
- Maven

---

## Running Locally

**1. Clone the repository**
```bash
git clone https://github.com/your-username/payment-system.git
cd payment-system
```

**2. Start infrastructure**
```bash
docker-compose up -d
```

Starts: PostgreSQL (x3), Redis, Kafka (KRaft), Grafana LGTM

**3. Start services in order**
```
1. Auth Service          → port 8081
2. Payment Service       → port 8082
3. Fraud Service         → port 8083
4. Notification Service  → port 8084
5. API Gateway           → port 8080
```

**4. Create admin user**
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@yourdomain.com';
```

---

## API Reference

### Auth
```
POST /api/v1/auth/register    → Register, returns JWT + userId
POST /api/v1/auth/login       → Login, returns JWT + userId
```

### Accounts
```
GET  /api/v1/accounts/me                        → My account info
GET  /api/v1/accounts/{accountNumber}/balance   → My balance
POST /api/v1/accounts/{accountNumber}/deposit   → Deposit funds
GET  /api/v1/accounts                           → List all (ADMIN only)
```

### Payments
```
POST /api/v1/payments    → Process payment between accounts
```
Required headers:
```
Authorization: Bearer <jwt>
Idempotency-Key: <uuid>
```
Body:
```json
{
  "senderAccountNumber": "ABC123DEF456",
  "receiverAccountNumber": "XYZ789GHI012",
  "amount": 500.00
}
```

### Fraud (ADMIN only)
```
GET /api/v1/fraud/alerts              → List all fraud alerts
GET /api/v1/fraud/alerts/{id}         → Get specific alert
PUT /api/v1/fraud/alerts/{id}/approve → Legitimate? unfreeze account
PUT /api/v1/fraud/alerts/{id}/reject  → Confirmed fraud? keep frozen
```

---

## Example Flow

```
1. Register         POST /api/v1/auth/register
                    → Account auto-created via Kafka event

2. Login            POST /api/v1/auth/login
                    → Receive JWT + account number

3. Deposit          POST /api/v1/accounts/{number}/deposit
                    → Fund your account

4. Pay              POST /api/v1/payments
                    → Money moves, ledger updated, Kafka event fired

5. Notification     → Notification Service logs confirmation

6. Fraud Check      → Fraud Service analyzes transaction
                    → If HIGH risk: account frozen, payment reversed
                    → Admin reviews via fraud endpoints

7. Admin Review     → PUT /api/v1/fraud/alerts/{id}/approve
                    → Account unfrozen
```

---

## Observability

- Grafana UI: `http://localhost:3000` (admin/admin)
- Traces in Tempo - Full request journey across services
- Logs in Loki - correlated with trace IDs
- Metrics in Mimir

---

## Known Improvements for Production

- Service discovery via Eureka/Consul replace hardcoded URIs
- Kubernetes orchestration
- Secrets management via Vault
- ML-based fraud scoring alongside rule engine
- Real email/SMS provider for Notification Service
- CI/CD pipeline
- Multi-region deployment
