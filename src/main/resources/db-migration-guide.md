# Database Migration Guide

## Overview

This guide explains how to apply database indexes and constraints to the FinTech Wallet API database.

## Prerequisites

- MySQL 8.0.16 or higher (for CHECK constraint support)
- Database user with ALTER and CREATE INDEX privileges
- Existing `wallet_db` database with `accounts` and `transactions` tables

## Migration Options

### Option 1: Manual Execution (Recommended for Production)

1. Connect to your MySQL database:
   ```bash
   mysql -u root -p wallet_db
   ```

2. Execute the schema script:
   ```sql
   source src/main/resources/schema.sql;
   ```

3. Verify indexes were created:
   ```sql
   SHOW INDEX FROM accounts;
   SHOW INDEX FROM transactions;
   ```

4. Verify constraints were created:
   ```sql
   SHOW CREATE TABLE accounts;
   SHOW CREATE TABLE transactions;
   ```

### Option 2: Spring Boot Automatic Initialization (Development)

1. Update `application.properties` to enable schema initialization:
   ```properties
   spring.sql.init.mode=always
   spring.sql.init.continue-on-error=false
   ```

2. Restart the application - Spring Boot will execute `schema.sql` on startup

**Warning**: This will attempt to create indexes/constraints on every startup. Use only in development.

### Option 3: Using Flyway (Recommended for Production)

If you want versioned migrations, add Flyway:

1. Add dependency to `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.flywaydb</groupId>
       <artifactId>flyway-core</artifactId>
   </dependency>
   <dependency>
       <groupId>org.flywaydb</groupId>
       <artifactId>flyway-mysql</artifactId>
   </dependency>
   ```

2. Create migration directory: `src/main/resources/db/migration/`

3. Move schema.sql to: `src/main/resources/db/migration/V2__add_indexes_and_constraints.sql`

4. Update `application.properties`:
   ```properties
   spring.flyway.enabled=true
   spring.flyway.baseline-on-migrate=true
   spring.jpa.hibernate.ddl-auto=validate
   ```

5. Restart application - Flyway will apply migrations automatically

## Indexes Created

| Index Name | Table | Columns | Purpose |
|------------|-------|---------|---------|
| `idx_accounts_user` | accounts | user_id | Faster account lookups by user |
| `idx_transactions_sender` | transactions | sender_id | Faster transaction history by sender |
| `idx_transactions_receiver` | transactions | receiver_id | Faster transaction history by receiver |
| `idx_transactions_sender_timestamp` | transactions | sender_id, timestamp | Time-range queries by sender |
| `idx_transactions_receiver_timestamp` | transactions | receiver_id, timestamp | Time-range queries by receiver |

## Constraints Created

| Constraint Name | Table | Rule | Requirement |
|-----------------|-------|------|-------------|
| `chk_balance` | accounts | balance >= 0 | 9.2 - No negative balances |
| `chk_transaction_amount` | transactions | amount > 0 | Data integrity |
| `chk_different_accounts` | transactions | sender_id != receiver_id | Data integrity |

## Performance Impact

- **Indexes**: Improve query performance for transaction history and account lookups
- **Write overhead**: Minimal - indexes are updated automatically on INSERT/UPDATE
- **Storage**: Approximately 10-15% additional disk space for indexes

## Rollback Instructions

If you need to remove the indexes and constraints:

```sql
-- Remove indexes
DROP INDEX idx_accounts_user ON accounts;
DROP INDEX idx_transactions_sender ON transactions;
DROP INDEX idx_transactions_receiver ON transactions;
DROP INDEX idx_transactions_sender_timestamp ON transactions;
DROP INDEX idx_transactions_receiver_timestamp ON transactions;

-- Remove constraints
ALTER TABLE accounts DROP CONSTRAINT chk_balance;
ALTER TABLE transactions DROP CONSTRAINT chk_transaction_amount;
ALTER TABLE transactions DROP CONSTRAINT chk_different_accounts;
```

## Verification Queries

After applying migrations, verify everything works:

```sql
-- Test that negative balance is rejected
UPDATE accounts SET balance = -100 WHERE id = 1;
-- Expected: ERROR 3819 (HY000): Check constraint 'chk_balance' is violated.

-- Test that zero/negative transaction amount is rejected
INSERT INTO transactions (sender_id, receiver_id, amount, status, timestamp) 
VALUES (1, 2, -50, 'COMPLETED', NOW());
-- Expected: ERROR 3819 (HY000): Check constraint 'chk_transaction_amount' is violated.

-- Test that same sender/receiver is rejected
INSERT INTO transactions (sender_id, receiver_id, amount, status, timestamp) 
VALUES (1, 1, 50, 'COMPLETED', NOW());
-- Expected: ERROR 3819 (HY000): Check constraint 'chk_different_accounts' is violated.

-- Verify indexes exist
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'wallet_db'
  AND TABLE_NAME IN ('accounts', 'transactions')
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME;
```

## Troubleshooting

### Error: "Check constraint is violated"
- This means existing data violates the constraint
- Clean up invalid data before applying constraints
- Example: `UPDATE accounts SET balance = 0 WHERE balance < 0;`

### Error: "Duplicate key name"
- Index already exists
- Either drop existing index or use `CREATE INDEX IF NOT EXISTS`

### MySQL Version < 8.0.16
- CHECK constraints are not supported
- Consider upgrading MySQL or use triggers as alternative
- Application-level validation is already in place as fallback

## Next Steps

After applying migrations:
1. Run integration tests to verify functionality
2. Monitor query performance with `EXPLAIN` statements
3. Consider adding more indexes based on actual query patterns
4. Set up monitoring for constraint violations in production
