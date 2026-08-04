# url-shortener

URL shortener service built with Java/Spring Boot and Redis for high-performance URL shortening and redirection.

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Redis running locally or accessible remotely
- Docker (optional)

### Installation

1. **Clone and setup:**
   ```bash
   git clone <repository-url>
   cd url-shortener
   ```

2. **Configure environment:**
   ```bash
   # Create .env or set environment variables
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   export SERVER_PORT=8080
   export BASE_URL=http://localhost:8080/s
   ```

3. **Build and run:**
   ```bash
   # Build with tests
   mvn clean package
   
   # Run application
   mvn spring-boot:run
   
   # Or use JAR directly
   java -jar target/url-shortener-service-1.0.0.jar
   ```

### API Documentation

Once running, access:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Health**: `http://localhost:8080/api/v1/urls/health`
- **Metrics**: `http://localhost:8080/actuator/prometheus`

### Running Tests

```bash
# Run all tests with coverage
mvn clean test jacoco:report

# View coverage report
# Open target/site/jacoco/index.html in browser

# Run specific test class
mvn test -Dtest=HashGeneratorTest

# Run integration tests only
mvn test -Dgroups=integration
```

### Project Structure

```
src/
├── main/java/com/urlshortener/
│   ├── config/          # Spring configuration
│   ├── controller/      # REST controllers
│   ├── service/         # Business logic
│   ├── repository/      # Data access layer
│   ├── model/          # Domain models
│   ├── dto/            # Request/Response DTOs
│   ├── exception/      # Custom exceptions
│   ├── util/           # Utilities (HashGenerator, etc.)
│   └── validator/      # Input validators
├── resources/
│   └── application.yml  # Configuration
└── test/java/...       # Tests (unit & integration)
```

## API Endpoints

### Shorten URL
```
POST /api/v1/urls/shorten
Content-Type: application/json

{
  "originalUrl": "https://example.com/very/long/url",
  "userId": "optional-user-id"
}

Response (201):
{
  "shortCode": "abc123",
  "shortUrl": "http://localhost:8080/s/abc123",
  "originalUrl": "https://example.com/very/long/url",
  "createdTimestamp": 1628000000000
}
```

### Get Original URL
```
GET /api/v1/urls/{shortCode}

Response (200):
https://example.com/very/long/url
```

## Development Guidelines

Follow guidelines in [guidelines.md](guidelines.md) covering:
- Code quality (SOLID principles, naming conventions)
- Testing (80%+ coverage, unit/integration tests)
- Logging (structured JSON logging with SLF4J)
- Performance (< 100ms for shorten, < 50ms for redirect)
- Security (input validation, URL verification)

## Performance Targets

- **Shortening API**: < 100ms p95 latency
- **Redirection API**: < 50ms p95 latency
- **Throughput**: 1000+ requests/second
- **Memory**: < 500MB normal operations
- **Code Coverage**: 80%+ minimum

## Configuration Options

| Variable | Default | Description |
|----------|---------|-------------|
| REDIS_HOST | localhost | Redis server hostname |
| REDIS_PORT | 6379 | Redis server port |
| SERVER_PORT | 8080 | Application port |
| BASE_URL | http://localhost:8080/s | Short URL base |
| REDIS_DATABASE | 0 | Redis database number |

## Monitoring

- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Prometheus**: `GET /actuator/prometheus`

## Docker Support

```bash
# Build Docker image
docker build -t url-shortener:latest .

# Run with Redis container
docker-compose up -d
```

## CI/CD Pipeline

- Runs on every push to main branch
- Linting (Checkstyle) + Tests (JUnit 5) + Coverage (JaCoCo) required
- Minimum 80% code coverage required
- Security scanning (OWASP Dependency Check)
