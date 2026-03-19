# Testcontainers Integration Test Setup

## Overview

This project uses Testcontainers to run integration tests against a real MySQL database in a Docker container. This provides more realistic testing compared to in-memory databases like H2.

## Prerequisites

**Docker must be installed and running** on your system to execute integration tests that extend `BaseIntegrationTest`.

### Installing Docker

- **Windows**: [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
- **macOS**: [Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/)
- **Linux**: [Docker Engine](https://docs.docker.com/engine/install/)

After installation, ensure Docker is running before executing integration tests.

## Usage

### Base Integration Test Class

All integration tests that require a real MySQL database should extend `BaseIntegrationTest`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void testSomething() {
        // Your test code here
        // accountRepository and transactionRepository are available
    }
}
```

### Features Provided by BaseIntegrationTest

1. **MySQL Container**: Automatically starts a MySQL 8.0 container before tests
2. **Spring Boot Context**: Full application context with random port
3. **Automatic Cleanup**: Database is cleaned between tests via `@BeforeEach`
4. **Repository Access**: `accountRepository` and `transactionRepository` are injected
5. **Container Reuse**: MySQL container is reused across test classes for faster execution

### Running Integration Tests

```bash
# Run all tests (requires Docker)
./mvnw test

# Run specific integration test
./mvnw test -Dtest=BaseIntegrationTestVerification

# Skip integration tests if Docker is not available
./mvnw test -DskipITs
```

### Configuration

The MySQL container is configured with:
- **Image**: mysql:8.0
- **Database**: testdb
- **Username**: test
- **Password**: test
- **Reuse**: Enabled for faster test execution

Spring Boot is automatically configured to use the Testcontainers MySQL instance via `@DynamicPropertySource`.

## Troubleshooting

### Error: "Could not find a valid Docker environment"

**Cause**: Docker is not installed or not running.

**Solution**: 
1. Install Docker Desktop (see Prerequisites above)
2. Start Docker Desktop
3. Verify Docker is running: `docker ps`
4. Re-run tests

### Tests are slow on first run

**Cause**: Testcontainers needs to download the MySQL Docker image on first run.

**Solution**: This is normal. Subsequent runs will be faster as the image is cached.

### Port conflicts

**Cause**: Another MySQL instance is using the same port.

**Solution**: Testcontainers automatically assigns random ports, so this should not occur. If it does, stop other MySQL instances or restart Docker.

## Benefits of Testcontainers

1. **Realistic Testing**: Tests run against actual MySQL, not an in-memory database
2. **Isolation**: Each test run gets a fresh database instance
3. **CI/CD Friendly**: Works in CI environments with Docker support
4. **No Manual Setup**: No need to install or configure MySQL locally
5. **Version Control**: MySQL version is specified in code, ensuring consistency

## Requirements Validated

- **Requirement 11.1**: Concurrent transfers execute serially on same account
- **Requirement 11.2**: Concurrent transfers on different accounts execute in parallel
- **Requirement 11.3**: Pessimistic locking prevents race conditions

## Related Files

- `BaseIntegrationTest.java`: Base class for all integration tests
- `BaseIntegrationTestVerification.java`: Verification test for Testcontainers setup
- `pom.xml`: Testcontainers dependencies configuration
