# Engineering Summary

## Project: URL Shortener Service (Agentic SDLC Demonstration)

---

## 1. Executive Summary

This project delivers a production-grade URL shortener service built using an **agentic engineering
model**: an AI agent (GitHub Copilot CLI) autonomously decomposed requirements, implemented code,
wrote tests, fixed failures, and generated documentation — all under human oversight with explicit
approval gates at high-impact decision points.

The URL shortener is the engineering artifact. The agentic orchestration process used to build it
is the primary demonstration object.

---

## 2. Artifacts Produced

### Application Code

| Artifact | Location | Description |
|---|---|---|
| Spring Boot Application | `src/main/java/com/urlshortener/` | 16 Java source files |
| `UrlShortenerApplication.java` | `com.urlshortener` | Spring Boot entry point with `@EnableCaching` |
| `RedirectController.java` | `controller/` | Root-level `/{shortCode}` → 302 redirect |
| `UrlController.java` | `controller/` | `/api/v1/urls/` — shorten, JSON lookup, health |
| `UrlService.java` | `service/` | Core business logic: shorten, redirect, analytics |
| `UrlMapping.java` | `model/` | MongoDB document: shortCode, originalUrl, userId, accessCount |
| `RedirectLog.java` | `model/` | Analytics document: IP, UA, referer, responseTimeMs |
| `UrlRepository.java` | `repository/` | Spring Data MongoDB interface |
| `RedirectLogRepository.java` | `repository/` | Spring Data MongoDB interface (analytics) |
| `UrlValidator.java` | `validator/` | HTTP/HTTPS URL format validation |
| `HashGenerator.java` | `util/` | Base62 short code generator (6 chars, SHA-256-seeded) |
| `RequestInfoExtractor.java` | `util/` | HTTP request metadata extractor (IP, UA, referer) |
| `ShortenUrlRequest/Response.java` | `dto/` | Input/output DTOs with `@Valid` constraints |
| `UrlException.java` | `exception/` | Domain exception; converts to structured JSON errors |
| `MongoConfig.java` | `config/` | MongoDB connection configuration |
| `OpenApiConfig.java` | `config/` | Swagger/OpenAPI metadata configuration |

### Test Suite

| Artifact | Tests | Scope |
|---|---|---|
| `UrlServiceTest.java` | 15+ | Unit: shorten, getOriginalUrl, validation |
| `UrlServiceRedirectTest.java` | 6+ | Unit: handleRedirect, analytics, failure modes |
| `HashGeneratorTest.java` | 8+ | Unit: code length, character set, uniqueness |
| `UrlValidatorTest.java` | 12+ | Unit: valid/invalid URL patterns |
| `RequestInfoExtractorTest.java` | 10+ | Unit: IP extraction, X-Forwarded-For, UA, referer |
| `RedirectLogModelTest.java` | 10+ | Unit: model construction, field validation |
| `UrlMappingModelTest.java` | 14+ | Unit: model fields, builder, equality |
| `UrlExceptionTest.java` | 3 | Unit: both constructors, RuntimeException hierarchy |
| `UrlControllerTest.java` | 10+ | Integration: all endpoints, MockMvc + MockBean |
| `UrlControllerAdditionalTest.java` | 11+ | Integration: headers, edge cases, multi-redirect |
| `UrlControllerErrorHandlingTest.java` | 15+ | Integration: 400/404 errors, complex inputs |
| `RepositoryIntegrationTest.java` | 14+ | Integration: MongoDB CRUD operations |
| `UrlShortenerApplicationTest.java` | 1 | Context load verification |
| **Total** | **~129 tests** | **Coverage ≥ 70% (JaCoCo enforced)** |

### Documentation

| Document | Purpose |
|---|---|
| `ARCHITECTURE.md` | System design, components, data model, API surface, design decisions |
| `ORCHESTRATION.md` | Agentic orchestration model: dependency graph, gates, approval checkpoints, retry/fallback, policy guardrails, observability, reliability metrics |
| `SCENARIOS.md` | Three engineering scenarios: greenfield, brownfield, ambiguous — each with decomposition, orchestration, and validation |
| `ENGINEERING_SUMMARY.md` | This document: plan, rationale, artifacts, risks, trade-offs, assumptions, limitations |
| `SETUP.md` | Setup instructions, running the service, API guide |

