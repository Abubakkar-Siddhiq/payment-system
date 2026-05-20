<a id="readme-top"></a>

[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![LinkedIn][linkedin-shield]][linkedin-url]

<br />
<div align="center">
  <h3 align="center">💸💸💸</h3>
  <h3 align="center">Payment System</h3>

  <p align="center">
    Production-grade P2P payment infrastructure with distributed fraud detection and event-driven architecture
    <br />
    <a href="https://github.com/abubakkar-siddhiq/payment-system"><strong>Explore the code »</strong></a>
    <br />
    <br />
    <a href="https://medium.com/@Abubakkar-Siddhiq/how-i-built-a-distributed-payment-system-from-scratch-and-what-it-taught-me-9086c00ebca4">Read the Build Journal</a>
    &middot;
    <a href="https://github.com/abubakkar-siddhiq/payment-system/issues">Report Bug</a>
    &middot;
    <a href="https://github.com/abubakkar-siddhiq/payment-system/issues">Request Feature</a>
  </p>
</div>

---

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a></li>
    <li><a href="#architecture">Architecture</a></li>
    <li><a href="#services">Services</a></li>
    <li><a href="#key-design-decisions">Key Design Decisions</a></li>
    <li><a href="#microservices-patterns">Microservices Patterns</a></li>
    <li><a href="#security">Security</a></li>
    <li><a href="#built-with">Built With</a></li>
    <li><a href="#getting-started">Getting Started</a></li>
    <li><a href="#api-reference">API Reference</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

---

<!-- ABOUT THE PROJECT -->
## About The Project

A distributed payment processing system built with Spring Boot microservices — modeled after the infrastructure layer that powers platforms like Razorpay or PayPal. Handles peer-to-peer money movement with fraud detection, event-driven architecture, and production-grade reliability patterns.

Built to answer one question: *what actually happens when you tap "Pay"?*

Key capabilities:
- Never processes the same payment twice — Redis idempotency
- Prevents double spending — Pessimistic DB locking
- Zero message loss on crash — Outbox Pattern
- Automatic fraud reversal — Saga Pattern
- Real-time fraud detection — Rule-based engine with admin review queue

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- ARCHITECTURE -->
## Architecture

