# Architecture Overview — URL Shortener Service

## 1. System Purpose

A production-grade URL shortening service built to demonstrate agentic software engineering:
end-to-end SDLC execution with autonomous task decomposition, multi-step orchestration, and
human-governed quality control.

---

## 2. Component Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                        CLIENT (Browser / API Consumer)           │
└────────────────────────┬─────────────────────────────────────────┘
                         │  HTTP
          ┌──────────────▼──────────────┐
          │         Spring Boot App      │
          │         (Port 8080)          │
          │                              │
          │  ┌─────────────────────────┐ │
          │  │   RedirectController    │ │  GET /{shortCode}
          │  │  (root-level routing)   │ │  → 302 Redirect + Analytics
          │  └────────────┬────────────┘ │
          │               │              │
          │  ┌────────────▼────────────┐ │
          │  │     UrlController       │ │  POST /api/v1/urls/shorten
          │  │  (API v1 namespace)     │ │  GET  /api/v1/urls/{code}
          │  └────────────┬────────────┘ │  GET  /api/v1/urls/health
          │               │              │
          │  ┌────────────▼────────────┐ │
          │  │       UrlService        │ │  shortenUrl()
          │  │   (Business Logic)      │ │  getOriginalUrl()
          │  │                         │ │  handleRedirect()
          │  └──────┬──────────┬───────┘ │
          │         │          │          │
          │  ┌──────▼──┐  ┌───▼────────┐ │
          │  │UrlRepo  │  │RedirectLog │ │
          │  │(MongoDB)│  │Repository  │ │
          │  └──────┬──┘  └───┬────────┘ │
          │         │          │          │
          └─────────┼──────────┼──────────┘
                    │          │
          ┌─────────▼──────────▼──────────┐
          │          MongoDB               │
          │   Collection: url_mappings     │
          │   Collection: redirect_logs    │
          └────────────────────────────────┘
