# Setup and Running Guide

## Prerequisites

| Requirement | Version | Check Command |
|---|---|---|
| Java | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| MongoDB | 6+ | `mongod --version` |

---

## 1. Start MongoDB

MongoDB must be running locally before starting the application or running tests.

**Windows (as a service):**
```
net start MongoDB
```

**Windows (manual):**
```
mongod --dbpath C:\data\db
```

**macOS/Linux:**
```bash
brew services start mongodb-community   # macOS
sudo systemctl start mongod             # Linux
```

**Verify MongoDB is running:**
```bash
mongosh --eval "db.adminCommand('ping')"
# Expected: { ok: 1 }
```

---

## 2. Clone and Build

```bash
git clone https://github.com/nagvallabhaneni/url-shortener.git
cd url-shortener
mvn clean package -DskipTests
```

---

## 3. Run the Application

```bash
mvn spring-boot:run
```

Or with the built JAR:
```bash
java -jar target/url-shortener-*.jar
```

**Environment variable overrides (optional):**

| Variable | Default | Description |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/url_shortener` | MongoDB connection string |
| `SERVER_PORT` | `8080` | HTTP port |
| `BASE_URL` | `http://localhost:8080/s` | Prefix for shortened URLs |

**Example with custom MongoDB:**
```bash
MONGODB_URI=mongodb://user:pass@mongo-host:27017/mydb mvn spring-boot:run
```

**Verify the application started:**
```bash
curl http://localhost:8080/api/v1/urls/health
# Expected: Service is running
```

---

## 4. API Reference

### Shorten a URL

```bash
curl -X POST http://localhost:8080/api/v1/urls/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com/very/long/path", "userId": "user123"}'
```

**Response (201 Created):**
```json
{
  "shortCode": "abc123",
  "shortUrl": "http://localhost:8080/s/abc123",
  "originalUrl": "https://example.com/very/long/path",
  "createdTimestamp": 1705756800000
}
```

### Redirect (Browser)

Open in browser:
```
http://localhost:8080/abc123
```
→ Redirects to `https://example.com/very/long/path` (HTTP 302)

Or with curl (follow redirect):
```bash
curl -L http://localhost:8080/abc123
```

### Get Original URL as JSON (No Redirect)

```bash
curl http://localhost:8080/api/v1/urls/abc123
```

**Response (200 OK):**
```json
{
  "originalUrl": "https://example.com/very/long/path"
}
```

**Response (404 Not Found):**
```json
{
  "error": "URL not found for short code: abc123"
}
```

### Health Check

```bash
curl http://localhost:8080/api/v1/urls/health
# Response: Service is running
```

---

## 4a. Run the Agentic Orchestrator

The orchestrator is part of the same Spring Boot app (no separate process) — once `mvn
spring-boot:run` is up (step 3) and MongoDB is running (step 1), drive it via
`/api/v1/orchestration/**`. See `AGENT_DESIGN.md` for how it works internally and
`SCENARIOS.md` for full worked examples.

**Start a run:**
```bash
curl -X POST http://localhost:8080/api/v1/orchestration/runs \
  -H "Content-Type: application/json" \
  -d '{"requirementText":"Build a URL shortener with core APIs, analytics, and reliability features.","scenarioType":"GREENFIELD"}'
```
Response includes `"id"` (the runId) and per-stage results; it stops at the first thing
needing you (a `WAITING_APPROVAL` gate) or runs straight through to `COMPLETED`.

**Check current state / audit trail:**
```bash
curl http://localhost:8080/api/v1/orchestration/runs/{runId}
```

**Approve or reject a pending gate** (e.g. `RELEASE_READINESS`, `DATA_MIGRATION_REVIEW`,
or `IMPLEMENTATION` if a guardrail was tripped):
```bash
curl -X POST http://localhost:8080/api/v1/orchestration/runs/{runId}/stages/RELEASE_READINESS/approve \
  -H "Content-Type: application/json" -d '{"approver":"you"}'

curl -X POST http://localhost:8080/api/v1/orchestration/runs/{runId}/stages/RELEASE_READINESS/reject \
  -H "Content-Type: application/json" -d '{"approver":"you","reason":"docs incomplete"}'
```

**Stop a run (safe-stop):**
```bash
curl -X POST http://localhost:8080/api/v1/orchestration/runs/{runId}/stop
```

**Aggregate reliability metrics:**
```bash
curl http://localhost:8080/api/v1/orchestration/metrics
```

