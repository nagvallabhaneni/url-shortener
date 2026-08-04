# Three Engineering Scenarios

---

## Scenario 1 — Greenfield: Build URL Shortener From Scratch

### 1.1 Requirement as Received

> "Build a URL shortener service with core APIs, analytics, and reliability features."

### 1.2 Requirement Interpretation and Normalization

**Ambiguities identified:**

| Ambiguity | Question Asked | Resolution |
|---|---|---|
| "Core APIs" — what exactly? | Does "core" mean shorten + redirect only, or also list/delete/stats? | Normalized to: POST shorten, GET redirect, GET JSON lookup, GET health |
| "Analytics" — what data to capture? | Click count only, or full request metadata (IP, UA, referer, time)? | Full request metadata stored in `redirect_logs` collection |
| "Reliability features" — what scope? | HA, retries, circuit breakers, or just clean error handling? | Scoped to: input validation, error-resilient logging, structured exception handling |
| Short code format? | UUID? Sequential integer? Custom Base62? | Base62, 6 chars; human-readable, URL-safe, no encoding needed |
| Storage technology? | SQL, MongoDB, Redis? | MongoDB (document store; shortCode = natural _id; schema flexibility) |
| User identity? | Anonymous? Authenticated? | `userId` field accepted but not validated (prototype scope) |

**Normalized engineering problem:**
> Build a Spring Boot REST service that: (1) accepts a long URL + userId and returns a short code;
> (2) redirects browsers from `/{shortCode}` to the original URL with 302; (3) logs each redirect
> request with IP, UA, referer, and response time to MongoDB; (4) validates URLs at input time;
> (5) returns structured JSON errors; (6) is testable with ≥70% coverage.

### 1.3 Task Decomposition with Dependencies

```
[T1] Define data models (UrlMapping, RedirectLog)
  └─► [T2] Implement MongoDB repositories (UrlRepository, RedirectLogRepository)
        └─► [T3] Implement UrlService (shortenUrl, getOriginalUrl, handleRedirect)
              ├─► [T4a] Implement UrlController (POST /shorten, GET /json, GET /health)
              └─► [T4b] Implement RedirectController (GET /{shortCode})
                    └─► [T5] Implement RequestInfoExtractor (IP, UA, referer parsing)
                          └─► [T6] Write unit tests (service, validator, hash, model)
                                └─► [T7] Write integration tests (controller, repository)
                                      └─► [T8] Verify coverage ≥ 70% (JaCoCo)
                                            └─► [T9] Generate API documentation (Swagger)
```

**Parallel opportunities:**
- `[T4a]` and `[T4b]` can be developed simultaneously (different controllers)
- `[T6]` unit tests and `[T7]` integration tests can be written in parallel
- `[T9]` Swagger is auto-generated at startup; no explicit task needed

### 1.4 Orchestration Path (What Actually Happened)

| Step | Action | Gate/Checkpoint | Outcome |
|---|---|---|---|
| 1 | Agent interpreted requirements, proposed Spring Boot + MongoDB architecture | **CP-1**: Human confirms storage choice | Approved |
| 2 | Agent designed data models (`UrlMapping`, `RedirectLog`) with `shortCode` as `_id` | Auto-proceed (no human risk) | Models created |
| 3 | Agent implemented `UrlService` with shorten + redirect logic | Auto-proceed | Service implemented |
| 4 | Agent implemented `UrlController` + `RedirectController` | Auto-proceed | Controllers created |
| 5 | Agent added Swagger/OpenAPI config | Auto-proceed | API docs live at `/swagger-ui.html` |
| 6 | Agent wrote 97+ tests across 11 test files | Auto-proceed | Tests created |
| 7 | Tests failed — Redis dependency issue | **Re-plan**: Human: "No Redis, use MongoDB only" | Redis removed |
| 8 | Tests failed — embedded MongoDB rejected | **CP-2**: Human: "Use local MongoDB" | `application-test.yml` created |
| 9 | Redirect tests failing (404 instead of 302) | Agent diagnosed: wrong endpoint paths | `RedirectController` separated |
| 10 | Coverage failures (exception + root packages) | Agent proposed JaCoCo exclusion | **CP-5**: pom.xml updated |
| 11 | All tests green, coverage ≥70% | **Release Gate**: Human review | ✅ Greenfield complete |

### 1.5 Validation

