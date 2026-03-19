package com.fintech.wallet;

import com.fintech.wallet.entity.Account;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for BaseIntegrationTest setup.
 * 
 * This test verifies that:
 * - MySQL container starts successfully
 * - Spring Boot context loads with Testcontainers configuration
 * - Database operations work correctly
 * 
 * Validates: Requirements 11.1, 11.2, 11.3
 */
class BaseIntegrationTestVerification extends BaseIntegrationTest {
    
    @Test
    void testMySQLContainerIsRunning() {
        assertTrue(BaseIntegrationTest.mysqlContainer.isRunning(), 
            "MySQL container should be running");
    }
    
    @Test
    void testDatabaseConnectionWorks() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Account account = new Account(null, 100L, new BigDecimal("1000.00"), null, now, now);
        
        // Act
        Account savedAccount = accountRepository.save(account);
        
        // Assert
        assertNotNull(savedAccount.getId(), "Account should have an ID after saving");
        assertEquals(new BigDecimal("1000.00"), savedAccount.getBalance());
        
        // Verify we can retrieve it
        Account retrievedAccount = accountRepository.findById(savedAccount.getId()).orElse(null);
        assertNotNull(retrievedAccount, "Should be able to retrieve saved account");
        assertEquals(savedAccount.getId(), retrievedAccount.getId());
    }
    
    @Test
    void testDatabaseCleanupBetweenTests() {
        // This test verifies that the @BeforeEach cleanup works
        long accountCount = accountRepository.count();
        long transactionCount = transactionRepository.count();
        
        assertEquals(0, accountCount, "Account table should be empty at test start");
        assertEquals(0, transactionCount, "Transaction table should be empty at test start");
    }
}