---

## 3. Plan and Rationale

### Phase 1 — Foundation
**Goal**: Working shorten + redirect service.  
**Rationale**: Establish a runnable baseline before adding complexity. Tests cannot be written without a working service; documentation cannot be accurate without working code.

### Phase 2 — Analytics (Brownfield)
**Goal**: Add redirect logging with IP, UA, referer, response time.  
**Rationale**: Analytics were added as a brownfield change to demonstrate impact analysis, backward compatibility, and additive schema evolution. This was not a parallel feature — it intentionally extended existing code to show engineering judgment in brownfield contexts.

### Phase 3 — Testing
**Goal**: ≥70% line coverage with unit + integration tests.  
**Rationale**: Coverage enforcement at build time (JaCoCo) ensures quality gates are non-optional. Tests were written after the service was complete to demonstrate test-after-TDD, though TDD (test-first) would be preferred in production.

### Phase 4 — Documentation and Orchestration Narrative
**Goal**: Full documentation satisfying agentic SDLC demonstration criteria.  
**Rationale**: The assignment evaluates the orchestration process, not just the code. Documentation must capture the decision lineage, approval checkpoints, re-planning events, and scenario decompositions that demonstrate controlled autonomy.

---

## 4. Key Decisions and Rationale

| # | Decision | Why | What Was Rejected |
|---|---|---|---|
| D1 | MongoDB as primary store | `shortCode` is natural `_id`; O(1) lookup | PostgreSQL (unnecessary schema), Redis-only (no persistence) |
| D2 | Base62 6-char short codes | 56B combinations, URL-safe, human-typeable | UUID (too long), sequential integers (enumerable) |
| D3 | Separate `RedirectController` at root | URL shortener redirect must be at `/{code}`, not `/api/v1/urls/{code}` | Single controller (forced ugly URLs) |
| D4 | Optimistic (non-atomic) access count | Prototype simplicity; analytics are approximate | MongoDB `$inc` (correct but adds complexity) |
| D5 | Analytics failure is swallowed | User redirect must never fail due to analytics write failure | Propagate exception (breaks user experience) |
| D6 | No Redis caching | Human constraint; MongoDB `_id` lookup is already O(1) | Redis (adds operational complexity) |
| D7 | `expiresAt` modeled but not enforced | Forward-compatible schema; scope controlled | Full expiry enforcement (requires scheduler + TTL index) |
| D8 | JaCoCo excludes `UrlShortenerApplication` | `main()` cannot be meaningfully unit tested; bootstrap classes are universally excluded | Call `main()` in test (starts second Spring context, port conflicts) |

---

## 5. Risks and Trade-offs

### Active Risks (Prototype Scope Accepted)

| Risk | Likelihood | Impact | Accepted Because |
|---|---|---|---|
| Short code collision | Low (56B space) | High (wrong redirect) | Prototype scope; production adds collision detection loop |
| Non-atomic access count | High under concurrent load | Low (count undercount) | Analytics are approximate; exact counts not a hard requirement |
| Analytics data loss on MongoDB failure | Low | Medium | User redirect unaffected; swallowed exception is intentional |
| No URL expiry enforcement | Certain | Low | `expiresAt` modeled for future; purge job out of scope |
| No authentication on `/shorten` | Certain | Medium | Prototype; any user can create short URLs |
| IP stored unmasked in `redirect_logs` | Certain | High (GDPR) | Prototype; GDPR controls out of scope |

### Architectural Trade-offs

| Trade-off | Decision Made | Alternative |
|---|---|---|
| Simplicity vs. exactness | Optimistic increment (simpler) | Atomic MongoDB `$inc` (exact) |
| Performance vs. consistency | No cache (simpler) | Caffeine cache (faster, stale risk) |
| User experience vs. observability | Swallow analytics errors | Propagate errors (breaks redirects) |
| Schema flexibility vs. query power | MongoDB (flexible schema) | PostgreSQL (strong query/reporting) |

---

## 6. Assumptions