| Validation Type | Method | Result |
|---|---|---|
| Unit tests | 60+ unit tests across service, model, util, validator | ✅ All pass |
| Integration tests | 37+ integration tests with MockMvc + MockBean | ✅ All pass |
| Coverage gate | JaCoCo enforces 70% minimum at `mvn verify` | ✅ ≥70% |
| API validation | Manual Swagger UI testing at `/swagger-ui.html` | ✅ All endpoints respond |
| Input validation | `@Valid` + `UrlValidator` rejects invalid URLs | ✅ Returns 400 |
| Error handling | `UrlException` → structured `{"error": "..."}` JSON | ✅ Consistent |

---

## Scenario 2 — Brownfield: Add Redirect Analytics to Existing Service

### 2.1 Scenario Setup

**Existing state**: A URL shortener service that only supports POST `/shorten` and GET `/{shortCode}` redirect, with no logging, no analytics, and a `UrlMapping` model with no access tracking fields.

**New requirement**: "Add analytics to track how often each short URL is clicked, including the requester's IP address, browser, and referrer."

### 2.2 Impact Analysis

**Agent's impact identification process:**

```
Requirement received → Agent scans codebase for affected touch points:

  Affected: UrlMapping model          → Add accessCount, lastAccessedAt fields
  Affected: RedirectController        → Must now capture request metadata
  New:      RedirectLog model         → New document schema for analytics data
  New:      RedirectLogRepository     → New Spring Data interface
  Affected: UrlService.handleRedirect → Must increment count AND log request
  New:      RequestInfoExtractor util → Extract IP/UA/referer from HttpRequest
  Affected: UrlService constructor    → Inject RedirectLogRepository
  Affected: UrlServiceTest            → Mock must include RedirectLogRepository
  New:      redirect_logs collection  → New MongoDB collection
  Affected: application.yml          → No change needed (same MongoDB instance)
```

**Impact classification:**

| Component | Impact Level | Type | Risk |
|---|---|---|---|
| `UrlMapping` | Medium | Schema addition (additive) | Low — no existing fields removed |
| `RedirectController` | High | Behavioral change | Medium — new dependencies injected |
| `UrlService` | High | Logic addition | Medium — new async path added |
| `UrlServiceTest` | High | Test update required | Low — mock update only |
| MongoDB | Low | New collection auto-created | Low — MongoDB is schema-flexible |
| Existing redirect behavior | None | No change to redirect response | Zero — 302 still returned |

### 2.3 Orchestration of the Brownfield Change

**Pre-change checklist (entry gate):**
- [x] Existing tests green before change begins
- [x] Impact scope documented and reviewed
- [x] No breaking changes to existing API contract
- [x] Rollback plan defined: remove `RedirectLog`, revert `handleRedirect()`, remove `RedirectLogRepository`

**Change sequencing:**

```
[Step 1] Add fields to UrlMapping (accessCount, lastAccessedAt) — ADDITIVE
    ↓ No migration needed: MongoDB adds fields on next write
[Step 2] Create RedirectLog model and RedirectLogRepository
    ↓ Safe: new files, no existing code touched
[Step 3] Add RequestInfoExtractor utility
    ↓ Safe: new file, stateless utility
[Step 4] Update UrlService.handleRedirect() — inject RedirectLogRepository
    ↓ RISK POINT: constructor change; all callers must update
[Step 5] Update RedirectController — pass request metadata to service
    ↓ Depends on Step 4
[Step 6] Update UrlServiceTest — add RedirectLogRepository mock
    ↓ Required to keep unit tests passing
[Step 7] Run full test suite — verify no regression
    ↓ EXIT GATE: zero test failures
[Step 8] Verify redirect_logs collection populating in MongoDB
    ↓ Manual validation
```

**Rollback plan:**
If Step 4 breaks existing tests and cannot be fixed quickly:
1. Revert `UrlService.java` to pre-change version
2. Revert `RedirectController.java`
3. Delete `RedirectLog.java`, `RedirectLogRepository.java`, `RequestInfoExtractor.java`
4. Keep `UrlMapping` field additions (they are harmless if unused)

### 2.4 Validation

| Check | Validation Method | Expected |
|---|---|---|
| Existing POST /shorten still works | Integration test: `shouldShortenUrlSuccessfully` | 201 Created |
| Redirect still returns 302 | Integration test: `shouldRedirectSuccessfully` | 302 Redirect |
| Analytics captured | Query `redirect_logs` collection after redirect | Document inserted |
| Access count incremented | Check `url_mappings.accessCount` after redirect | Incremented by 1 |
| Logging failure doesn't break redirect | `redirectLogRepository` throws; redirect still 302 | 302 returned |
| No regression in test suite | `mvn clean test` — zero failures | All tests pass |

---

## Scenario 3 — Ambiguous Requirement: "Add Caching Support"

### 3.1 Requirement as Received

> "Add caching support to the URL shortener to improve performance."

### 3.2 Ambiguity Analysis