```
                                        Client
                                           │
                                    ┌──────▼─────┐
                                    │ API Gateway│  JWT Auth · Rate Limiting
                                    │  Port 8080 │  Role-based Routing
                                    └─────┬──────┘
                                          │
                          ┌───────────────┼───────────────┐
                          │               │               │
                    ┌─────▼──────┐  ┌─────▼──────┐  ┌────▼───────┐
                    │    Auth    │  │  Payment   │  │   Fraud    │
                    │  Service   │  │  Service   │  │  Service   │
                    │  Port 8081 │  │  Port 8082 │  │  Port 8083 │
                    └─────┬──────┘  └─────┬──────┘  └────┬───────┘
                          │               │               │
                       Auth DB       Payment DB       Fraud DB
                      (Postgres)     (Postgres)      (Postgres)
                                          │
                                     Redis Cache
                                   (Idempotency +
                                    Rate Limiting)
                
                                ────── Kafka Event Bus ──────
                
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
                
                         ╔════════════════════════════════════╗
                         ║      Docker Internal Network       ║
                         ║  Only port 8080 exposed externally ║
                         ╚════════════════════════════════════╝
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- SERVICES -->
## Services

### API Gateway (8080)
- JWT validation on every request — no token, no entry
- Role-based access control — `/fraud/**` restricted to ADMIN
- Rate limiting per IP via Redis — 5 req/min on login, 10 req/min on payments
- Strips untrusted `X-User-Id` headers — injects verified identity downstream

### Auth Service (8081)
- User registration and login
- JWT issuance with userId as subject and role as claim
- BCrypt password hashing
- Publishes `user-registered` event on registration

### Payment Service (8082)
- Double-entry ledger — every transaction creates DEBIT and CREDIT entries
- Idempotency via Redis — duplicate requests return same response without reprocessing
- Pessimistic DB locking — prevents double spending on concurrent requests
- Outbox Pattern — guaranteed Kafka delivery even on service crash
- Account ownership validation — users can only move money from their own accounts
- Account number abstraction — UUIDs stay internal, users interact via account numbers
- Consumes `user-registered` → auto-creates account on registration
- Consumes `fraud-alert` → reverses transaction and freezes account (Saga)
- Consumes `fraud-review` → unfreezes account on admin approval

### Notification Service (8084)
- Consumes payment events from Kafka
- Async payment confirmations — never blocks payment processing
- Extensible to email/SMS providers

### Fraud Detection Service (8083)
- Rule-based fraud engine — evaluates every transaction
- Rules: high value (≥ ₹10,000), round numbers, self-transfer, rapid successive payments
- Saves alerts to dedicated fraud DB with risk levels (LOW, MEDIUM, HIGH)
- Admin review queue — approve (unfreeze) or reject (keep frozen) via outbox pattern
- Publishes fraud alerts to trigger Saga compensation in Payment Service

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- KEY DESIGN DECISIONS -->
## Key Design Decisions

**Outbox Pattern** — Direct Kafka publishing risks message loss if service crashes mid-transaction. Events are written to an outbox table in the same DB transaction. A scheduler reads and publishes. Atomicity guaranteed.

**Idempotency with Redis** — Every payment request carries a client-generated `Idempotency-Key` header. Redis stores the key with TTL. Duplicate requests return the original response without reprocessing — prevents double charges on network retries.

**Pessimistic Locking** — Concurrent payments to the same account can cause double spending. `@Lock(PESSIMISTIC_WRITE)` on account fetches ensures one transaction processes at a time per account.

**Saga Pattern** — Fraud detection is async. Payment processes first, fraud analysis follows. When fraud is confirmed, a compensation event reverses the transaction and freezes the account. No records deleted — full audit trail preserved.

**Kafka over RabbitMQ** — Multiple services consume every payment event independently. Kafka's message retention means no events are lost if a service goes down — consumer catches up on restart.

**Database per Service** — Each service owns its data. No cross-service JPA relationships. Services communicate via events. Payment DB, Fraud DB, and Auth DB are completely independent.

**Account Number Abstraction** — Users share account numbers (e.g. `ABC123DEF456`). Internal UUIDs never surface in the API. Account number to UUID resolution happens server-side.

**JWT at Gateway Only** — Auth Service issues JWT. Gateway validates it using a shared secret — no Auth Service call needed per request. Downstream services trust the `X-User-Id` header injected by Gateway.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- MICROSERVICES PATTERNS -->
## Microservices Patterns

| Pattern | Implementation |
|---|---|
| API Gateway | Spring Cloud Gateway — single entry point |
| Database per Service | Auth DB, Payment DB, Fraud DB |
| Event-Driven Architecture | Kafka backbone across all services |
| Outbox Pattern | Payment Service + Fraud Service |
| Idempotent Consumer | Redis idempotency keys |
| Saga Pattern | Fraud reversal compensation flow |
| Pessimistic Locking | Account fetches during payment processing |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- SECURITY -->
## Security

- JWT validation at Gateway — no unauthenticated request reaches any service
- Header stripping — client-supplied `X-User-Id` removed, Gateway injects verified identity
- Account ownership validation — users can only deposit/transfer from their own accounts
- Role-based access — ADMIN role required for fraud endpoints
- Rate limiting per IP — brute force and spam protection on auth and payment routes
- BCrypt password hashing
- Internal network isolation — only port 8080 exposed externally via Docker
- No UUIDs in API surface — account numbers only
- IDOR prevention — account ownership checked on every write operation

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- BUILT WITH -->
## Built With

[![Java][Java-shield]][Java-url]
[![Spring][Spring-shield]][Spring-url]
[![Kafka][Kafka-shield]][Kafka-url]
[![Redis][Redis-shield]][Redis-url]
[![Postgres][Postgres-shield]][Postgres-url]
[![Docker][Docker-shield]][Docker-url]
[![Grafana][Grafana-shield]][Grafana-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- GETTING STARTED -->
## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 21
- Maven

### Installation

1. Clone the repository
   ```sh
   git clone https://github.com/abubakkar-siddhiq/payment-system.git
   cd payment-system
   ```

2. Start infrastructure
   ```sh
   docker-compose up -d
   ```
   Starts: PostgreSQL (x3), Redis, Kafka (KRaft), Grafana LGTM

3. Build each service
   ```sh
   cd payment-service && mvn clean package -DskipTests && cd ..
   cd authservice && mvn clean package -DskipTests && cd ..
   cd frauddetectionsystem && mvn clean package -DskipTests && cd ..
   cd notificationservice && mvn clean package -DskipTests && cd ..
   cd gateway && mvn clean package -DskipTests && cd ..
   ```

4. Start services in order
   ```
   1. Auth Service          → port 8081
   2. Payment Service       → port 8082
   3. Fraud Service         → port 8083
   4. Notification Service  → port 8084
   5. API Gateway           → port 8080
   ```

5. Create admin user
   ```sql
   UPDATE users SET role = 'ADMIN' WHERE email = 'admin@yourdomain.com';
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- API REFERENCE -->
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
PUT /api/v1/fraud/alerts/{id}/approve → Legitimate — unfreeze account
PUT /api/v1/fraud/alerts/{id}/reject  → Confirmed fraud — keep frozen
```

### Example Flow
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

7. Admin Review     → PUT /api/v1/fraud/alerts/{id}/approve
                    → Account unfrozen
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- ROADMAP -->
## Roadmap

- [x] Payment Service with idempotency and pessimistic locking
- [x] Outbox Pattern for guaranteed Kafka delivery
- [x] Fraud Detection with rule-based engine
- [x] Saga Pattern for fraud reversal
- [x] API Gateway with JWT and rate limiting
- [x] Distributed tracing with Grafana LGTM
- [x] Docker network isolation
- [ ] Service discovery via Eureka/Consul
- [ ] ML-based fraud scoring
- [ ] Kubernetes orchestration
- [ ] CI/CD pipeline
- [ ] Load testing with k6

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- CONTACT -->
## Contact

Abubakkar Siddhiq - [LinkedIn](https://linkedin.com/in/abubakkar-siddhiq)

Project Link: [https://github.com/abubakkar-siddhiq/payment-system](https://github.com/abubakkar-siddhiq/payment-system)

Build Journal: [medium blog](https://medium.com/@Abubakkar-Siddhiq/how-i-built-a-distributed-payment-system-from-scratch-and-what-it-taught-me-9086c00ebca4)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

* [Microservices.io — Pattern reference](https://microservices.io)
* [Baeldung — Spring Boot guides](https://baeldung.com)
* [Stripe Engineering Blog — Idempotency](https://stripe.com/blog/engineering)
* [ByteByteGo — Payment system design](https://bytebytego.com)
* [Confluent — Kafka fundamentals](https://confluent.io/learn/kafka)
* [Monzo Engineering Blog — Ledger design](https://monzo.com/blog/engineering)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- MARKDOWN LINKS & IMAGES -->
[forks-shield]: https://img.shields.io/github/forks/abubakkar-siddhiq/payment-system.svg?style=for-the-badge
[forks-url]: https://github.com/abubakkar-siddhiq/payment-system/network/members
[stars-shield]: https://img.shields.io/github/stars/abubakkar-siddhiq/payment-system.svg?style=for-the-badge
[stars-url]: https://github.com/abubakkar-siddhiq/payment-system/stargazers
[issues-shield]: https://img.shields.io/github/issues/abubakkar-siddhiq/payment-system.svg?style=for-the-badge
[issues-url]: https://github.com/abubakkar-siddhiq/payment-system/issues
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/abubakkar-siddhiq
[Java-shield]: https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://java.com
[Spring-shield]: https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white
[Spring-url]: https://spring.io/projects/spring-boot
[Kafka-shield]: https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white
[Kafka-url]: https://kafka.apache.org
[Redis-shield]: https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white
[Redis-url]: https://redis.io
[Postgres-shield]: https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white
[Postgres-url]: https://postgresql.org
[Docker-shield]: https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white
[Docker-url]: https://docker.com
[Grafana-shield]: https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white
[Grafana-url]: https://grafana.com
