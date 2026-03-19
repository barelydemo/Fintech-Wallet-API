# FinTech Wallet API

A high-concurrency peer-to-peer money transfer system built with Java 17, Spring Boot 3, and MySQL. The system uses pessimistic locking to prevent race conditions and ensures atomic transactions.

## Prerequisites

- Java 17 or higher
- Maven 3.9+ (or use the included Maven wrapper)
- MySQL 8.0+

## Project Structure

```
fintech-wallet-api/
├── src/
│   ├── main/
│   │   ├── java/com/fintech/wallet/
│   │   │   └── WalletApiApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/fintech/wallet/
│           └── WalletApiApplicationTests.java
├── pom.xml
└── README.md
```

## Dependencies

- **Spring Boot 3.2.0**: Core framework
- **Spring Data JPA**: Database access layer
- **Spring Boot Validation**: Request validation
- **MySQL Connector**: MySQL database driver
- **Lombok**: Reduce boilerplate code
- **HikariCP**: High-performance connection pool (included with Spring Boot)
- **H2 Database**: In-memory database for testing

## Configuration

The application is configured via `src/main/resources/application.properties`:

### Database Configuration
- MySQL connection URL: `jdbc:mysql://localhost:3306/wallet_db`
- Default credentials: root/root (change for production)

### HikariCP Connection Pool
- Maximum pool size: 20 connections
- Minimum idle: 5 connections
- Connection timeout: 30 seconds
- Leak detection threshold: 60 seconds

### JPA/Hibernate
- DDL auto: update (creates/updates schema automatically)
- SQL logging: enabled for debugging

## Building the Project

Using Maven wrapper (recommended):
```bash
# Windows
.\mvnw.cmd clean install

# Linux/Mac
./mvnw clean install
```

Using system Maven:
```bash
mvn clean install
```

## Running Tests

```bash
# Windows
.\mvnw.cmd test

# Linux/Mac
./mvnw test
```

## Running the Application

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

The application will start on port 8080.

## Database Setup

Before running the application, create the MySQL database:

```sql
CREATE DATABASE wallet_db;
```

The application will automatically create the necessary tables on startup (due to `spring.jpa.hibernate.ddl-auto=update`).

## Next Steps

This is the initial project setup (Task 1). The following components will be implemented in subsequent tasks:

1. Database schema and entity models (Account, Transaction)
2. Repository interfaces with pessimistic locking support
3. Custom exception classes
4. DTO classes for API requests/responses
5. TransferService with core business logic
6. REST controller layer
7. Global exception handler
8. Integration tests with Testcontainers

## Architecture Overview

The system implements a layered architecture:
- **Controller Layer**: REST API endpoints
- **Service Layer**: Business logic with transaction management
- **Repository Layer**: Data access with pessimistic locking
- **Entity Layer**: JPA entities with validation

## Key Features

- **Pessimistic Locking**: Prevents race conditions in concurrent transfers
- **Atomic Transactions**: All-or-nothing transfer execution
- **Connection Pooling**: High-performance database connections via HikariCP
- **Validation**: Multi-layer validation (controller, service, database)
- **Audit Trail**: Transaction records for all transfers

## License

This project is for educational purposes.