**Try the other two scenario types:**
```bash
# Brownfield — triggers dynamic re-plan + DATA_MIGRATION_REVIEW gate
curl -X POST http://localhost:8080/api/v1/orchestration/runs -H "Content-Type: application/json" \
  -d '{"requirementText":"Add a new field to the schema to capture referer analytics on redirect","scenarioType":"BROWNFIELD"}'

# Ambiguous — REQUIREMENTS stage records the resolved assumption
curl -X POST http://localhost:8080/api/v1/orchestration/runs -H "Content-Type: application/json" \
  -d '{"requirementText":"Add caching support to make it faster","scenarioType":"AMBIGUOUS"}'
```

**Force a simulated failure** (to see retry/fallback/rollback without flaky infra):
```bash
curl -X POST http://localhost:8080/api/v1/orchestration/runs -H "Content-Type: application/json" \
  -d '{"requirementText":"Build a URL shortener","scenarioType":"GREENFIELD","simulatedFailures":{"UNIT_TESTING":2}}'
```

**Without curl:** every endpoint is also listed in Swagger UI (step 5 below) under the
`orchestration-controller` tag — you can call them from the browser.

**Run just the orchestrator tests (no MongoDB needed — they use mocks):**
```bash
mvn -q test "-Dtest=OrchestrationEngineTest,OrchestrationControllerIntegrationTest"
```

---

## 5. API Documentation (Swagger UI)

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

---

## 6. Run Tests

### All tests (unit + integration):

```bash
mvn clean test
```

MongoDB must be running. Tests use the `url_shortener_test` database (isolated from dev).

### Tests with coverage report:

```bash
mvn clean verify
```

Coverage report: `target/site/jacoco/index.html`

Coverage gate: Build fails if any package drops below 70% line coverage.

### Run only unit tests (no MongoDB needed):

```bash
mvn clean test -pl src/test/java/com/urlshortener/unit
```

Or by test class pattern:
```bash
mvn clean test -Dtest="**/unit/**"
```

---

## 7. View Analytics Data

Query `redirect_logs` in MongoDB to see analytics:

```javascript
// mongosh
use: url_shortener

// All redirects for a short code
db.redirect_logs.find({ shortCode: "abc123" }).sort({ redirectedAt: -1 })

// Top 10 most-clicked URLs
db.url_mappings.find().sort({ accessCount: -1 }).limit(10)

// Average response time
db.redirect_logs.aggregate([
  { $group: { _id: null, avgMs: { $avg: "$responseTimeMs" } } }
])
```

---

## 8. Actuator Endpoints (Observability)

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Application health + MongoDB status |
| `GET /actuator/metrics` | All available metrics |
| `GET /actuator/metrics/http.server.requests` | HTTP request latency histogram |
| `GET /actuator/prometheus` | Prometheus-format metrics |

---

## 9. Logs

Application logs are written to:
- **Console**: `stdout` (INFO level)
- **File**: `logs/url-shortener.log` (DEBUG level, 10MB rotation, 30-day retention)

**Sample log entries:**
```
2024-01-20 14:00:00 - URL shortened successfully. ShortCode: abc123, OriginalUrl: https://example.com
2024-01-20 14:00:01 - Redirect request for short code: abc123
2024-01-20 14:00:01 - Redirect processed. ShortCode: abc123, AccessCount: 1, ResponseTime: 12ms
```

---

## 10. Project Structure

```
url-shortener/
├── src/
│   ├── main/java/com/urlshortener/
│   │   ├── UrlShortenerApplication.java
│   │   ├── config/          (MongoConfig, OpenApiConfig)
│   │   ├── controller/      (RedirectController, UrlController)
│   │   ├── dto/             (ShortenUrlRequest, ShortenUrlResponse)
│   │   ├── exception/       (UrlException)
│   │   ├── model/           (UrlMapping, RedirectLog)
│   │   ├── repository/      (UrlRepository, RedirectLogRepository)
│   │   ├── service/         (UrlService)
│   │   ├── util/            (HashGenerator, RequestInfoExtractor)
│   │   └── validator/       (UrlValidator)
│   ├── main/resources/
│   │   └── application.yml
│   └── test/java/com/urlshortener/
│       ├── UrlShortenerApplicationTest.java
│       ├── integration/     (UrlControllerTest, AdditionalTest, ErrorHandlingTest, RepositoryIntegrationTest)
│       └── unit/            (UrlServiceTest, UrlServiceRedirectTest, HashGeneratorTest, ...)
├── ARCHITECTURE.md           ← System design and decisions
├── ORCHESTRATION.md          ← Agentic orchestration model
├── SCENARIOS.md              ← Three engineering scenarios
├── ENGINEERING_SUMMARY.md    ← Final engineering summary
├── SETUP.md                  ← This file
├── pom.xml
└── logs/
    └── url-shortener.log
```