This requirement is **intentionally vague**. The agent's first step is to decompose the ambiguity
before writing any code.

**Questions the agent must raise:**

| Ambiguity Dimension | Questions to Ask |
|---|---|
| **What to cache?** | Short code → URL lookups? Redirect responses? Shortened URL creation results? |
| **Cache technology?** | In-memory (Caffeine/ConcurrentHashMap)? Distributed (Redis)? CDN-level (HTTP cache headers)? |
| **Cache TTL?** | How long should a cached entry live? Minutes? Hours? Forever? |
| **Invalidation strategy?** | If a URL is deleted or updated, how does the cache get cleared? |
| **Cache scope?** | Per-instance (JVM heap)? Shared across multiple instances? |
| **Failure mode?** | If cache is unavailable, do we fall back to MongoDB or fail the request? |
| **Metrics?** | Do we need cache hit/miss ratio visibility? |
| **Compliance?** | Does caching an IP-associated redirect log entry raise GDPR concerns? |

### 3.3 Normalization Process

**Agent's disambiguation approach:**

```
Step 1: Identify the performance bottleneck
  → Redirect path: GET /{shortCode} → MongoDB lookup → 302
  → The bottleneck is the MongoDB read on every redirect

Step 2: Determine what varies and what's stable
  → url_mappings records are mostly read-heavy, rarely updated (only accessCount changes)
  → Caching the shortCode→originalUrl mapping is safe and high-value

Step 3: Identify constraints from earlier decisions
  → Human rejected Redis (CP-1): no external cache store
  → Therefore: in-process cache (Spring Cache + Caffeine) is the appropriate scope

Step 4: Define the normalized requirement
  "Cache the shortCode→originalUrl lookup in JVM memory using Spring Cache with Caffeine.
   TTL = 60 minutes (configurable). Invalidate on URL delete (future feature).
   Fall back to MongoDB on cache miss. Cache metrics via Actuator."
```

### 3.4 Task Decomposition (Post-Normalization)

```
[T1] Add Caffeine dependency to pom.xml
[T2] Configure Spring Cache in application.yml (ttl, max-size)
[T3] Annotate UrlService.getOriginalUrl() with @Cacheable("urls")
[T4] Annotate UrlService.handleRedirect() — decision point:
       Option A: Cache the full redirect (cache originalUrl)
       Option B: Don't cache handleRedirect() — it has side effects (accessCount++)
       → DECISION: Cache only getOriginalUrl(); handleRedirect() reads from cache indirectly
                   or calls getOriginalUrl() internally
[T5] Add @CacheEvict on URL delete method (when implemented)
[T6] Write unit test: second call returns from cache (verify repository called once)
[T7] Verify cache metrics at /actuator/metrics → cache.gets
```

**Human approval checkpoint (CP-Cache):**
Before implementing, agent presents the normalized requirement and design to the human:
- "I plan to add Caffeine in-process cache with 60-minute TTL for URL lookups.
  This means accessCount will increment on every redirect but the MongoDB read
  will only happen on cache miss. Is this acceptable, or should every redirect
  hit MongoDB for exact access count tracking?"
- **Human decision required**: exact counts vs. performance

### 3.5 Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Stale cache after URL update | Medium | Medium | Short TTL + `@CacheEvict` on updates |
| Memory pressure from large cache | Low | Medium | Bound max-size in config (`max-cache-size: 10000`) |
| accessCount undercount with caching | High | Low | Document as known trade-off; analytics are approximate |
| Cache not cleared on restart | Certain | Low | Acceptable: cache is warm-up cost on restart |
| Multi-instance cache inconsistency | High (if scaled) | High | Requires Redis if running >1 instance |

### 3.6 Validation Approach

| Test | What It Proves |
|---|---|
| First redirect: MongoDB called once | Cache populated on miss |
| Second redirect: MongoDB NOT called | Cache hit working |
| After 60 min TTL: MongoDB called again | TTL expiry working |
| `/actuator/metrics?name=cache.gets` | Cache metrics visible |
| Cache miss on unknown shortCode | Graceful fallback to 404 |

### 3.7 What This Scenario Demonstrates

1. **Ambiguity resolution**: Agent does not blindly implement "caching" — it asks what, how, and under what constraints before writing code
2. **Constraint awareness**: Agent recalls earlier human decision (no Redis) and adapts solution accordingly
3. **Side-effect reasoning**: Agent identifies that `handleRedirect()` has write side effects and separates cacheable from non-cacheable paths
4. **Human checkpoint**: Non-obvious trade-off (exact counts vs. performance) is escalated to human before implementation
5. **Risk documentation**: Trade-offs are explicit and communicated, not hidden