1. **Single instance deployment**: No horizontal scaling assumed; non-atomic increments and in-JVM state are acceptable.
2. **MongoDB is externally managed**: No embedded MongoDB; assumes MongoDB running at `localhost:27017` (configurable via `MONGODB_URI` env var).
3. **Trusted reverse proxy**: `X-Forwarded-For` header is trusted for IP extraction; no proxy validation.
4. **No authentication required for prototype**: `/shorten` endpoint is open; any caller can create short URLs.
5. **Base URL is configurable**: `BASE_URL` env var; defaults to `http://localhost:8080/s` (not a real domain).
6. **Analytics are approximate**: Access counts and redirect logs are best-effort; not guaranteed under failures.
7. **Short codes do not expire**: `expiresAt` field is modeled but never enforced.

---

## 7. Limitations

| Limitation | Impact | Remediation Path |
|---|---|---|
| No collision detection on short code generation | Rare wrong redirects at very high scale | Add retry loop with existence check in `HashGenerator` |
| No URL expiry | Deleted/moved URLs still redirect | Add `@Scheduled` cleanup job + TTL index on `expiresAt` |
| No rate limiting | Abuse of `/shorten` endpoint | Add Spring Security + bucket4j rate limiter |
| No authentication | Any user creates URLs | Add JWT/OAuth2 to `/shorten` |
| Non-atomic access count | Undercount under concurrent load | Replace with `MongoTemplate.updateFirst()` + `$inc` |
| GDPR non-compliant IP logging | Legal risk in EU | Mask last octet or hash IP address before storage |
| No custom short code support | Users cannot choose their short code | Add custom code field to request with availability check |
| No analytics query API | No way to retrieve redirect stats | Add `GET /api/v1/urls/{code}/analytics` endpoint |
| Single-region MongoDB | Single point of failure | Atlas multi-region replica set |
| No HTTPS enforcement | Traffic is unencrypted in test | Add TLS termination at load balancer |

---

## 8. Future Roadmap

**Phase 1 (Production Hardening):**
- Collision detection in `HashGenerator`
- Atomic access count with `$inc`
- URL expiry enforcement + purge scheduler
- Rate limiting with bucket4j
- JWT authentication on `/shorten`

**Phase 2 (Scale):**
- Caffeine in-JVM cache for URL lookups
- Redis cache for multi-instance deployments
- Read replica for analytics queries

**Phase 3 (Features):**
- Custom short codes
- Analytics query API (`GET /urls/{code}/analytics`)
- Bulk URL shortening
- URL preview endpoint (no redirect, shows destination)
- QR code generation

**Phase 4 (Compliance):**
- IP masking for GDPR
- Data retention policies on `redirect_logs`
- Audit log (tamper-evident)
- User consent for tracking

---

## 9. Agentic Execution Assessment

### What Worked Well

| Capability | Evidence |
|---|---|
| Requirement interpretation | Ambiguities surfaced before coding; normalized problem statement produced |
| Task decomposition | Dependency graph explicit; parallel paths identified |
| Code generation quality | Production-quality Spring Boot patterns; Lombok, Builder, validation, structured errors |
| Failure detection | Agent identified root cause of 11 test failures across multiple sessions |
| Re-planning | Agent re-planned after Redis rejection, embedded MongoDB rejection, path mismatch discovery |
| Human checkpointing | Agent asked before modifying main code, pom.xml, and endpoint contracts |
| Documentation | Full SDLC documentation generated from code and session history |

### Where Human Judgment Was Essential

| Situation | Human Decision Required |
|---|---|
| Storage technology choice | Human rejected Redis; agent could not know infrastructure constraints |
| Test database strategy | Human rejected embedded MongoDB; agent could not know team preferences |
| Endpoint path design | Human approved `RedirectController` separation after agent proposed it |
| Coverage exclusion | Human approved pom.xml modification for `UrlShortenerApplication` |
| Scope control | Human confirmed analytics/expiry/auth are out of prototype scope |

### Governance Maintained Throughout

- Agent never modified main code without explicit approval
- All design decisions are documented with rationale (this document + ARCHITECTURE.md)
- Every test failure was traced to root cause before fix was applied
- No silent failures: all exceptions are logged; analytics failure is logged even when swallowed
- Build gates enforce quality: `mvn verify` fails if coverage drops below 70%
