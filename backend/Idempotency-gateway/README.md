# Idempotency-Gateway (The "Pay-Once" Protocol)
A fault-tolerant idempotency middleware layer designed for payment processing backends. Guarantees exactly-once transaction processing under high concurrency, automatic client retries, network timeouts, and race conditions.

## 1. Architecture Diagram 

### Overview
The gateway operates as an inline validation and concurrency control proxy before payment execution. It combines in-memory atomic locking for low-latency in-flight deduplication with a thread-safe ConcurrentHashMap for caching successful transaction records.

<img width="6625" height="5019" alt="deepseek_mermaid_20261124_91f036" src="https://github.com/user-attachments/assets/49baf40b-deb2-41cd-bbaa-c8a4cdb972b9" />


## 2. Key Design Decisions & Engineering Trade-offs
### 1. Canonical Payload Hashing (SHA-256)
**Problem:** Variations in JSON formatting (e.g., whitespace, key ordering) should not cause false negatives or bypass security checks. {"amount": 100, "currency": "GHS"} and {"currency": "GHS", "amount": 100} must be treated as identical.

**Solution:** The gateway deserializes the JSON payload using JsonMapper (Jackson 3), converts it to a TreeMap to alphabetically sort the keys, and serializes it back to a canonical string before computing a SHA-256 checksum.

## 3. The "Developer's Choice": Fintech Production Enhancements
In production payment systems, unbounded idempotency state presents serious operational risks (memory leaks and degraded lookup performance). We implemented the following critical mechanism:

**Feature:** Configurable Time-To-Live (TTL) Key Expiration (Default: 24 Hours)
**Rationale:** Retaining idempotency keys indefinitely causes unbounded memory growth. A retry window is a bounded period, not a permanent ledger.

**Implementation:** Each IdempotencyRecord is persisted with a LocalDateTime createdAt timestamp. When a request arrives, the system checks if the existing record is older than 24 hours (LocalDateTime.now().minusHours(24)). If expired, the record is evicted from the map, and the new request is processed as a fresh transaction.

## 4. Setup Instructions
Prerequisites: JDK 17 and Maven (or use the included mvnw wrapper). No database or external service is required.
git clone <your-forked-repo-url>
cd backend/Idempotency-gateway
Run the application (starts on port 8080)
./mvnw spring-boot:run
The server is ready when the console prints Tomcat started on port 8080.

## 5. API Documentation

### **`POST /process-payment`**
Headers:
Idempotency-Key: (Required) A unique string.
Content-Type: application/json
Request Body:

json
{"amount": 100, "currency": "GHS"}

Processes a payment transaction exactly once. Replays are handled seamlessly, and conflicts are strictly rejected to prevent fraud.
For example if we use Postman
#### **Request Headers**
| Header | Type | Required | Description |
| :--- | :--- | :---: | :--- |
| **`Idempotency-Key`** | `String` | **Yes** | A unique client-generated string (e.g., UUID) identifying the transaction. |
| **`Content-Type`** | `String` | **Yes** | Must be `application/json`. |

X-Cache-Hit	Returns true if this is a replayed response, indicating no new charge was made.
Response Scenarios
## 🟢 1. First Transaction (Happy Path)
Status: 200 OK (or 201 Created depending on server config)

Description: The payment was successfully processed and recorded in the system.

json
{
  "status": "Charged 100 GHS"
}
## 🔵 2. Duplicate Transaction (Replay)
Status: 200 OK

Headers: X-Cache-Hit: true

Description: The Idempotency-Key matched an existing record. The original response is replayed instantly without charging the customer again.

json
{
  "status": "Charged 100 GHS"
}
## 🔴 3. Fraud/Error Check (Same Key, Different Payload)
Status: 422 Unprocessable Entity

Description: The Idempotency-Key matches an existing record, but the request body has changed (e.g., someone tried to change the amount from 100 to 500).

json
{
  "error": "Idempotency key already used for a different request body."
}
## 🟡 4. Bad Request (Missing Header)
Status: 400 Bad Request

Description: The request was sent without the required Idempotency-Key header.

### cURL Examples
Simulate a successful transaction:
curl -i -X POST http://localhost:8080/process-payment \
  -H "Idempotency-Key: test-key-001" \
  -H "Content-Type: application/json" \
  -d '{"amount":100,"currency":"GHS"}'
Simulate a duplicate retry (Replay):
(Run the exact same command again. Note the X-Cache-Hit: true header and instant response).

### Simulate a conflict (Tamper attempt):
curl -i -X POST http://localhost:8080/process-payment \
  -H "Idempotency-Key: test-key-001" \
  -H "Content-Type: application/json" \
  -d '{"amount":500,"currency":"GHS"}'

