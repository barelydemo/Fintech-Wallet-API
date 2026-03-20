# FinTech Wallet API

A high-concurrency peer-to-peer money transfer system built with Java 17, Spring Boot 3, and MySQL. The system enables secure money transfers between accounts while maintaining data consistency and preventing race conditions through pessimistic locking.

## Features

- **Atomic Transactions**: All transfers are atomic - either fully complete or fully rollback
- **Concurrency Control**: Pessimistic locking prevents race conditions in high-load scenarios
- **Balance Conservation**: Money is never created or destroyed - total system balance remains constant
- **Comprehensive Error Handling**: Clear error messages for insufficient funds, missing accounts, and validation failures
- **Audit Trail**: All completed transfers are recorded with timestamps for reconciliation
- **RESTful API**: Clean REST endpoints with JSON request/response format

## Prerequisites

Before running the application, ensure you have the following installed:

- **Java 17 or higher** - [Download Java](https://adoptium.net/)
- **Maven 3.9+** - [Download Maven](https://maven.apache.org/download.cgi)
- **MySQL 8.0+** - [Download MySQL](https://dev.mysql.com/downloads/mysql/)

Verify installations:
```bash
java -version    # Should show Java 17 or higher
mvn -version     # Should show Maven 3.9 or higher
mysql --version  # Should show MySQL 8.0 or higher
```

## Database Setup

### 1. Create Database

Connect to MySQL and create the database:

```bash
mysql -u root -p
```

```sql
CREATE DATABASE wallet_db;
USE wallet_db;
```

### 2. Configure Database Connection

Update `src/main/resources/application.properties` with your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/wallet_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. Initialize Schema

The application uses Hibernate's `ddl-auto=update` to automatically create tables on startup. Tables will be created when you first run the application.

### 4. Apply Indexes and Constraints (Optional but Recommended)

For production environments, apply database indexes and constraints:

```bash
mysql -u root -p wallet_db < src/main/resources/db/mysql-indexes-constraints.sql
```

This adds:
- Performance indexes for faster queries
- CHECK constraints to enforce data integrity at database level
- Composite indexes for time-range queries

See `src/main/resources/db-migration-guide.md` for detailed migration instructions.

## Running the Application

### Option 1: Using Maven

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

### Option 2: Using JAR

```bash
# Build the JAR
mvn clean package

# Run the JAR
java -jar target/wallet-api-1.0.0.jar
```

The application will start on `http://localhost:8080`

### Verify Application is Running

```bash
curl http://localhost:8080/actuator/health
```

## API Endpoints

### 1. Create Transfer

Initiates a money transfer between two accounts.

**Endpoint**: `POST /api/transfers`

**Request Body**:
```json
{
  "senderId": 1,
  "receiverId": 2,
  "amount": 100.00
}
```

**Success Response** (200 OK):
```json
{
  "transactionId": 42,
  "status": "COMPLETED",
  "message": "Transfer successful",
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Responses**:

- **400 Bad Request** - Insufficient Funds:
```json
{
  "error": "Insufficient Funds",
  "message": "Account 1 has insufficient balance for transfer",
  "timestamp": "2024-01-15T10:31:00"
}
```

- **404 Not Found** - Account Not Found:
```json
{
  "error": "Account Not Found",
  "message": "Receiver account 999 does not exist",
  "timestamp": "2024-01-15T10:32:00"
}
```

- **400 Bad Request** - Validation Error:
```json
{
  "error": "Validation Failed",
  "message": "Amount must be at least 0.01",
  "timestamp": "2024-01-15T10:33:00"
}
```

### 2. Get Transfer Status

Retrieves the status of a transfer by transaction ID.

**Endpoint**: `GET /api/transfers/{transactionId}`

**Example**: `GET /api/transfers/42`

**Success Response** (200 OK):
```json
{
  "transactionId": 42,
  "status": "COMPLETED",
  "message": "Transfer status retrieved",
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Response** (404 Not Found):
```json
{
  "error": "Transaction Not Found",
  "message": "Transaction 999 does not exist",
  "timestamp": "2024-01-15T10:35:00"
}
```

## Example Usage with cURL

### Create Test Accounts

First, you'll need to create accounts in the database:

```sql
USE wallet_db;

INSERT INTO accounts (user_id, balance, version, created_at, updated_at) 
VALUES 
  (100, 1000.00, 0, NOW(), NOW()),
  (101, 500.00, 0, NOW(), NOW()),
  (102, 0.00, 0, NOW(), NOW());
```

### Successful Transfer

```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": 1,
    "receiverId": 2,
    "amount": 100.00
  }'
```

### Check Transfer Status

```bash
curl http://localhost:8080/api/transfers/1
```

### Transfer with Insufficient Funds

```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": 1,
    "receiverId": 2,
    "amount": 10000.00
  }'
```

### Transfer to Non-Existent Account

```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": 1,
    "receiverId": 999,
    "amount": 50.00
  }'
```

## Running Tests

The project includes comprehensive test coverage:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test end-to-end workflows with real database (Testcontainers)
- **Property-Based Tests**: Verify universal correctness properties across random inputs

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=TransferServiceTest
```

### Run Integration Tests Only

```bash
mvn test -Dtest=*IntegrationTest
```

### Run Property-Based Tests Only

```bash
mvn test -Dtest=*PropertyTest
```

**Note**: Integration tests use Testcontainers to spin up a MySQL container automatically. Docker must be running.

## Configuration

### Application Properties

Key configuration options in `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/wallet_db
spring.datasource.username=root
spring.datasource.password=root

# Connection Pool Configuration (HikariCP)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Logging Configuration
logging.level.com.fintech.wallet=DEBUG
```

### Production Configuration

For production environments, consider:

1. **Change `ddl-auto` to `validate`**:
   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   ```

2. **Disable SQL logging**:
   ```properties
   spring.jpa.show-sql=false
   logging.level.org.hibernate.SQL=INFO
   ```

3. **Use environment variables for credentials**:
   ```properties
   spring.datasource.username=${DB_USERNAME}
   spring.datasource.password=${DB_PASSWORD}
   ```

4. **Increase connection pool size** based on load:
   ```properties
   spring.datasource.hikari.maximum-pool-size=50
   ```

## Architecture Overview

### Components

- **TransferController**: REST API layer handling HTTP requests
- **TransferService**: Business logic layer with transaction management
- **AccountRepository**: Data access layer with pessimistic locking support
- **TransactionRepository**: Data access layer for transaction records
- **GlobalExceptionHandler**: Centralized exception handling

### Concurrency Control

The system uses **pessimistic locking** to prevent race conditions:

1. When a transfer starts, both sender and receiver accounts are locked with `SELECT ... FOR UPDATE`
2. Other transactions attempting to access the same accounts will wait
3. Locks are released when the transaction commits or rolls back
4. This ensures serializable execution of concurrent transfers

### Transaction Isolation

- **Isolation Level**: `READ_COMMITTED`
- **Locking Strategy**: Pessimistic write locks on account rows
- **Timeout**: 30 seconds to prevent indefinite waiting

## Troubleshooting

### Application won't start

**Error**: `Communications link failure`

**Solution**: Verify MySQL is running and credentials are correct:
```bash
mysql -u root -p -e "SELECT 1"
```

### Tests fail with "Could not start container"

**Error**: Testcontainers cannot start MySQL container

**Solution**: Ensure Docker is running:
```bash
docker ps
```

### Transfer fails with timeout

**Error**: `CannotAcquireLockException`

**Solution**: Another transaction is holding a lock. This is expected under high concurrency. The client should retry with exponential backoff.

### Negative balance in database

**Error**: Account has negative balance

**Solution**: This should never happen if constraints are applied. Check:
1. Are database constraints applied? Run the migration script.
2. Are you bypassing the API and updating the database directly? Always use the API.

## Project Structure

```
fintech-wallet-api/
├── src/
│   ├── main/
│   │   ├── java/com/fintech/wallet/
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── dto/                 # Request/Response DTOs
│   │   │   ├── entity/              # JPA entities
│   │   │   ├── exception/           # Custom exceptions
│   │   │   ├── repository/          # Data access layer
│   │   │   ├── service/             # Business logic layer
│   │   │   └── WalletApiApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/
│   │       │   └── mysql-indexes-constraints.sql
│   │       └── db-migration-guide.md
│   └── test/
│       └── java/com/fintech/wallet/
│           ├── controller/          # Controller tests
│           ├── service/             # Service tests
│           └── BaseIntegrationTest.java
├── pom.xml
└── README.md
```

## Performance Considerations

### Connection Pooling

The application uses HikariCP with:
- Maximum pool size: 20 connections
- Minimum idle: 5 connections
- Connection timeout: 30 seconds

Adjust based on your load:
```properties
spring.datasource.hikari.maximum-pool-size=50  # For high traffic
```

### Database Indexes

Indexes are created on:
- `accounts.user_id` - Fast account lookups
- `transactions.sender_id` - Fast transaction history by sender
- `transactions.receiver_id` - Fast transaction history by receiver
- Composite indexes for time-range queries

### Lock Wait Timeout

MySQL lock wait timeout is set to 30 seconds:
```
innodb_lock_wait_timeout=30
```

This prevents indefinite waiting but may cause timeouts under extreme load.

## Security Considerations

### Current Implementation

- Input validation using Bean Validation
- SQL injection prevention through JPA/Hibernate
- Transaction atomicity and isolation

### Production Recommendations

1. **Add Authentication**: Implement Spring Security with JWT
2. **Add Authorization**: Ensure users can only transfer from their own accounts
3. **Rate Limiting**: Prevent abuse with rate limiting (e.g., Bucket4j)
4. **HTTPS**: Use TLS in production
5. **Audit Logging**: Log all transfer attempts for security monitoring
6. **Input Sanitization**: Additional validation for edge cases

## License

This project is for educational purposes.

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the design document: `.kiro/specs/fintech-wallet-api/design.md`
3. Review the requirements: `.kiro/specs/fintech-wallet-api/requirements.md`
4. Check test examples in `src/test/java/com/fintech/wallet/`