```

---

## 3. Technology Stack

| Layer | Technology | Version | Reason |
|---|---|---|---|
| Runtime | Java | 17 LTS | Long-term support, modern language features |
| Framework | Spring Boot | 3.3.0 | Production-ready auto-configuration |
| Database | MongoDB | 6+ | Schema-flexible document store; short codes as natural _id keys |
| API Docs | SpringDoc OpenAPI | 2.x | Auto-generates Swagger UI from annotations |
| Testing | JUnit 5 + Mockito | 5.x | Industry standard; supports parallel test execution |
| Coverage | JaCoCo | 0.8.x | Enforces 70% minimum line coverage at build time |
| Build | Maven | 3.8+ | Dependency management and lifecycle control |
| Boilerplate | Lombok | 1.18.x | Reduces DTO/model noise without affecting runtime |

---

## 4. Data Model

### Collection: `url_mappings`

```json
{
  "_id":            "abc123",
  "originalUrl":    "https://example.com/very/long/path",
  "userId":         "user-123",
  "createdAt":      "2024-01-15T10:30:00",
  "expiresAt":      null,
  "accessCount":    42,
  "lastAccessedAt": "2024-01-20T14:00:00"
}
```

- `_id` = shortCode (MongoDB native key — no separate index needed)
- `accessCount` incremented on every redirect (optimistic, non-atomic; acceptable for analytics approximation)
- `userId` enables per-user analytics and quota enforcement

### Collection: `redirect_logs`

```json
{
  "_id":          "ObjectId(...)",
  "shortCode":    "abc123",
  "userId":       "user-123",
  "ipAddress":    "203.0.113.42",
  "userAgent":    "Mozilla/5.0 ...",
  "referer":      "https://google.com",
  "redirectedAt": "2024-01-20T14:00:00",
  "responseTimeMs": 12
}
```

- Append-only log; no updates ever
- Decoupled from `url_mappings` — logging failure never blocks a redirect

---

## 5. API Surface

| Method | Path | Description | Response |
|---|---|---|---|
| `POST` | `/api/v1/urls/shorten` | Shorten a URL | `201` + `{shortCode, shortUrl, originalUrl, createdTimestamp}` |
| `GET` | `/{shortCode}` | Redirect to original URL | `302` Location header + analytics logged |
| `GET` | `/api/v1/urls/{shortCode}` | Get original URL as JSON | `200` + `{originalUrl}` or `404` + `{error}` |
| `GET` | `/api/v1/urls/health` | Service health check | `200` "Service is running" |
| `GET` | `/v3/api-docs` | OpenAPI spec (JSON) | `200` |
| `GET` | `/swagger-ui.html` | Swagger UI | `200` |

---

## 6. Key Design Decisions

### D1 — MongoDB as primary store (not SQL)
**Decision**: Use MongoDB with `shortCode` as the document `_id`.  
**Rationale**: Short codes are natural unique keys; document lookup by `_id` in MongoDB is O(1) with no index overhead. A relational DB would need an indexed `shortCode` column on top of a primary key, adding write overhead.  
**Trade-off**: Atomic `accessCount` increment requires `$inc` operator (not used here — see D4).

### D2 — Base62 hash for short codes (not UUID)
**Decision**: `HashGenerator` produces 6-character alphanumeric codes from SHA-256 of random bytes.  
**Rationale**: 6 chars of Base62 = 62^6 ≈ 56 billion combinations; human-typeable; URL-safe without encoding.  
**Trade-off**: Collision probability exists at scale; production would add collision detection loop.

### D3 — Separate `RedirectController` at root path
**Decision**: Redirect endpoint lives in a dedicated controller at `/{shortCode}` (root), not under `/api/v1/urls/`.  
**Rationale**: A URL shortener's redirect must be at root (e.g., `short.ly/abc123`), not at `/api/v1/urls/abc123`. Mixing API namespace with redirect namespace would make shortened URLs impractical.  
**Trade-off**: Two controllers with shared `UrlService` — adds one bean but improves routing clarity.

### D4 — Optimistic (non-atomic) access count increment
**Decision**: `getAccessCount() + 1` then `save()` rather than MongoDB `$inc`.  
**Rationale**: Simplicity for prototype scope. Analytics approximation is acceptable; exact counts are not a hard requirement.  
**Trade-off**: Under high concurrency, counts may be slightly undercounted (lost update). Production fix: use `MongoTemplate.updateFirst()` with `$inc`.

### D5 — Analytics logging is error-resilient (fire-and-forget)
**Decision**: `logRedirect()` wraps `redirectLogRepository.save()` in try/catch; exceptions are swallowed.  
**Rationale**: A failed analytics write must never cause a user-facing redirect failure. User experience (redirect) > analytics completeness.  
**Trade-off**: Under MongoDB failure, analytics data is silently lost. Production fix: use a dead-letter queue or async event bus.

### D6 — Redis dropped in favour of MongoDB-only
**Decision**: Initially considered Redis for caching short code lookups; removed entirely.  
**Rationale**: MongoDB `_id` lookups are already O(1) with in-memory WiredTiger cache. Adding Redis introduces operational complexity (a second data store, cache invalidation, TTL management) with marginal latency gain for this scale.  
**Trade-off**: At very high throughput (>100K RPS), a Redis cache layer would reduce MongoDB load.

### D7 — No URL expiry enforcement at redirect time
**Decision**: `expiresAt` field is modeled but not enforced during redirect.  
**Rationale**: Scope control — expiry TTL logic adds complexity (scheduled cleanup job, index on `expiresAt`). Field is modeled to show forward compatibility.  
**Trade-off**: Expired URLs are still redirectable. Production fix: add `@Scheduled` purge job and expiry check in `handleRedirect()`.

---

## 7. Security Considerations

| Concern | Current Handling | Production Enhancement |
|---|---|---|
| URL injection | `UrlValidator` rejects non-HTTP/HTTPS schemes | Add blocklist for known malware domains |
| IP logging | `RequestInfoExtractor` reads `X-Forwarded-For` | Mask last octet for GDPR compliance |
| Short code enumeration | Random Base62; not sequential | Add rate limiting on redirect endpoint |
| Open redirect abuse | Validates URL at creation time | Add domain allowlist/blocklist |
| Authentication | None (prototype) | Add JWT/OAuth2 for `/shorten` endpoint |

---

## 8. Observability

- **Structured logging**: Every request/response logged via SLF4J with correlation fields (`shortCode`, `userId`, `responseTimeMs`)
- **Log file**: `logs/url-shortener.log` with 10MB rotation, 30-day retention
- **Actuator endpoints**: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` exposed
- **Response time tracking**: `responseTimeMs` captured in `RedirectLog` for P95/P99 analysis
- **Analytics**: `redirect_logs` collection queryable for funnel analysis, top URLs, geographic distribution
