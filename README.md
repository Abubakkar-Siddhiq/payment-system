# Payment System

A distributed payment processing system built with Spring Boot microservices, designed to simulate the infrastructure layer behind real-world payment platforms like Razorpay or PayPal.

---

## Architecture Overview

```
Client
  ↓
API Gateway (Port 8080)
  → JWT Authentication
  → Role-based Access Control
  → Request Routing + Rate Limiting
  ↓
┌─────────────────────────────────────────────────┐
│  Auth Service     Payment Service               │
│  (Port 8081)      (Port 8082)                   │
│  Register/Login   Accounts, Transactions        │
│       ↓                ↓                        │
│   User DB         PostgreSQL + Redis            │
└─────────────────────────────────────────────────┘
         ↓ Kafka Event Bus
┌─────────────────────────────────────┐
│  Notification Service  Fraud Service│
│  (Port 8083)           (Port 8084)  │
│  Payment Alerts        Rules Engine │
│                        Fraud DB     │
└─────────────────────────────────────┘
```

---

## Services

### API Gateway
- JWT validation on every request
- Role-based routing — ADMIN routes protected
- Injects `X-User-Id` and `X-User-Role` headers downstream
- Rate limiting

### Auth Service
- User registration and login
- JWT token issuance with role claims
- Publishes `user-registered` event to Kafka
- BCrypt password hashing

### Payment Service
- Double-entry ledger for every transaction
- Idempotency using Redis — prevents duplicate payments
- Pessimistic DB locking — prevents double spending
- Account lifecycle: create, deposit, transfer
- Consumes `user-registered` events to auto-create accounts
- Consumes `fraud-alert` events to reverse fraudulent transactions and freeze accounts
- Publishes payment events to Kafka

### Notification Service
- Consumes payment events from Kafka
- Logs payment confirmations (extensible to email/SMS)

### Fraud Detection Service
- Rule-based fraud engine
- Flags high value transactions (≥ ₹10,000)
- Flags round number transactions
- Flags self-transfers
- Saves fraud alerts to dedicated DB
- Publishes fraud alerts to trigger payment reversal via Saga pattern

---

## Key Design Decisions

### Why Kafka over RabbitMQ?
Multiple services consume the same payment event independently. Kafka's consumer group model and message retention ensure no events are lost even if a service goes down temporarily.

### Why Redis for Idempotency?
Fast key-value lookups with TTL support. Idempotency keys expire after 24 hours automatically. A payment request sent twice returns the same response without processing twice.

### Why Pessimistic Locking?
Concurrent payment requests to the same account could cause double spending. Pessimistic locking via `@Lock(LockModeType.PESSIMISTIC_WRITE)` ensures one transaction processes at a time per account.

### Why Separate Databases per Service?
Each service owns its data. Payment Service cannot directly query Fraud Service's DB. Services communicate via events — this is the microservices data isolation principle.

### Why Double-Entry Ledger?
Every transaction creates a DEBIT for the sender and CREDIT for the receiver. The books always balance. No transaction is ever deleted — only reversed via compensating transactions.

### Saga Pattern for Fraud Reversal
When Fraud Service detects a high-risk transaction it publishes a fraud alert. Payment Service consumes it and executes a compensating transaction — crediting the sender back, debiting the receiver, and freezing the sender account. Full audit trail preserved.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.x |
| API Gateway | Spring Cloud Gateway |
| Messaging | Apache Kafka (KRaft mode) |
| Cache / Idempotency | Redis |
| Database | PostgreSQL |
| Auth | Spring Security + JWT (jjwt) |
| Containerization | Docker + Docker Compose |
| Mapping | MapStruct |
| Boilerplate | Lombok |

---

## Microservices Patterns Used

- **API Gateway Pattern** — single entry point for all clients
- **Database per Service** — each service owns its data
- **Event-Driven Architecture** — services communicate via Kafka
- **Idempotent Consumer** — Redis prevents duplicate processing
- **Saga Pattern** — distributed transaction management for fraud reversal
- **Pessimistic Locking** — prevents concurrent double spending

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

This starts: PostgreSQL (Payment), PostgreSQL (Fraud), Redis, Kafka broker.

**3. Start services in order**
```
1. Auth Service       → port 8081
2. Payment Service    → port 8082
3. Notification Service → port 8083
4. Fraud Service      → port 8084
5. API Gateway        → port 8080
```

---

## API Reference

### Auth
```
POST /api/auth/register    → Register new user
POST /api/auth/login       → Login, returns JWT token
```

### Accounts
```
POST   /api/accounts              → Create account
GET    /api/accounts/{id}         → Get account + balance
POST   /api/accounts/{id}/deposit → Deposit funds
```

### Payments
```
POST /api/payments    → Process payment between accounts
```
Requires header: `Idempotency-Key: <uuid>`

### Fraud (Admin only)
```
GET /api/fraud/alerts              → List all fraud alerts
GET /api/fraud/alerts/{id}         → Get specific alert
PUT /api/fraud/alerts/{id}/approve → Legitimate transaction, unfreeze account
PUT /api/fraud/alerts/{id}/reject  → Confirmed fraud, reverse transaction
```

---

## Example Payment Flow

```
1. Register user         POST /api/auth/register
2. Login                 POST /api/auth/login → get JWT
3. Deposit funds         POST /api/accounts/{id}/deposit
4. Send payment          POST /api/payments (with Idempotency-Key header)
5. Notification Service logs payment confirmation
6. Fraud Service analyzes transaction
7. If flagged → account frozen, transaction reversed automatically
```

---

## Environment Variables

```
JWT_SECRET=your-base64-encoded-secret
POSTGRES_PASSWORD=changeme
```

---

## What's Next

- Outbox Pattern — guaranteed Kafka delivery on service crash
- Circuit Breaker — Resilience4j for fault tolerance
- Distributed Tracing — Micrometer + Zipkin
- Swagger/OpenAPI documentation
- Containerize all services in Docker Compose