# URL Shortener Service - Development Guidelines

## Table of Contents
1. [Project Overview](#project-overview)
2. [Code Quality](#code-quality)
3. [Testing](#testing)
4. [Logging](#logging)
5. [Performance](#performance)
6. [Security](#security)
7. [Documentation](#documentation)
8. [Git Workflow](#git-workflow)

---

## Project Overview

The URL Shortener Service is a backend application designed to:
- Convert long URLs into short, unique identifiers
- Redirect users from short URLs to original long URLs
- Track URL usage analytics
- Provide high availability and performance

**Technology Stack**: Java/Spring Boot, Swagger UI for local testing, In Memory Database (Redis)

---

## Code Quality

### Standards and Principles

#### 1. **Code Style & Formatting** (Java/Spring Boot)
- Use 4-space indentation (Java standard)
- Follow Google Java Style Guide conventions
- Use meaningful variable and method names (camelCase for variables/methods, PascalCase for classes)
- Maximum line length: 120 characters
- Avoid deep nesting (max 3 levels)
- Keep methods small and focused (max 25-30 lines)
- Use @Formatter annotations for consistent formatting

#### 2. **SOLID Principles**
- **Single Responsibility Principle**: Each module/class should have one reason to change
- **Open/Closed Principle**: Open for extension, closed for modification
- **Liskov Substitution Principle**: Subclasses should be substitutable for base classes
- **Interface Segregation Principle**: Clients shouldn't depend on methods they don't use
- **Dependency Inversion Principle**: Depend on abstractions, not concrete implementations

#### 3. **DRY (Don't Repeat Yourself)**
- Avoid code duplication
- Extract common logic into reusable functions/utilities
- Create shared utility modules for cross-cutting concerns

#### 4. **KISS (Keep It Simple, Stupid)**
- Write simple, understandable code
- Prefer explicit over implicit
- Avoid premature optimization
- Use built-in language features and libraries when possible

#### 5. **Comments and Documentation**
- Write comments for WHY, not WHAT (code should be self-explanatory for WHAT), Keep comments concise and relevant
- Comment complex algorithms and edge cases
- Keep comments up-to-date with code changes
- Use JSDoc/docstrings for public APIs and functions

#### 6. **Error Handling**
- Handle all errors explicitly (no silent failures)
- Use typed errors or error codes for different error scenarios
- Provide meaningful error messages
- Log errors with full context (stack trace, input parameters, etc.)
- Never suppress exceptions without good reason

#### 7. **Configuration Management**
- Use environment variables for environment-specific config
- Keep secrets out of source code (use .env files, not committed)
- Provide sensible defaults for development
- Use config validation on startup

---

## Testing

### Test Coverage Requirements

#### 1. **Coverage Targets**
- **Overall Code Coverage**: Minimum 80%
- **Critical Paths**: 100% coverage (URL shortening, redirection, database operations)
- **Business Logic**: Minimum 90% coverage
- **Utility Functions**: Minimum 85% coverage
- **Error Paths**: Minimum 90% coverage

#### 2. **Test Types**

**Unit Tests**
- Test individual functions/methods in isolation
- Mock external dependencies (database, APIs, services)
- Use assertions for expected behavior
- Test both happy paths and error cases
- Example: Test URL validation, hash generation independently

**Integration Tests**
- Test interaction between components
- Use in-memory or test database
- Test API endpoints with real request/response cycle
- Example: Test URL creation → storage → retrieval flow

**Functional/E2E Tests**
- Test complete user workflows
- Test with real or near-real environment setup
- Test API contracts and response formats
- Example: Create short URL → visit short URL → get redirected

**Performance Tests**
- Load testing with expected traffic volume
- Stress testing beyond expected limits
- Latency benchmarks
- Database query performance

#### 3. **Test Organization** (JUnit + Mockito)
```
src/
├── test/java/com/urlshortener/
│   ├── unit/
│   │   ├── UrlServiceTest.java
│   │   ├── UrlValidatorTest.java
│   │   └── HashUtilsTest.java
│   └── integration/
│       ├── UrlControllerTest.java
│       └── UrlRepositoryTest.java
```

#### 4. **Test Best Practices** (JUnit 5 + Mockito)
- Use descriptive test names: `shouldReturnShortenedUrlWhenValidUrlProvided()`
- Arrange-Act-Assert (AAA) pattern:
  ```java
  @Test
  void shouldShortenUrl() {
    // Arrange
    String longUrl = "https://example.com/very/long/url";
    
    // Act
    UrlResponse result = urlService.shortenUrl(longUrl);
    
    // Assert
    assertNotNull(result.getShortCode());
    assertEquals(6, result.getShortCode().length());
  }
  ```
- Use `@DisplayName` for test descriptions
- Use `@Nested` for organizing related tests
- Use Mockito for mocking dependencies
- Test edge cases and boundary conditions

#### 5. **Test Data Management**
- Use factories or fixtures for consistent test data
- Seed test database with known data
- Clean up test data after each test
- Never use production data in tests
- Use realistic test data that represents actual scenarios

#### 6. **Coverage Tools** (Java)
- Use JaCoCo (Java Code Coverage) for coverage reports
- Generate coverage reports after every test run
- Flag coverage regressions in CI/CD (use JaCoCo Maven plugin)
- Target: 80%+ overall, 100% for critical paths
- Integrate with SonarQube for detailed analysis

#### 7. **Continuous Testing**
- Run tests automatically on git hooks (pre-commit)
- Run full test suite in CI/CD pipeline
- Run tests before merging to main branches
- Generate coverage reports in CI/CD
- Fail builds if coverage drops below thresholds

---

## Logging

### Logging Standards

#### 1. **Logging Levels**

| Level | When to Use | Example |
|-------|------------|---------|
| **DEBUG** | Development, detailed app flow | Function entry/exit, variable values, loop iterations |
| **INFO** | Important application events | Server startup, URL creation, successful operations |
| **WARN** | Potentially harmful situations | Deprecated API usage, resource constraints, retries |
| **ERROR** | Error conditions | Failed database operations, validation errors, exceptions |
| **FATAL/CRITICAL** | Application shutdown situations | Out of memory, database connection loss, unrecoverable errors |

#### 2. **What to Log**

**DO Log:**
- Application startup and shutdown events
- Incoming requests (method, path, parameters - sanitized)
- Database operations (queries, success/failure)
- External API calls (request, response, duration)
- Authentication/Authorization events
- Errors with full context (stack trace, input, expected vs actual)
- Performance metrics (execution time, memory usage)
- Business events (URL created, accessed, deleted)

**DON'T Log:**
- Passwords, tokens, API keys, secrets
- Sensitive PII (full credit card numbers, SSNs, etc.)
- Entire request/response bodies (just relevant parts)
- Repetitive debug logs in loops (batch or sample them)
- Third-party library internal logs at DEBUG level

#### 3. **Log Format**

Use structured logging (JSON format) for easy parsing:

\\\json
{
  "timestamp": "2026-08-04T09:22:22.713Z",
  "level": "INFO",
  "service": "url-shortener",
  "component": "url.service",
  "action": "shorten_url",
  "message": "Successfully shortened URL",
  "userId": "user123",
  "originalUrl": "https://example.com/path",
  "shortCode": "abc123",
  "duration_ms": 45,
  "requestId": "req-uuid-12345"
}
\\\

#### 4. **Logging Best Practices**
- Include request IDs for tracing across services
- Log at appropriate levels (don't log everything as ERROR)
- Include context and metadata (user ID, operation ID, duration)
- Sanitize sensitive information automatically
- Use correlation IDs for related log entries
- Log exceptions with stack traces
- Include duration for operations (performance tracking)
- Log state transitions
- Use consistent field names across all logs

#### 5. **Log Rotation and Storage**
- Implement log rotation (daily or by size)
- Store logs for minimum 30 days
- Use log aggregation service (ELK, Datadog, CloudWatch, etc.)
- Archive old logs to long-term storage
- Set up alerts for ERROR and FATAL logs

#### 6. **Logger Configuration** (SLF4J + Logback)
```java
private static final Logger logger = LoggerFactory.getLogger(UrlService.class);

// Usage
logger.debug("Processing URL: {}", url);
logger.info("Successfully created short URL: {}", shortCode);
logger.warn("Cache limit approaching: {}", cacheSize);
logger.error("Failed to shorten URL", exception);
```

---

## Performance

### Performance Requirements & Guidelines

#### 1. **Performance Targets**
- URL shortening API: < 100ms p95 latency
- URL redirection API: < 50ms p95 latency
- Database queries: < 10ms for cached results, < 50ms for disk reads
- Throughput: Support 1000+ requests/second
- Memory usage: < 500MB for normal operations

#### 2. **Caching Strategy**

**In-Memory Cache**
- Cache frequently accessed URLs
- Use LRU (Least Recently Used) eviction policy
- Cache TTL: 1 hour for read operations, no cache for writes
- Max cache size: 10,000 entries
- Example: Cache short-to-long URL mappings

**Database Query Optimization**
- Index frequently queried fields (short_code, created_at, user_id)
- Use database connection pooling
- Batch read operations when possible
- Use denormalization for read-heavy operations

**HTTP Caching**
- Add Cache-Control headers for GET requests
- Use ETags for conditional requests
- Set appropriate cache expiry (5 minutes for redirects, 1 hour for metadata)

#### 3. **Database Optimization**
- Use indexes on:
  - Primary key (short_code)
  - Foreign keys (user_id)
  - Frequently filtered fields (status, created_at)
- Query optimization:
  - Use EXPLAIN ANALYZE to understand query plans
  - Avoid SELECT * (only fetch needed columns)
  - Use LIMIT for large result sets
  - Batch insert operations
- Connection pooling:
  - Min pool size: 5, Max pool size: 20
  - Connection timeout: 5 seconds
  - Idle timeout: 30 minutes

#### 4. **API Optimization**
- Implement pagination for list endpoints
- Use compression (gzip) for responses
- Implement request timeout (30 seconds)
- Use HTTP/2 or HTTP/3
- Implement rate limiting per user/IP

#### 5. **Code Performance**
- Avoid N+1 queries (eager load related data)
- Use async/await patterns (don't block)
- Implement circuit breakers for external calls
- Use lazy loading for large data structures
- Profile code regularly (identify bottlenecks)

#### 6. **Monitoring & Metrics**
- Track response times (p50, p95, p99)
- Monitor error rates
- Track throughput (requests/second)
- Monitor resource usage (CPU, memory, disk)
- Set up alerts for performance degradation
- Use APM tools (New Relic, DataDog, Prometheus)

#### 7. **Load Testing**
- Test with expected peak load (1000+ req/s)
- Test with gradual ramp-up
- Identify performance bottlenecks
- Measure resource utilization under load
- Document performance profiles

#### 8. **Scalability Considerations**
- Stateless services (easy horizontal scaling)
- Use load balancers for distribution
- Database replication for read scaling
- Cache layers (Redis) for high read volume
- Async job queues for heavy operations

---

## Security

### Security Requirements

#### 1. **Input Validation**
- Validate URL format (must be valid HTTP/HTTPS URL)
- Validate short code format
- Validate user input length (prevent buffer overflows)
- Whitelist allowed characters
- Reject malicious payloads (SQL injection, XSS, etc.)

#### 2. **Authentication & Authorization**
- Use strong authentication (OAuth2, JWT)
- Implement API key validation
- Rate limit by user/API key
- Validate user permissions before operations
- Log all authentication events

#### 3. **Data Protection**
- Encrypt sensitive data in transit (HTTPS only)
- Encrypt sensitive data at rest
- Hash passwords using bcrypt/scrypt
- Use secure random number generation for short codes
- Implement data retention policies

#### 4. **API Security**
- Implement CORS properly
- Use security headers (Content-Security-Policy, X-Frame-Options, etc.)
- Implement request signing for critical operations
- Validate Content-Type headers
- Implement request size limits

#### 5. **Dependency Management**
- Keep dependencies up-to-date
- Use vulnerability scanning tools (npm audit, Snyk, OWASP)
- Remove unused dependencies
- Pin dependency versions
- Regular security audits

---

## Documentation

### Documentation Standards

#### 1. **API Documentation**
- Document all endpoints (method, path, parameters, response)
- Include request/response examples
- Document error codes and messages
- Document authentication requirements
- Use OpenAPI/Swagger for API specs
- Keep documentation in sync with code

#### 2. **Code Documentation**
- JSDoc/docstring for all public functions
- Document complex algorithms
- Document configuration options
- Include usage examples in code comments
- Document assumptions and constraints

#### 3. **Architecture Documentation**
- Document system architecture (components, interactions)
- Document data flow
- Document deployment architecture
- Include architecture diagrams
- Document design decisions (ADRs - Architecture Decision Records)

#### 4. **Setup & Deployment**
- Include setup instructions in README
- Document environment variables
- Document database schema
- Include deployment procedures
- Document rollback procedures

---

## Git Workflow

### Git Best Practices

#### 1. **Branching Strategy**
- Use feature branches: \eature/short-description\
- Use bugfix branches: \ugfix/issue-description\
- Protect main/production branches
- Require PR reviews before merging
- Delete merged branches

#### 2. **Commit Standards**
- Use meaningful commit messages
- Format: \	ype(scope): description\
  - Types: feat, fix, docs, style, refactor, test, chore, perf
  - Example: \eat(api): add URL expiration feature\
- Keep commits atomic (one logical change per commit)
- Sign commits with GPG when possible

#### 3. **Pull Request Standards**
- Include descriptive title and description
- Link related issues
- Include test coverage changes
- Request reviews from team members
- Address review feedback
- Include migration steps if database schema changed

#### 4. **Code Review**
- Review for correctness, performance, security
- Check test coverage
- Verify logging and error handling
- Ensure documentation is updated
- Be constructive and collaborative

---

## Quality Checklist

Before committing code, verify:

- [ ] Code follows style guide
- [ ] All new functions have tests
- [ ] Test coverage >= 80%
- [ ] No console.log statements (use logger)
- [ ] No hardcoded secrets or credentials
- [ ] Error handling implemented
- [ ] Logging added for debugging
- [ ] API/function documented
- [ ] Performance considered
- [ ] Database queries optimized
- [ ] PR description includes context
- [ ] Related issue is linked

---

## Tools & Technologies

### Recommended Tools (Java/Spring Boot Stack)

**Testing**
- JUnit 5 (Jupiter) for unit testing
- Mockito for mocking dependencies
- RestAssured for API testing
- TestContainers for integration tests

**Code Quality**
- Checkstyle for style enforcement
- SpotBugs for bug detection
- SonarQube for code analysis
- JaCoCo for code coverage

**Logging**
- SLF4J + Logback for structured logging
- Spring Boot Actuator for metrics

**Monitoring**
- Micrometer with Prometheus metrics
- Spring Boot Actuator endpoints
- Grafana for visualization

**Performance**
- JMH (Java Microbenchmark Harness) for benchmarking
- JProfiler or YourKit for profiling
- LoadUI or JMeter for load testing

**Security**
- OWASP Dependency-Check Maven plugin
- SnykIO for vulnerability scanning
- Spring Security for authentication/authorization

---

## CI/CD Pipeline Requirements

The CI/CD pipeline should:

1. Run linting on all commits
2. Run full test suite
3. Generate code coverage reports
4. Fail if coverage < 80%
5. Run security scanning
6. Run performance tests on main branch
7. Build and deploy on main branch merge
8. Generate build artifacts/logs
9. Send notifications on failures

---

## Success Metrics

Track these metrics to ensure quality:

- Code coverage (target: 80%+)
- Test pass rate (target: 100%)
- Performance metrics (latency, throughput)
- Error rates (target: < 0.1%)
- Security vulnerabilities (target: 0)
- Deployment frequency
- Lead time for changes
- Mean time to recovery (MTTR)

---

**Last Updated**: 2026-08-04
**Version**: 1.0
